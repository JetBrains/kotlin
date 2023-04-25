/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Compatible
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.keysToMap

object ExpectedActualResolver {

    fun findActualForExpected(
        expected: MemberDescriptor,
        platformModule: ModuleDescriptor,
        moduleVisibilityFilter: ModuleFilter = allModulesProvidingActualsFor(expected.module, platformModule),
    ): Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>? {
        return when (expected) {
            is CallableMemberDescriptor -> {
                expected.findNamesakesFromModule(platformModule, moduleVisibilityFilter).filter { actual ->
                    expected != actual && !actual.isExpect &&
                            // TODO: use some other way to determine that the declaration is from Kotlin.
                            //       This way behavior differs between fast and PSI-based Java class reading mode
                            // TODO: support non-source definitions (e.g. from Java)
                            actual.couldHaveASource
                }.groupBy { actual ->
                    areCompatibleCallables(expected, actual, platformModule)
                }
            }
            is ClassDescriptor -> {
                expected.findClassifiersFromModule(platformModule, moduleVisibilityFilter).filter { actual ->
                    expected != actual && !actual.isExpect && actual.couldHaveASource
                }.groupBy { actual ->
                    areCompatibleClassifiers(expected, actual)
                }
            }
            else -> null
        }
    }

    fun findExpectedForActual(
        actual: MemberDescriptor,
        moduleFilter: (ModuleDescriptor) -> Boolean = allModulesProvidingExpectsFor(actual.module)
    ): Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>? {
        return when (actual) {
            is CallableMemberDescriptor -> {
                val container = actual.containingDeclaration
                val candidates = when (container) {
                    is ClassifierDescriptorWithTypeParameters -> {
                        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
                        val expectedClass =
                            findExpectedForActual(container, moduleFilter)?.values?.firstOrNull()?.firstOrNull() as? ClassDescriptor
                        expectedClass?.getMembers(actual.name)?.filterIsInstance<CallableMemberDescriptor>().orEmpty()
                    }
                    is PackageFragmentDescriptor -> actual.findNamesakesFromModule(actual.module, moduleFilter)
                    else -> return null // do not report anything for incorrect code, e.g. 'actual' local function
                }

                candidates.filter { declaration ->
                    actual != declaration && declaration.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE && declaration.isExpect
                }.groupBy { declaration ->
                    // TODO: optimize by caching this per actual-expected class pair, do not create a new substitutor for each actual member
                    val substitutor =
                        if (container is ClassDescriptor) {
                            val expectedClass = declaration.containingDeclaration as ClassDescriptor
                            // TODO: this might not work for members of inner generic classes
                            if (expectedClass.declaredTypeParameters.size == container.declaredTypeParameters.size) {
                                Substitutor(expectedClass.declaredTypeParameters, container.declaredTypeParameters)
                            } else null
                        } else null
                    areCompatibleCallables(declaration, actual, parentSubstitutor = substitutor)
                }
            }
            is ClassifierDescriptorWithTypeParameters -> {
                actual.findClassifiersFromModule(actual.module, moduleFilter).filter { declaration ->
                    actual != declaration && declaration is ClassDescriptor && declaration.isExpect
                }.groupBy { expected ->
                    areCompatibleClassifiers(expected as ClassDescriptor, actual)
                }
            }
            else -> null
        }
    }

    private fun CallableMemberDescriptor.findNamesakesFromModule(
        module: ModuleDescriptor,
        moduleFilter: (ModuleDescriptor) -> Boolean
    ): Collection<CallableMemberDescriptor> {
        val scopes = when (val containingDeclaration = containingDeclaration) {
            is PackageFragmentDescriptor -> {
                listOf(module.getPackage(containingDeclaration.fqName).memberScope)
            }
            is ClassDescriptor -> {
                val classes = containingDeclaration.findClassifiersFromModule(module, moduleFilter)
                    .mapNotNull { if (it is TypeAliasDescriptor) it.classDescriptor else it }
                    .filterIsInstance<ClassDescriptor>()
                if (this is ConstructorDescriptor) return classes.flatMap { it.constructors }

                classes.map { it.unsubstitutedMemberScope }
            }
            else -> return emptyList()
        }

        return when (this) {
            is FunctionDescriptor -> scopes.flatMap { scope ->
                scope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { it == name }
                    .filter { it.name == name }
                    .filterIsInstance<CallableMemberDescriptor>()
            }

            is PropertyDescriptor -> scopes.flatMap { scope ->
                scope.getContributedDescriptors(DescriptorKindFilter.VARIABLES) { it == name }
                    .filter { it.name == name }
                    .filterIsInstance<CallableMemberDescriptor>()
            }

            else -> throw AssertionError("Unsupported declaration: $this")
        }.applyFilter(moduleFilter)
    }

