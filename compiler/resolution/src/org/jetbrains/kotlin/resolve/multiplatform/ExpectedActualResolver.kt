/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Compatible
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver.Compatibility.Incompatible
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

object ExpectedActualResolver {
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

    fun findActualForExpected(expected: MemberDescriptor, platformModule: ModuleDescriptor): Map<Compatibility, List<MemberDescriptor>>? {
        return when (expected) {
            is CallableMemberDescriptor -> {
                expected.findNamesakesFromModule(platformModule).filter { actual ->
                    expected != actual && !actual.isExpect &&
                    // TODO: support non-source definitions (e.g. from Java)
                    actual.source.containingFile != SourceFile.NO_SOURCE_FILE
                }.groupBy { actual ->
                    areCompatibleCallables(expected, actual)
                }
            }
            is ClassDescriptor -> {
                expected.findClassifiersFromModule(platformModule).filter { actual ->
                    expected != actual && !actual.isExpect &&
                    actual.source.containingFile != SourceFile.NO_SOURCE_FILE
                }.groupBy { actual ->
                    areCompatibleClassifiers(expected, actual)
                }
            }
            else -> null
        }
    }

    fun findExpectedForActual(actual: MemberDescriptor, commonModule: ModuleDescriptor): Map<Compatibility, List<MemberDescriptor>>? {
        return when (actual) {
            is CallableMemberDescriptor -> {
                val container = actual.containingDeclaration
                val candidates = when (container) {
                    is ClassDescriptor -> {
                        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
                        val expectedClass =
                            findExpectedForActual(container, commonModule)?.values?.firstOrNull()?.firstOrNull() as? ClassDescriptor
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
        // For IncompatibilityKind.STRONG `actual` declaration is considered as overload and error reports on expected declaration
        enum class IncompatibilityKind {
            WEAK, STRONG
        }

        // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
        sealed class Incompatible(val reason: String?, val kind: IncompatibilityKind = IncompatibilityKind.WEAK) : Compatibility() {
            // Callables

            object ParameterShape : Incompatible("parameter shapes are different (extension vs non-extension)", IncompatibilityKind.STRONG)

            object ParameterCount : Incompatible("number of value parameters is different", IncompatibilityKind.STRONG)
            object TypeParameterCount : Incompatible("number of type parameters is different", IncompatibilityKind.STRONG)

            object ParameterTypes : Incompatible("parameter types are different", IncompatibilityKind.STRONG)
            object ReturnType : Incompatible("return type is different", IncompatibilityKind.STRONG)

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

            object TypeParameterUpperBounds : Incompatible("upper bounds of type parameters are different", IncompatibilityKind.STRONG)
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
        if (!valueParametersCountCompatible(a, b, aParams, bParams)) {
            return Incompatible.ParameterCount
        }

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

        if (!areCompatibleModalities(a.modality, b.modality)) return Incompatible.Modality
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

    private fun valueParametersCountCompatible(
        a: CallableMemberDescriptor,
        b: CallableMemberDescriptor,
        aParams: List<ValueParameterDescriptor>,
        bParams: List<ValueParameterDescriptor>
    ): Boolean {
        if (aParams.size == bParams.size) return true

        return if (a.isAnnotationConstructor() && b.isAnnotationConstructor())
            aParams.isEmpty() && bParams.all { it.declaresDefaultValue() }
        else
            false
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
        for (i in a.indices) {
            val aBounds = a[i].upperBounds
            val bBounds = b[i].upperBounds
            if (aBounds.size != bBounds.size || !areCompatibleTypeLists(aBounds.map(substitutor), bBounds, platformModule)) {
                return Incompatible.TypeParameterUpperBounds
            }
        }

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

        if (!areCompatibleModalities(a.modality, b.modality)) return Incompatible.Modality

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

    private fun areCompatibleModalities(a: Modality, b: Modality): Boolean {
        return a == Modality.FINAL && b == Modality.OPEN ||
                a == b
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
