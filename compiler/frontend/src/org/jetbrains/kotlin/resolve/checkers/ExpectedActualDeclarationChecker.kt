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
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.isPrimaryConstructorOfInlineClass
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Compatible
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Incompatible
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.ifEmpty
import java.io.File

object ExpectedActualDeclarationChecker : DeclarationChecker {
    val OPTIONAL_EXPECTATION_FQ_NAME = FqName("kotlin.OptionalExpectation")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return

        if (declaration !is KtNamedDeclaration) return
        if (descriptor !is MemberDescriptor || DescriptorUtils.isEnumEntry(descriptor)) return

        if (descriptor.isExpect) {
            checkExpectedDeclarationHasActual(declaration, descriptor, context.trace, descriptor.module, context.expectActualTracker)
        } else {
            val checkActual = !context.languageVersionSettings.getFlag(AnalysisFlag.multiPlatformDoNotCheckActual)
            checkActualDeclarationHasExpected(declaration, descriptor, context.trace, checkActual)
        }
    }

    fun checkExpectedDeclarationHasActual(
        reportOn: KtNamedDeclaration,
        descriptor: MemberDescriptor,
        trace: BindingTrace,
        platformModule: ModuleDescriptor,
        expectActualTracker: ExpectActualTracker
    ) {
        // Only look for top level actual members; class members will be handled as a part of that expected class
        if (descriptor.containingDeclaration !is PackageFragmentDescriptor) return

        val compatibility = ExpectedActualResolver.findActualForExpected(descriptor, platformModule) ?: return

        if (compatibility.allStrongIncompatibilities() && isOptionalAnnotationClass(descriptor)) return

        val shouldReportError =
            compatibility.allStrongIncompatibilities() ||
                    Compatible !in compatibility && compatibility.values.flatMapTo(hashSetOf()) { it }.all { actual ->
                val expectedOnes = ExpectedActualResolver.findExpectedForActual(actual, descriptor.module)
                expectedOnes != null && Compatible in expectedOnes.keys
            }

        if (shouldReportError) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            trace.report(Errors.NO_ACTUAL_FOR_EXPECT.on(reportOn, descriptor, platformModule, incompatibility))
        } else {
            val actualMembers = compatibility.asSequence()
                .filter { (compatibility, _) ->
                    compatibility is Compatible || (compatibility is Incompatible && compatibility.kind != Compatibility.IncompatibilityKind.STRONG)
                }.flatMap { it.value.asSequence() }

            expectActualTracker.reportExpectActual(expected = descriptor, actualMembers = actualMembers)
        }
    }

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

        if (ExpectedActualDeclarationChecker.isOptionalAnnotationClass(descriptor)) {
            with(ExpectedActualResolver) {
                return descriptor.findCompatibleActualForExpected(descriptor.module).isEmpty()
            }
        }

        return false
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

    fun Map<out Compatibility, Collection<MemberDescriptor>>.allStrongIncompatibilities(): Boolean =
        this.keys.all { it is Incompatible && it.kind == Compatibility.IncompatibilityKind.STRONG }

    private fun checkActualDeclarationHasExpected(
        reportOn: KtNamedDeclaration, descriptor: MemberDescriptor, trace: BindingTrace, checkActual: Boolean
    ) {
        // TODO: ideally, we should always use common module here
        // However, in compiler context platform & common modules are joined into one module,
        // so there is yet no "common module" in this situation.
        // So yet we are using own module in compiler context and common module in IDE context.
        val commonOrOwnModules = descriptor.module.expectedByModules.ifEmpty { listOf(descriptor.module) }
        val compatibility = commonOrOwnModules
            .mapNotNull { ExpectedActualResolver.findExpectedForActual(descriptor, it) }
            .ifEmpty { return }
            .fold(LinkedHashMap<Compatibility, List<MemberDescriptor>>()) { resultMap, partialMap ->
                resultMap.apply { putAll(partialMap) }
            }

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
                    (descriptor as? ClassDescriptor)?.constructors?.singleOrNull() ?:
                    (descriptor as? TypeAliasDescriptor)?.constructors?.singleOrNull()?.underlyingConstructorDescriptor
                val expectedConstructor = expected.constructors.singleOrNull()
                if (expectedConstructor != null && actualConstructor != null) {
                    checkAnnotationConstructors(expectedConstructor, actualConstructor, trace, reportOn)
                }
            }
        }
    }

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
                val actualParameter = DescriptorToSourceUtils.descriptorToDeclaration(actualParameterDescriptor)

                val expectedValue = trace.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expectedParameter.defaultValue)
                // TODO: support arguments coming from Java via typealias, see PsiAnnotationMethod.getDefaultValue()
                val actualValue = (actualParameter as? KtParameter)?.let { parameter ->
                    trace.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, parameter.defaultValue)
                }
                if (expectedValue != actualValue) {
                    val target = (actualParameter as? KtParameter)?.defaultValue ?: (reportOn as? KtTypeAlias)?.nameIdentifier ?: reportOn
                    trace.report(Errors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE.on(target, actualParameterDescriptor))
                }
            }
        }
    }
}
