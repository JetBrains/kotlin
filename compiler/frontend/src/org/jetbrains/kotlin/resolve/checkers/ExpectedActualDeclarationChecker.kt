/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.CombinedModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.isPrimaryConstructorOfInlineClass
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Compatible
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Incompatible
import org.jetbrains.kotlin.resolve.multiplatform.ModuleFilter
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.ifEmpty
import java.io.File

class ExpectedActualDeclarationChecker(
    val moduleStructureOracle: ModuleStructureOracle,
    val argumentExtractors: Iterable<ActualAnnotationArgumentExtractor>
) : DeclarationChecker {
    interface ActualAnnotationArgumentExtractor {
        fun extractDefaultValue(parameter: ValueParameterDescriptor, expectedType: KotlinType): ConstantValue<*>?
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return

        // Note that this check is necessary, because for default accessors KtProperty is passed for KtDeclaration, so this
        // case won't be covered by the next check (also, it accidentally fixes KT-28385)
        if (descriptor is PropertyAccessorDescriptor) return
        if (declaration !is KtNamedDeclaration) return
        if (descriptor !is MemberDescriptor || DescriptorUtils.isEnumEntry(descriptor)) return

        if (descriptor.isExpect) {
            checkExpectedDeclarationHasProperActuals(declaration, descriptor, context.trace, context.expectActualTracker)
        } else {
            val checkActual = !context.languageVersionSettings.getFlag(AnalysisFlags.multiPlatformDoNotCheckActual)

            val allImplementedModules = moduleStructureOracle.findAllDependsOnPaths(descriptor.module).flatMap { it.nodes }.toHashSet()
            checkActualDeclarationHasExpected(
                declaration,
                descriptor,
                context.trace,
                checkActual,
                moduleVisibilityFilter = { it in allImplementedModules }
            )
        }
    }

    private fun checkExpectedDeclarationHasProperActuals(
        reportOn: KtNamedDeclaration,
        descriptor: MemberDescriptor,
        trace: BindingTrace,
        expectActualTracker: ExpectActualTracker
    ) {
        val allActualizationPaths = moduleStructureOracle.findAllReversedDependsOnPaths(descriptor.module)
        val leafModuleToVisibleModules = allActualizationPaths.groupBy { it.nodes.last() }
            .mapValues { it.value.flatMap { it.nodes }.toSet() }

        for ((leafModule, modulesVisibleFromLeaf) in leafModuleToVisibleModules) {
            checkExpectedDeclarationHasAtLeastOneActual(
                reportOn,
                descriptor,
                trace,
                leafModule,
                expectActualTracker,
                moduleVisibilityFilter = { it in modulesVisibleFromLeaf }
            )
        }

        /*
        Note that we have to check for 'duplicate actuals' separately, considering paths
        instead of only leaf-module, because currently we have to distinguish the following
        two cases:

               expect class A                      expect class A
                /            \                       /           \
               /              \                     /             \
         actual class A     actual class A   actual class A    (no actual)
               \              /                     \             /
                \            /                       \           /
           (warning or nothing, TBD)                 actual class A
                                                  (ERROR: duplicate actuals)

        If we merge behaviour (e.g. decide to report ERROR for first case too)
        for those two cases, we can drop separate logic for DUPLICATE_ACTUALS
        */
        for (path in allActualizationPaths) {
            val modulesOnThisPath = path.nodes.toSet()
            checkExpectedDeclarationHasAtMostOneActual(
                reportOn, descriptor, trace, path, moduleVisibilityFilter = { it in modulesOnThisPath }
            )
        }
    }

    private fun checkExpectedDeclarationHasAtMostOneActual(
        reportOn: KtNamedDeclaration,
        descriptor: MemberDescriptor,
        trace: BindingTrace,
        path: ModulePath,
        moduleVisibilityFilter: ModuleFilter
    ) {
        val compatibility = path.nodes
            .mapNotNull { ExpectedActualResolver.findActualForExpected(descriptor, it, moduleVisibilityFilter) }
            .ifEmpty { return }
            .fold(LinkedHashMap<Compatibility, List<MemberDescriptor>>()) { resultMap, partialMap ->
                resultMap.apply { putAll(partialMap) }
            }

        // Several compatible actuals on one path: report AMBIGUIOUS_ACTUALS here
        val atLeastWeaklyCompatibleActuals = compatibility.filterKeys { it.isCompatibleOrWeakCompatible() }.values.flatten()
        if (atLeastWeaklyCompatibleActuals.size > 1) {
            trace.report(Errors.AMBIGUOUS_ACTUALS.on(
                reportOn,
                descriptor,
                atLeastWeaklyCompatibleActuals
                    .map { it.module }
                    .sortedBy { it.name.asString() }
            ))
        }
    }

    private fun checkExpectedDeclarationHasAtLeastOneActual(
        reportOn: KtNamedDeclaration,
        descriptor: MemberDescriptor,
        trace: BindingTrace,
        module: ModuleDescriptor,
        expectActualTracker: ExpectActualTracker,
        moduleVisibilityFilter: ModuleFilter
    ) {
        // Only look for top level actual members; class members will be handled as a part of that expected class
        if (descriptor.containingDeclaration !is PackageFragmentDescriptor) return

        val compatibility = ExpectedActualResolver.findActualForExpected(descriptor, module, moduleVisibilityFilter) ?: return

        // Only strong incompatibilities, but this is an OptionalExpectation -- don't report it
        if (compatibility.allStrongIncompatibilities() && isOptionalAnnotationClass(descriptor)) return

        // Only strong incompatibilities, or error won't be reported on actual: report NO_ACTUAL_FOR_EXPECT here
        if (compatibility.allStrongIncompatibilities() ||
            Compatible !in compatibility && descriptor.hasNoActualWithDiagnostic(compatibility)
        ) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            trace.report(Errors.NO_ACTUAL_FOR_EXPECT.on(reportOn, descriptor, module, incompatibility))
            return
        }

        // Here we have exactly one compatible actual and/or some weakly incompatible. In either case, we don't report anything on expect
        val actualMembers = compatibility.asSequence()
            .filter { (compatibility, _) ->
                compatibility is Compatible || (compatibility is Incompatible && compatibility.kind != Compatibility.IncompatibilityKind.STRONG)
            }.flatMap { it.value.asSequence() }

        expectActualTracker.reportExpectActual(expected = descriptor, actualMembers = actualMembers)
    }

    private fun MemberDescriptor.hasNoActualWithDiagnostic(
        compatibility: Map<Compatibility, List<MemberDescriptor>>
    ): Boolean {
        return compatibility.values.flatMapTo(hashSetOf()) { it }.all { actual ->
            val expectedOnes = ExpectedActualResolver.findExpectedForActual(actual, module)
            expectedOnes != null && Compatible in expectedOnes.keys
        }
    }

    private fun ExpectActualTracker.reportExpectActual(expected: MemberDescriptor, actualMembers: Sequence<MemberDescriptor>) {
        if (this is ExpectActualTracker.DoNothing) return

        val expectedFile = sourceFile(expected) ?: return
        for (actual in actualMembers) {
            val actualFile = sourceFile(actual) ?: continue
            report(expectedFile = expectedFile, actualFile = actualFile)
        }
    }

    private fun sourceFile(descriptor: MemberDescriptor): File? =
        descriptor.source
            .containingFile
            .safeAs<PsiSourceFile>()
            ?.run { VfsUtilCore.virtualToIoFile(psiFile.virtualFile) }

    private fun checkActualDeclarationHasExpected(
        reportOn: KtNamedDeclaration,
        descriptor: MemberDescriptor,
        trace: BindingTrace,
        checkActual: Boolean,
        moduleVisibilityFilter: ModuleFilter
    ) {
        val compatibility = ExpectedActualResolver.findExpectedForActual(descriptor, descriptor.module, moduleVisibilityFilter)
            ?: return

        checkAmbiguousExpects(compatibility, trace, reportOn, descriptor)

        val hasActualModifier = descriptor.isActual && reportOn.hasActualModifier()
        if (!hasActualModifier) {
            if (compatibility.allStrongIncompatibilities()) return

            if (Compatible in compatibility) {
                if (checkActual && requireActualModifier(descriptor)) {
                    trace.report(Errors.ACTUAL_MISSING.on(reportOn))
                }

                return
            }
        }

        // 'firstOrNull' is needed because in diagnostic tests, common sources appear twice, so the same class is duplicated
        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
        val singleIncompatibility = compatibility.keys.firstOrNull()
        if (singleIncompatibility is Incompatible.ClassScopes) {
            assert(descriptor is ClassDescriptor || descriptor is TypeAliasDescriptor) {
                "Incompatible.ClassScopes is only possible for a class or a typealias: $descriptor"
            }

            // Do not report "expected members have no actual ones" for those expected members, for which there's a clear
            // (albeit maybe incompatible) single actual suspect, declared in the actual class.
            // This is needed only to reduce the number of errors. Incompatibility errors for those members will be reported
            // later when this checker is called for them
            fun hasSingleActualSuspect(
                expectedWithIncompatibility: Pair<MemberDescriptor, Map<Incompatible, Collection<MemberDescriptor>>>
            ): Boolean {
                val (expectedMember, incompatibility) = expectedWithIncompatibility
                val actualMember = incompatibility.values.singleOrNull()?.singleOrNull()
                return actualMember != null &&
                        actualMember.isExplicitActualDeclaration() &&
                        !incompatibility.allStrongIncompatibilities() &&
                        ExpectedActualResolver.findExpectedForActual(
                            actualMember,
                            expectedMember.module
                        )?.values?.singleOrNull()?.singleOrNull() == expectedMember
            }

            val nonTrivialUnfulfilled = singleIncompatibility.unfulfilled.filterNot(::hasSingleActualSuspect)

            if (nonTrivialUnfulfilled.isNotEmpty()) {
                val classDescriptor =
                    (descriptor as? TypeAliasDescriptor)?.expandedType?.constructor?.declarationDescriptor as? ClassDescriptor
                        ?: (descriptor as ClassDescriptor)
                trace.report(
                    Errors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS.on(
                        reportOn, classDescriptor, nonTrivialUnfulfilled
                    )
                )
            }
        } else if (Compatible !in compatibility) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            trace.report(Errors.ACTUAL_WITHOUT_EXPECT.on(reportOn, descriptor, incompatibility))
        } else {
            val expected = compatibility[Compatible]!!.first()
            if (expected is ClassDescriptor && expected.kind == ClassKind.ANNOTATION_CLASS) {
                val actualConstructor =
                    (descriptor as? ClassDescriptor)?.constructors?.singleOrNull()
                        ?: (descriptor as? TypeAliasDescriptor)?.constructors?.singleOrNull()?.underlyingConstructorDescriptor
                val expectedConstructor = expected.constructors.singleOrNull()
                if (expectedConstructor != null && actualConstructor != null) {
                    checkAnnotationConstructors(expectedConstructor, actualConstructor, trace, reportOn)
                }
            }
        }
    }

    private fun checkAmbiguousExpects(
        compatibility: Map<Compatibility, List<MemberDescriptor>>,
        trace: BindingTrace,
        reportOn: KtNamedDeclaration,
        descriptor: MemberDescriptor
    ) {
        val filesWithAtLeastWeaklyCompatibleExpects = compatibility.asSequence()
            .filter { (compatibility, _) ->
                compatibility.isCompatibleOrWeakCompatible()
            }
            .map { (_, members) -> members }
            .flatten()
            .map { it.module }
            .sortedBy { it.name.asString() }
            .toList()

        if (filesWithAtLeastWeaklyCompatibleExpects.size > 1) {
            trace.report(Errors.AMBIGUOUS_EXPECTS.on(reportOn, descriptor, filesWithAtLeastWeaklyCompatibleExpects))
        }
    }

    private fun Compatibility.isCompatibleOrWeakCompatible() =
        this is Compatible ||
                this is Incompatible && kind == ExpectedActualResolver.Compatibility.IncompatibilityKind.WEAK

    // we don't require `actual` modifier on
    //  - annotation constructors, because annotation classes can only have one constructor
    //  - inline class primary constructors, because inline class must have primary constructor
    //  - value parameter inside primary constructor of inline class, because inline class must have one value parameter
    private fun requireActualModifier(descriptor: MemberDescriptor): Boolean {
        return !descriptor.isAnnotationConstructor() &&
                !descriptor.isPrimaryConstructorOfInlineClass() &&
                !isUnderlyingPropertyOfInlineClass(descriptor)
    }

    private fun isUnderlyingPropertyOfInlineClass(descriptor: MemberDescriptor): Boolean {
        return descriptor is PropertyDescriptor && descriptor.isUnderlyingPropertyOfInlineClass()
    }

    // This should ideally be handled by CallableMemberDescriptor.Kind, but default constructors have kind DECLARATION and non-empty source.
    // Their source is the containing KtClass instance though, as opposed to explicit constructors, whose source is KtConstructor
    private fun MemberDescriptor.isExplicitActualDeclaration(): Boolean =
        when (this) {
            is ConstructorDescriptor -> DescriptorToSourceUtils.getSourceFromDescriptor(this) is KtConstructor<*>
            is CallableMemberDescriptor -> kind == CallableMemberDescriptor.Kind.DECLARATION
            else -> true
        }

    private fun checkAnnotationConstructors(
        expected: ConstructorDescriptor, actual: ConstructorDescriptor, trace: BindingTrace, reportOn: PsiElement
    ) {
        for (expectedParameterDescriptor in expected.valueParameters) {
            // Actual parameter with the same name is guaranteed to exist because this method is only called for compatible annotations
            val actualParameterDescriptor = actual.valueParameters.first { it.name == expectedParameterDescriptor.name }

            if (expectedParameterDescriptor.declaresDefaultValue() && actualParameterDescriptor.declaresDefaultValue()) {
                val expectedParameter =
                    DescriptorToSourceUtils.descriptorToDeclaration(expectedParameterDescriptor) as? KtParameter ?: continue

                val expectedValue = trace.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expectedParameter.defaultValue)
                    ?.toConstantValue(expectedParameterDescriptor.type)

                val actualValue =
                    getActualAnnotationParameterValue(actualParameterDescriptor, trace.bindingContext, expectedParameterDescriptor.type)
                if (expectedValue != actualValue) {
                    val ktParameter = DescriptorToSourceUtils.descriptorToDeclaration(actualParameterDescriptor)
                    val target = (ktParameter as? KtParameter)?.defaultValue ?: (reportOn as? KtTypeAlias)?.nameIdentifier ?: reportOn
                    trace.report(Errors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE.on(target, actualParameterDescriptor))
                }
            }
        }
    }

    private fun getActualAnnotationParameterValue(
        actualParameter: ValueParameterDescriptor, bindingContext: BindingContext, expectedType: KotlinType
    ): ConstantValue<*>? {
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(actualParameter)
        if (declaration is KtParameter) {
            return bindingContext.get(BindingContext.COMPILE_TIME_VALUE, declaration.defaultValue)?.toConstantValue(expectedType)
        }

        for (extractor in argumentExtractors) {
            extractor.extractDefaultValue(actualParameter, expectedType)?.let { return it }
        }

        return null
    }

    companion object {
        val OPTIONAL_EXPECTATION_FQ_NAME = FqName("kotlin.OptionalExpectation")

        @JvmStatic
        fun isOptionalAnnotationClass(descriptor: DeclarationDescriptor): Boolean =
            descriptor is ClassDescriptor &&
                    descriptor.kind == ClassKind.ANNOTATION_CLASS &&
                    descriptor.isExpect &&
                    descriptor.annotations.hasAnnotation(OPTIONAL_EXPECTATION_FQ_NAME)

        // TODO: move to some other place which is accessible both from backend-common and js.serializer
        @JvmStatic
        fun shouldGenerateExpectClass(descriptor: ClassDescriptor): Boolean {
            assert(descriptor.isExpect) { "Not an expected class: $descriptor" }

            if (isOptionalAnnotationClass(descriptor)) {
                with(ExpectedActualResolver) {
                    return descriptor.findCompatibleActualForExpected(descriptor.module).isEmpty()
                }
            }

            return false
        }

        fun Map<out Compatibility, Collection<MemberDescriptor>>.allStrongIncompatibilities(): Boolean =
            this.keys.all { it is Incompatible && it.kind == Compatibility.IncompatibilityKind.STRONG }

        private fun <K, V> LinkedHashMap<K, List<V>>.merge(other: Map<K, List<V>>): LinkedHashMap<K, List<V>> {
            for ((key, newValue) in other) {
                val oldValue = this[key] ?: emptyList()
                this[key] = oldValue + newValue
            }

            return this
        }

        private fun ModuleInfo.unwrapModuleInfo(): List<ModuleInfo> = if (this is CombinedModuleInfo) this.containedModules else listOf(this)
    }
}