    private fun ClassifierDescriptorWithTypeParameters.findClassifiersFromModule(
        module: ModuleDescriptor,
        moduleFilter: (ModuleDescriptor) -> Boolean
    ): Collection<ClassifierDescriptorWithTypeParameters> {
        val classId = classId ?: return emptyList()

        fun MemberScope.getAllClassifiers(name: Name): Collection<ClassifierDescriptorWithTypeParameters> =
            getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == name }
                .filterIsInstance<ClassifierDescriptorWithTypeParameters>()

        val segments = classId.relativeClassName.pathSegments()
        var classifiers = module.getPackage(classId.packageFqName).memberScope.getAllClassifiers(segments.first())
        classifiers = classifiers.applyFilter(moduleFilter)

        for (name in segments.subList(1, segments.size)) {
            classifiers = classifiers.mapNotNull { classifier ->
                (classifier as? ClassDescriptor)?.unsubstitutedInnerClassesScope?.getContributedClassifier(
                    name, NoLookupLocation.FOR_ALREADY_TRACKED
                ) as? ClassifierDescriptorWithTypeParameters
            }
        }

        return classifiers
    }

    // a is the declaration in common code, b is the definition in the platform-specific code
    private fun areCompatibleCallables(
        a: CallableMemberDescriptor,
        b: CallableMemberDescriptor,
        platformModule: ModuleDescriptor = b.module,
        parentSubstitutor: Substitutor? = null
    ): ExpectActualCompatibility<MemberDescriptor> {
        assert(a.name == b.name) {
            "This function should be invoked only for declarations with the same name: $a, $b"
        }
        assert(a.containingDeclaration is ClassifierDescriptorWithTypeParameters == b.containingDeclaration is ClassifierDescriptorWithTypeParameters) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $a, $b"
        }

        if (a is FunctionDescriptor && b !is FunctionDescriptor ||
            a !is FunctionDescriptor && b is FunctionDescriptor
        ) return Incompatible.CallableKind

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
            !areCompatibleTypes(aExtensionReceiver?.type?.let(substitutor), bExtensionReceiver?.type, platformModule)
        ) return Incompatible.ParameterTypes
        if (!areCompatibleTypes(substitutor(a.returnType), b.returnType, platformModule)) return Incompatible.ReturnType

        if (b.hasStableParameterNames() &&
            !equalsBy(aParams, bParams, ValueParameterDescriptor::getName)
        ) return Incompatible.ParameterNames
        if (!equalsBy(aTypeParams, bTypeParams, TypeParameterDescriptor::getName)) return Incompatible.TypeParameterNames

        if (!areCompatibleModalities(a.modality, b.modality)) return Incompatible.Modality
        if (!areDeclarationsWithCompatibleVisibilities(a, b)) return Incompatible.Visibility

        areCompatibleTypeParameters(aTypeParams, bTypeParams, platformModule, substitutor).let { if (it != Compatible) return it }

        if (!equalsBy(aParams, bParams) { p -> listOf(p.varargElementType != null) }) return Incompatible.ValueParameterVararg

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

        return if (platformModule.isTypeRefinementEnabled()) {
            areCompatibleTypesViaTypeRefinement(a, b, platformModule)
        } else {
            areCompatibleTypesViaTypeContext(a, b, platformModule)
        }
    }

    @OptIn(TypeRefinement::class)
    private fun areCompatibleTypesViaTypeRefinement(a: KotlinType, b: KotlinType, platformModule: ModuleDescriptor): Boolean {
        val typeRefinerForPlatformModule = platformModule.getKotlinTypeRefiner().let { moduleRefiner ->
            if (moduleRefiner is KotlinTypeRefiner.Default)
                KotlinTypeRefinerImpl.createStandaloneInstanceFor(platformModule)
            else
                moduleRefiner
        }

        return areCompatibleTypes(
            a, b,
            typeSystemContext = SimpleClassicTypeSystemContext,
            kotlinTypeRefiner = typeRefinerForPlatformModule,
        )
    }

    private fun areCompatibleTypesViaTypeContext(a: KotlinType, b: KotlinType, platformModule: ModuleDescriptor): Boolean {
        val typeSystemContext = object : ClassicTypeSystemContext {
            override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
                require(c1 is TypeConstructor)
                require(c2 is TypeConstructor)
                return isExpectedClassAndActualTypeAlias(c1, c2, platformModule) ||
                        isExpectedClassAndActualTypeAlias(c2, c1, platformModule) ||
                        super.areEqualTypeConstructors(c1, c2)
            }
        }

        return areCompatibleTypes(
            a, b,
            typeSystemContext = typeSystemContext,
            kotlinTypeRefiner = KotlinTypeRefiner.Default,
        )
    }

    private fun areCompatibleTypes(
        a: KotlinType,
        b: KotlinType,
        typeSystemContext: ClassicTypeSystemContext,
        kotlinTypeRefiner: KotlinTypeRefiner,
    ): Boolean {
        with(NewKotlinTypeCheckerImpl(kotlinTypeRefiner)) {
            return createClassicTypeCheckerState(
                isErrorTypeEqualsToAnything = false,
                typeSystemContext = typeSystemContext,
                kotlinTypeRefiner = kotlinTypeRefiner,
            ).equalTypes(a.unwrap(), b.unwrap())
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
                expected.findClassifiersFromModule(platformModule, moduleFilter = ALL_MODULES).any { classifier ->
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
    ): ExpectActualCompatibility<MemberDescriptor> {
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

    private fun areCompatibleFunctions(a: FunctionDescriptor, b: FunctionDescriptor): ExpectActualCompatibility<MemberDescriptor> {
        if (!equalBy(a, b) { f -> f.isSuspend }) return Incompatible.FunctionModifiersDifferent

        if (a.isExternal && !b.isExternal ||
            a.isInfix && !b.isInfix ||
            a.isInline && !b.isInline ||
            a.isOperator && !b.isOperator ||
            a.isTailrec && !b.isTailrec
        ) return Incompatible.FunctionModifiersNotSubset

        return Compatible
    }

    private fun areCompatibleProperties(
        expected: PropertyDescriptor,
        actual: PropertyDescriptor,
    ): ExpectActualCompatibility<MemberDescriptor> {
        return when {
            !equalBy(expected, actual) { p -> p.isVar } -> Incompatible.PropertyKind
            !equalBy(expected, actual) { p -> p.isLateInit } -> Incompatible.PropertyLateinitModifier
            expected.isConst && !actual.isConst -> Incompatible.PropertyConstModifier
            !arePropertySettersWithCompatibleVisibilities(expected, actual) -> Incompatible.PropertySetterVisibility
            else -> Compatible
        }
    }

    private fun arePropertySettersWithCompatibleVisibilities(expected: PropertyDescriptor, actual: PropertyDescriptor): Boolean {
        val expectedSetter = expected.setter
        val actualSetter = actual.setter
        if (expectedSetter == null || actualSetter == null) {
            return true
        }
        return areDeclarationsWithCompatibleVisibilities(expectedSetter, actualSetter)
    }

    private fun areCompatibleClassifiers(a: ClassDescriptor, other: ClassifierDescriptor): ExpectActualCompatibility<MemberDescriptor> {
        // Can't check FQ names here because nested expected class may be implemented via actual typealias's expansion with the other FQ name
        assert(a.name == other.name) { "This function should be invoked only for declarations with the same name: $a, $other" }

        val b = when (other) {
            is ClassDescriptor -> other
            is TypeAliasDescriptor -> other.classDescriptor ?: return Compatible // do not report extra error on erroneous typealias
            else -> throw AssertionError("Incorrect actual classifier for $a: $other")
        }

        if (a.kind != b.kind) return Incompatible.ClassKind

        if (!equalBy(a, b) { listOf(it.isCompanionObject, it.isInner, it.isInline || it.isValue) }) return Incompatible.ClassModifiers
        if (a.isFun && !b.isFun && b.isNotJavaSamInterface()) {
            return Incompatible.FunInterfaceModifier
        }

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
            }
        ) return Incompatible.Supertypes

        areCompatibleClassScopes(a, b, platformModule, substitutor).let { if (it != Compatible) return it }

        return Compatible
    }

    private fun areCompatibleModalities(a: Modality, b: Modality): Boolean {
        return a == Modality.FINAL && b == Modality.OPEN ||
                a == b
    }

    private fun areDeclarationsWithCompatibleVisibilities(
        a: CallableMemberDescriptor,
        b: CallableMemberDescriptor
    ): Boolean {
        val compare = DescriptorVisibilities.compare(a.visibility, b.visibility)
        return if (a.isOverridable) {
            // For overridable declarations visibility should match precisely, see KT-19664
            compare == 0
        } else {
            // For non-overridable declarations actuals are allowed to have more permissive visibility
            compare != null && compare <= 0
        }
    }

    private fun areCompatibleClassScopes(
        a: ClassDescriptor,
        b: ClassDescriptor,
        platformModule: ModuleDescriptor,
        substitutor: Substitutor
    ): ExpectActualCompatibility<MemberDescriptor> {
        val unfulfilled = arrayListOf<Pair<MemberDescriptor, Map<Incompatible<MemberDescriptor>, MutableCollection<MemberDescriptor>>>>()

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

            val incompatibilityMap = mutableMapOf<Incompatible<MemberDescriptor>, MutableCollection<MemberDescriptor>>()
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

// FIXME(dsavvinov): review clients, as they won't work properly in HMPP projects
@JvmOverloads
fun MemberDescriptor.findCompatibleActualsForExpected(
    platformModule: ModuleDescriptor, moduleFilter: ModuleFilter = allModulesProvidingActualsFor(module, platformModule)
): List<MemberDescriptor> =
    ExpectedActualResolver.findActualForExpected(this, platformModule, moduleFilter)?.get(Compatible).orEmpty()

@JvmOverloads
fun MemberDescriptor.findAnyActualsForExpected(
    platformModule: ModuleDescriptor, moduleFilter: ModuleFilter = allModulesProvidingActualsFor(module, platformModule)
): List<MemberDescriptor> {
    val actualsGroupedByCompatibility = ExpectedActualResolver.findActualForExpected(this, platformModule, moduleFilter)
    return actualsGroupedByCompatibility?.get(Compatible)
        ?: actualsGroupedByCompatibility?.values?.flatten()
        ?: emptyList()
}

fun MemberDescriptor.findCompatibleExpectsForActual(
    moduleFilter: ModuleFilter = allModulesProvidingExpectsFor(module)
): List<MemberDescriptor> =
    ExpectedActualResolver.findExpectedForActual(this, moduleFilter)?.get(Compatible).orEmpty()

fun DeclarationDescriptor.findExpects(): List<MemberDescriptor> {
    if (this !is MemberDescriptor) return emptyList()
    return this.findCompatibleExpectsForActual()
}

fun DeclarationDescriptor.findActuals(inModule: ModuleDescriptor): List<MemberDescriptor> {
    if (this !is MemberDescriptor) return emptyList()
    return this.findCompatibleActualsForExpected(inModule)
}

// TODO: Klibs still need to better handle source in deserialized descriptors.
val DeclarationDescriptorWithSource.couldHaveASource: Boolean
    get() = this.source.containingFile != SourceFile.NO_SOURCE_FILE ||
            this is DeserializedDescriptor

private fun ClassDescriptor.isNotJavaSamInterface(): Boolean {
    return isDefinitelyNotSamInterface || defaultFunctionTypeForSamInterface == null
}