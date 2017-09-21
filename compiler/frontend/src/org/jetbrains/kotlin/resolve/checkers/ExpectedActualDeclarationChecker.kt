/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker.Compatibility.Compatible
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker.Compatibility.Incompatible
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.checker.TypeCheckerContext
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.keysToMap

object ExpectedActualDeclarationChecker : DeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return

        if (descriptor !is MemberDescriptor || DescriptorUtils.isEnumEntry(descriptor)) return

        if (descriptor.isExpect) {
            checkExpectedDeclarationHasActual(declaration, descriptor, diagnosticHolder, descriptor.module)
        }
        else {
            val checkExpected = !languageVersionSettings.getFlag(AnalysisFlag.multiPlatformDoNotCheckActual)
            checkActualDeclarationHasExpected(declaration, descriptor, diagnosticHolder, checkExpected)
        }
    }

    fun checkExpectedDeclarationHasActual(
            reportOn: KtDeclaration,
            descriptor: MemberDescriptor,
            diagnosticHolder: DiagnosticSink,
            platformModule: ModuleDescriptor
    ) {
        // Only look for top level actual members; class members will be handled as a part of that expected class
        if (descriptor.containingDeclaration !is PackageFragmentDescriptor) return

        val compatibility = findActualForExpected(descriptor, platformModule) ?: return

        val shouldReportError =
                compatibility.isEmpty() ||
                Compatible !in compatibility && compatibility.values.flatMapTo(hashSetOf()) { it }.all { actual ->
                    val expectedOnes = findExpectedForActual(actual, descriptor.module)
                    expectedOnes != null && Compatible in expectedOnes.keys
                }

        if (shouldReportError) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            diagnosticHolder.report(Errors.NO_ACTUAL_FOR_EXPECT.on(reportOn, descriptor, platformModule, incompatibility))
        }
    }

    private fun findActualForExpected(expected: MemberDescriptor, platformModule: ModuleDescriptor): Map<Compatibility, List<MemberDescriptor>>? {
        return when (expected) {
            is CallableMemberDescriptor -> {
                expected.findNamesakesFromModule(platformModule).filter { actual ->
                    expected != actual && !actual.isExpect &&
                    // TODO: support non-source definitions (e.g. from Java)
                    DescriptorToSourceUtils.getSourceFromDescriptor(actual) is KtElement
                }.groupBy { actual ->
                    areCompatibleCallables(expected, actual)
                }
            }
            is ClassDescriptor -> {
                expected.findClassifiersFromModule(platformModule).filter { actual ->
                    expected != actual && !actual.isExpect &&
                    DescriptorToSourceUtils.getSourceFromDescriptor(actual) is KtElement
                }.groupBy { actual ->
                    areCompatibleClassifiers(expected, actual)
                }
            }
            else -> null
        }
    }

    private fun checkActualDeclarationHasExpected(
            reportOn: KtDeclaration, descriptor: MemberDescriptor, diagnosticHolder: DiagnosticSink, checkExpected: Boolean
    ) {
        // Using the platform module instead of the common module is sort of fine here because the former always depends on the latter.
        // However, it would be clearer to find the common module this platform module implements and look for expected there instead.
        // TODO: use common module here
        val compatibility = findExpectedForActual(descriptor, descriptor.module) ?: return

        val hasExpectedModifier = descriptor.isActual && reportOn.hasActualModifier()
        if (!hasExpectedModifier) {
            if (Compatible !in compatibility) return

            if (checkExpected) {
                diagnosticHolder.report(Errors.ACTUAL_MISSING.on(reportOn))
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
                       findExpectedForActual(actualMember, expectedMember.module)?.values?.singleOrNull()?.singleOrNull() == expectedMember
            }

            val nonTrivialUnfulfilled = singleIncompatibility.unfulfilled.filterNot(::hasSingleActualSuspect)

            if (nonTrivialUnfulfilled.isNotEmpty()) {
                val classDescriptor =
                        (descriptor as? TypeAliasDescriptor)?.expandedType?.constructor?.declarationDescriptor as? ClassDescriptor
                        ?: (descriptor as ClassDescriptor)
                diagnosticHolder.report(Errors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS.on(
                        reportOn, classDescriptor, nonTrivialUnfulfilled
                ))
            }
        }
        else if (Compatible !in compatibility) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            diagnosticHolder.report(Errors.ACTUAL_WITHOUT_EXPECT.on(reportOn, descriptor, incompatibility))
        }
    }

    // This should ideally be handled by CallableMemberDescriptor.Kind, but default constructors have kind DECLARATION and non-empty source.
    // Their source is the containing KtClass instance though, as opposed to explicit constructors, whose source is KtConstructor
    private fun MemberDescriptor.isExplicitActualDeclaration(): Boolean =
            when (this) {
                is ConstructorDescriptor -> DescriptorToSourceUtils.getSourceFromDescriptor(this) is KtConstructor<*>
                is CallableMemberDescriptor -> kind == CallableMemberDescriptor.Kind.DECLARATION
                else -> true
            }

    private fun findExpectedForActual(actual: MemberDescriptor, commonModule: ModuleDescriptor): Map<Compatibility, List<MemberDescriptor>>? {
        return when (actual) {
            is CallableMemberDescriptor -> {
                val container = actual.containingDeclaration
                val candidates = when (container) {
                    is ClassDescriptor -> {
                        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
                        val expectedClass = findExpectedForActual(container, commonModule)?.values?.firstOrNull()?.firstOrNull() as? ClassDescriptor
                        expectedClass?.getMembers(actual.name)?.filterIsInstance<CallableMemberDescriptor>().orEmpty()
                    }
                    is PackageFragmentDescriptor -> actual.findNamesakesFromModule(commonModule)
                    else -> return null // do not report anything for incorrect code, e.g. 'actual' local function
                }

                candidates.filter { declaration ->
                    actual != declaration && declaration.isExpect
                }.groupBy { declaration ->
                    // TODO: optimize by caching this per actual-expected class pair, do not create a new substitutor for each actual member
                    val substitutor =
                            if (container is ClassDescriptor) {
                                val expectedClass = declaration.containingDeclaration as ClassDescriptor
                                // TODO: this might not work for members of inner generic classes
                                Substitutor(expectedClass.declaredTypeParameters, container.declaredTypeParameters)
                            }
                            else null
                    areCompatibleCallables(declaration, actual, parentSubstitutor = substitutor)
                }
            }
            is ClassifierDescriptorWithTypeParameters -> {
                actual.findClassifiersFromModule(commonModule).filter { declaration ->
                    actual != declaration &&
                    declaration is ClassDescriptor && declaration.isExpect
                }.groupBy { expected ->
                    areCompatibleClassifiers(expected as ClassDescriptor, actual)
                }
            }
            else -> null
        }
    }

    fun MemberDescriptor.findCompatibleActualForExpected(platformModule: ModuleDescriptor): List<MemberDescriptor> =
            findActualForExpected(this, platformModule)?.get(Compatible).orEmpty()

    fun MemberDescriptor.findAnyActualForExpected(platformModule: ModuleDescriptor): List<MemberDescriptor> {
        val actualsGroupedByCompatibility = findActualForExpected(this, platformModule)
        return actualsGroupedByCompatibility?.get(Compatible)
               ?: actualsGroupedByCompatibility?.values?.flatten()
               ?: emptyList()
    }

    fun MemberDescriptor.findCompatibleExpectedForActual(commonModule: ModuleDescriptor): List<MemberDescriptor> =
            findExpectedForActual(this, commonModule)?.get(Compatible).orEmpty()

    private fun CallableMemberDescriptor.findNamesakesFromModule(module: ModuleDescriptor): Collection<CallableMemberDescriptor> {
        val containingDeclaration = containingDeclaration
        val scopes = when (containingDeclaration) {
            is PackageFragmentDescriptor -> {
                listOf(module.getPackage(containingDeclaration.fqName).memberScope)
            }
            is ClassDescriptor -> {
                val classes = containingDeclaration.findClassifiersFromModule(module).filterIsInstance<ClassDescriptor>()
                if (this is ConstructorDescriptor) return classes.flatMap { it.constructors }

                classes.map { it.unsubstitutedMemberScope }
            }
            else -> return emptyList()
        }

        return when (this) {
            is FunctionDescriptor -> scopes.flatMap { it.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED) }
            is PropertyDescriptor -> scopes.flatMap { it.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED) }
            else -> throw AssertionError("Unsupported declaration: $this")
        }
    }

    private fun ClassifierDescriptorWithTypeParameters.findClassifiersFromModule(
            module: ModuleDescriptor
    ): Collection<ClassifierDescriptorWithTypeParameters> {
        val classId = classId ?: return emptyList()

        fun MemberScope.getAllClassifiers(name: Name): Collection<ClassifierDescriptorWithTypeParameters> =
                getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == name }
                        .filterIsInstance<ClassifierDescriptorWithTypeParameters>()

        val segments = classId.relativeClassName.pathSegments()
        var classifiers = module.getPackage(classId.packageFqName).memberScope.getAllClassifiers(segments.first())

        for (name in segments.subList(1, segments.size)) {
            classifiers = classifiers.mapNotNull { classifier ->
                (classifier as? ClassDescriptor)?.unsubstitutedInnerClassesScope?.getContributedClassifier(
                        name, NoLookupLocation.FOR_ALREADY_TRACKED
                ) as? ClassifierDescriptorWithTypeParameters
            }
        }

        return classifiers
    }

    sealed class Compatibility {
        // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
        sealed class Incompatible(val reason: String?) : Compatibility() {
            // Callables

            object ParameterShape : Incompatible("parameter shapes are different (extension vs non-extension)")

            object ParameterCount : Incompatible("number of value parameters is different")
            object TypeParameterCount : Incompatible("number of type parameters is different")

            object ParameterTypes : Incompatible("parameter types are different")
            object ReturnType : Incompatible("return type is different")

            object ParameterNames : Incompatible("parameter names are different")
            object TypeParameterNames : Incompatible("names of type parameters are different")

            object ValueParameterHasDefault : Incompatible("some parameters have default values")
            object ValueParameterVararg : Incompatible("some value parameter is vararg in one declaration and non-vararg in the other")
            object ValueParameterNoinline : Incompatible("some value parameter is noinline in one declaration and not noinline in the other")
            object ValueParameterCrossinline : Incompatible("some value parameter is crossinline in one declaration and not crossinline in the other")

            // Functions

            object FunctionModifiersDifferent : Incompatible("modifiers are different (suspend)")
            object FunctionModifiersNotSubset : Incompatible("some modifiers on expected declaration are missing on the actual one (external, infix, inline, operator, tailrec)")

            // Properties

            object PropertyKind : Incompatible("property kinds are different (val vs var)")
            object PropertyModifiers : Incompatible("modifiers are different (const, lateinit)")

            // Classifiers

            object ClassKind : Incompatible("class kinds are different (class, interface, object, enum, annotation)")

            object ClassModifiers : Incompatible("modifiers are different (companion, inner)")

            object Supertypes : Incompatible("some supertypes are missing in the actual declaration")

            class ClassScopes(
                    val unfulfilled: List<Pair<MemberDescriptor, Map<Incompatible, Collection<MemberDescriptor>>>>
            ) : Incompatible("some expected members have no actual ones")

            object EnumEntries : Incompatible("some entries from expected enum are missing in the actual enum")

            // Common

            object Modality : Incompatible("modality is different")
            object Visibility : Incompatible("visibility is different")

            object TypeParameterUpperBounds : Incompatible("upper bounds of type parameters are different")
            object TypeParameterVariance : Incompatible("declaration-site variances of type parameters are different")
            object TypeParameterReified : Incompatible("some type parameter is reified in one declaration and non-reified in the other")

            object Unknown : Incompatible(null)
        }

        object Compatible : Compatibility()
    }

    // a is the declaration in common code, b is the definition in the platform-specific code
    private fun areCompatibleCallables(
            a: CallableMemberDescriptor,
            b: CallableMemberDescriptor,
            platformModule: ModuleDescriptor = b.module,
            parentSubstitutor: Substitutor? = null
    ): Compatibility {
        assert(a.name == b.name) { "This function should be invoked only for declarations with the same name: $a, $b" }
        assert(a.containingDeclaration is ClassDescriptor == b.containingDeclaration is ClassDescriptor) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $a, $b"
        }

        val aExtensionReceiver = a.extensionReceiverParameter
        val bExtensionReceiver = b.extensionReceiverParameter
        if ((aExtensionReceiver != null) != (bExtensionReceiver != null)) return Incompatible.ParameterShape

        val aParams = a.valueParameters
        val bParams = b.valueParameters
        if (aParams.size != bParams.size) return Incompatible.ParameterCount

        val aTypeParams = a.typeParameters
        val bTypeParams = b.typeParameters
        if (aTypeParams.size != bTypeParams.size) return Incompatible.TypeParameterCount

        val substitutor = Substitutor(aTypeParams, bTypeParams, parentSubstitutor)

        if (!areCompatibleTypeLists(aParams.map { substitutor(it.type) }, bParams.map { it.type }, platformModule) ||
            !areCompatibleTypes(aExtensionReceiver?.type?.let(substitutor), bExtensionReceiver?.type, platformModule))
            return Incompatible.ParameterTypes
        if (!areCompatibleTypes(substitutor(a.returnType), b.returnType, platformModule)) return Incompatible.ReturnType

        if (b.hasStableParameterNames() && !equalsBy(aParams, bParams, ValueParameterDescriptor::getName)) return Incompatible.ParameterNames
        if (!equalsBy(aTypeParams, bTypeParams, TypeParameterDescriptor::getName)) return Incompatible.TypeParameterNames

        if (a.modality != b.modality) return Incompatible.Modality
        if (a.visibility != b.visibility) return Incompatible.Visibility

        areCompatibleTypeParameters(aTypeParams, bTypeParams, platformModule, substitutor).let { if (it != Compatible) return it }

        if (!equalsBy(aParams, bParams, ValueParameterDescriptor::declaresDefaultValue)) return Incompatible.ValueParameterHasDefault
        if (!equalsBy(aParams, bParams, { p -> listOf(p.varargElementType != null) })) return Incompatible.ValueParameterVararg

        // Adding noinline/crossinline to parameters is disallowed, except if the expected declaration was not inline at all
        if (a is FunctionDescriptor && a.isInline) {
            if (aParams.indices.any { i -> !aParams[i].isNoinline && bParams[i].isNoinline }) return Incompatible.ValueParameterNoinline
            if (aParams.indices.any { i -> !aParams[i].isCrossinline && bParams[i].isCrossinline }) return Incompatible.ValueParameterCrossinline
        }

        when {
            a is FunctionDescriptor && b is FunctionDescriptor -> areCompatibleFunctions(a, b).let { if (it != Compatible) return it }
            a is PropertyDescriptor && b is PropertyDescriptor -> areCompatibleProperties(a, b).let { if (it != Compatible) return it }
            else -> throw AssertionError("Unsupported declarations: $a, $b")
        }

        return Compatible
    }

    private fun areCompatibleTypes(a: KotlinType?, b: KotlinType?, platformModule: ModuleDescriptor): Boolean {
        if (a == null) return b == null
        if (b == null) return false

        with(NewKotlinTypeChecker) {
            val context = object : TypeCheckerContext(false) {
                override fun areEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
                    return isExpectedClassAndActualTypeAlias(a, b, platformModule) ||
                           isExpectedClassAndActualTypeAlias(b, a, platformModule) ||
                           super.areEqualTypeConstructors(a, b)
                }
            }
            return context.equalTypes(a.unwrap(), b.unwrap())
        }
    }

    // For example, expectedTypeConstructor may be the expected class kotlin.text.StringBuilder, while actualTypeConstructor
    // is java.lang.StringBuilder. For the purposes of type compatibility checking, we must consider these types equal here.
    // Note that the case of an "actual class" works as expected though, because the actual class by definition has the same FQ name
    // as the corresponding expected class, so their type constructors are equal as per AbstractClassTypeConstructor#equals
    private fun isExpectedClassAndActualTypeAlias(
            expectedTypeConstructor: TypeConstructor,
            actualTypeConstructor: TypeConstructor,
            platformModule: ModuleDescriptor
    ): Boolean {
        val expected = expectedTypeConstructor.declarationDescriptor
        val actual = actualTypeConstructor.declarationDescriptor
        return expected is ClassifierDescriptorWithTypeParameters &&
               expected.isExpect &&
               actual is ClassifierDescriptorWithTypeParameters &&
               expected.findClassifiersFromModule(platformModule).any { classifier ->
                   // Note that it's fine to only check that this "actual typealias" expands to the expected class, without checking
                   // whether the type arguments in the expansion are in the correct order or have the correct variance, because we only
                   // allow simple cases like "actual typealias Foo<A, B> = FooImpl<A, B>", see DeclarationsChecker#checkActualTypeAlias
                   (classifier as? TypeAliasDescriptor)?.classDescriptor == actual
               }
    }

    private fun areCompatibleTypeLists(a: List<KotlinType?>, b: List<KotlinType?>, platformModule: ModuleDescriptor): Boolean {
        for (i in a.indices) {
            if (!areCompatibleTypes(a[i], b[i], platformModule)) return false
        }
        return true
    }

    private fun areCompatibleTypeParameters(
            a: List<TypeParameterDescriptor>,
            b: List<TypeParameterDescriptor>,
            platformModule: ModuleDescriptor,
            substitutor: Substitutor
    ): Compatibility {
        if (!areCompatibleTypeLists(a.map { substitutor(it.defaultType) }, b.map { it.defaultType }, platformModule))
            return Incompatible.TypeParameterUpperBounds
        if (!equalsBy(a, b, TypeParameterDescriptor::getVariance)) return Incompatible.TypeParameterVariance

        // Removing "reified" from an expected function's type parameter is fine
        if (a.indices.any { i -> !a[i].isReified && b[i].isReified }) return Incompatible.TypeParameterReified

        return Compatible
    }

    private fun areCompatibleFunctions(a: FunctionDescriptor, b: FunctionDescriptor): Compatibility {
        if (!equalBy(a, b) { f -> f.isSuspend }) return Incompatible.FunctionModifiersDifferent

        if (a.isExternal && !b.isExternal ||
            a.isInfix && !b.isInfix ||
            a.isInline && !b.isInline ||
            a.isOperator && !b.isOperator ||
            a.isTailrec && !b.isTailrec) return Incompatible.FunctionModifiersNotSubset

        return Compatible
    }

    private fun areCompatibleProperties(a: PropertyDescriptor, b: PropertyDescriptor): Compatibility {
        if (!equalBy(a, b) { p -> p.isVar }) return Incompatible.PropertyKind
        if (!equalBy(a, b) { p -> listOf(p.isConst, p.isLateInit) }) return Incompatible.PropertyModifiers

        return Compatible
    }

    private fun areCompatibleClassifiers(a: ClassDescriptor, other: ClassifierDescriptor): Compatibility {
        // Can't check FQ names here because nested expected class may be implemented via actual typealias's expansion with the other FQ name
        assert(a.name == other.name) { "This function should be invoked only for declarations with the same name: $a, $other" }

        val b = when (other) {
            is ClassDescriptor -> other
            is TypeAliasDescriptor -> other.classDescriptor ?: return Compatible // do not report extra error on erroneous typealias
            else -> throw AssertionError("Incorrect actual classifier for $a: $other")
        }

        if (a.kind != b.kind) return Incompatible.ClassKind

        if (!equalBy(a, b) { listOf(it.isCompanionObject, it.isInner) }) return Incompatible.ClassModifiers

        val aTypeParams = a.declaredTypeParameters
        val bTypeParams = b.declaredTypeParameters
        if (aTypeParams.size != bTypeParams.size) return Incompatible.TypeParameterCount

        if (a.modality != b.modality && !(a.modality == Modality.FINAL && b.modality == Modality.OPEN)) return Incompatible.Modality

        if (a.visibility != b.visibility) return Incompatible.Visibility

        val platformModule = other.module
        val substitutor = Substitutor(aTypeParams, bTypeParams)
        areCompatibleTypeParameters(aTypeParams, bTypeParams, platformModule, substitutor).let { if (it != Compatible) return it }

        // Subtract kotlin.Any from supertypes because it's implicitly added if no explicit supertype is specified,
        // and not added if an explicit supertype _is_ specified
        val aSupertypes = a.typeConstructor.supertypes.filterNot(KotlinBuiltIns::isAny)
        val bSupertypes = b.typeConstructor.supertypes.filterNot(KotlinBuiltIns::isAny)
        if (aSupertypes.map(substitutor).any { aSupertype ->
            bSupertypes.none { bSupertype -> areCompatibleTypes(aSupertype, bSupertype, platformModule) }
        }) return Incompatible.Supertypes

        areCompatibleClassScopes(a, b, platformModule, substitutor).let { if (it != Compatible) return it }

        return Compatible
    }


    private fun areCompatibleClassScopes(
            a: ClassDescriptor,
            b: ClassDescriptor,
            platformModule: ModuleDescriptor,
            substitutor: Substitutor
    ): Compatibility {
        val unfulfilled = arrayListOf<Pair<MemberDescriptor, Map<Incompatible, MutableCollection<MemberDescriptor>>>>()

        val bMembersByName = b.getMembers().groupBy { it.name }

        outer@ for (aMember in a.getMembers()) {
            if (aMember is CallableMemberDescriptor && !aMember.kind.isReal) continue

            val bMembers = bMembersByName[aMember.name]?.filter { bMember ->
                aMember is CallableMemberDescriptor && bMember is CallableMemberDescriptor ||
                aMember is ClassDescriptor && bMember is ClassDescriptor
            }.orEmpty()

            val mapping = bMembers.keysToMap { bMember ->
                when (aMember) {
                    is CallableMemberDescriptor ->
                            areCompatibleCallables(aMember, bMember as CallableMemberDescriptor, platformModule, substitutor)
                    is ClassDescriptor ->
                            areCompatibleClassifiers(aMember, bMember as ClassDescriptor)
                    else -> throw UnsupportedOperationException("Unsupported declaration: $aMember ($bMembers)")
                }
            }
            if (mapping.values.any { it == Compatible }) continue

            val incompatibilityMap = mutableMapOf<Incompatible, MutableCollection<MemberDescriptor>>()
            for ((descriptor, compatibility) in mapping) {
                when (compatibility) {
                    Compatible -> continue@outer
                    is Incompatible -> incompatibilityMap.getOrPut(compatibility) { SmartList() }.add(descriptor)
                }
            }

            unfulfilled.add(aMember to incompatibilityMap)
        }

        if (a.kind == ClassKind.ENUM_CLASS) {
            fun ClassDescriptor.enumEntries() =
                    unsubstitutedMemberScope.getDescriptorsFiltered().filter(DescriptorUtils::isEnumEntry).map { it.name }
            val aEntries = a.enumEntries()
            val bEntries = b.enumEntries()

            if (!bEntries.containsAll(aEntries)) return Incompatible.EnumEntries
        }

        // TODO: check static scope?

        if (unfulfilled.isEmpty()) return Compatible

        return Incompatible.ClassScopes(unfulfilled)
    }

    private fun ClassDescriptor.getMembers(name: Name? = null): Collection<MemberDescriptor> {
        val nameFilter = if (name != null) { it -> it == name } else MemberScope.ALL_NAME_FILTER
        return defaultType.memberScope
                .getDescriptorsFiltered(nameFilter = nameFilter)
                .filterIsInstance<MemberDescriptor>()
                .filterNot(DescriptorUtils::isEnumEntry)
                .plus(constructors.filter { nameFilter(it.name) })
    }

    private inline fun <T, K> equalBy(first: T, second: T, selector: (T) -> K): Boolean =
            selector(first) == selector(second)

    private inline fun <T, K> equalsBy(first: List<T>, second: List<T>, selector: (T) -> K): Boolean {
        for (i in first.indices) {
            if (selector(first[i]) != selector(second[i])) return false
        }

        return true
    }

    // This substitutor takes the type from A's signature and returns the type that should be in that place in B's signature
    private class Substitutor(
            aTypeParams: List<TypeParameterDescriptor>,
            bTypeParams: List<TypeParameterDescriptor>,
            private val parent: Substitutor? = null
    ) : (KotlinType?) -> KotlinType? {
        private val typeSubstitutor = TypeSubstitutor.create(
                TypeConstructorSubstitution.createByParametersMap(aTypeParams.keysToMap {
                    bTypeParams[it.index].defaultType.asTypeProjection()
                })
        )

        override fun invoke(type: KotlinType?): KotlinType? =
                (parent?.invoke(type) ?: type)?.asTypeProjection()?.let(typeSubstitutor::substitute)?.type
    }
}
