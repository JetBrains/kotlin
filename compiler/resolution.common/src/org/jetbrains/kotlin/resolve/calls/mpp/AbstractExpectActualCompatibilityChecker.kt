/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.enumMapOf
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

object AbstractExpectActualCompatibilityChecker {
    fun <T : DeclarationSymbolMarker> areCompatibleClassifiers(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassLikeSymbol: ClassLikeSymbolMarker,
        context: ExpectActualMatchingContext<T>,
    ): ExpectActualCompatibility<T> {
        val result = with(context) {
            areCompatibleClassifiers(expectClassSymbol, actualClassLikeSymbol, parentSubstitutor = null)
        }
        @Suppress("UNCHECKED_CAST")
        return result as ExpectActualCompatibility<T>
    }

    fun <T : DeclarationSymbolMarker> areCompatibleCallables(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        parentSubstitutor: TypeSubstitutorMarker?,
        expectContainingClass: RegularClassSymbolMarker?,
        actualContainingClass: RegularClassSymbolMarker?,
        context: ExpectActualMatchingContext<T>,
    ): ExpectActualCompatibility<T> {
        val result = with(context) {
            areCompatibleCallables(expectDeclaration, actualDeclaration, parentSubstitutor, expectContainingClass, actualContainingClass)
        }
        @Suppress("UNCHECKED_CAST")
        return result as ExpectActualCompatibility<T>
    }

    fun <T : DeclarationSymbolMarker> matchSingleExpectTopLevelDeclarationAgainstPotentialActuals(
        expectDeclaration: DeclarationSymbolMarker,
        actualDeclarations: List<DeclarationSymbolMarker>,
        context: ExpectActualMatchingContext<T>,
    ) {
        with(context) {
            matchSingleExpectAgainstPotentialActuals(
                expectDeclaration,
                actualDeclarations,
                substitutor = null,
                expectClassSymbol = null,
                actualClassSymbol = null,
                unfulfilled = null
            )
        }
    }

    context(ExpectActualMatchingContext<*>)
    @Suppress("warnings")
    private fun areCompatibleClassifiers(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassLikeSymbol: ClassLikeSymbolMarker,
        parentSubstitutor: TypeSubstitutorMarker?
    ): ExpectActualCompatibility<*> {
        // Can't check FQ names here because nested expected class may be implemented via actual typealias's expansion with the other FQ name
        require(expectClassSymbol.name == actualClassLikeSymbol.name) {
            "This function should be invoked only for declarations with the same name: $expectClassSymbol, $actualClassLikeSymbol"
        }

        val actualClass = when (actualClassLikeSymbol) {
            is RegularClassSymbolMarker -> actualClassLikeSymbol
            is TypeAliasSymbolMarker -> actualClassLikeSymbol.expandToRegularClass()
                ?: return ExpectActualCompatibility.Compatible // do not report extra error on erroneous typealias
            else -> error("Incorrect actual classifier for $expectClassSymbol: $actualClassLikeSymbol")
        }

        if (expectClassSymbol.classKind != actualClass.classKind) return Incompatible.ClassKind

        if (!equalBy(expectClassSymbol, actualClass) { listOf(it.isCompanion, it.isInner, it.isInline || it.isValue) }) {
            return Incompatible.ClassModifiers
        }

        if (expectClassSymbol.isFun && !actualClass.isFun && actualClass.isNotSamInterface()) {
            return Incompatible.FunInterfaceModifier
        }

        val expectTypeParameterSymbols = expectClassSymbol.typeParameters
        val actualTypeParameterSymbols = actualClass.typeParameters
        if (expectTypeParameterSymbols.size != actualTypeParameterSymbols.size) {
            return Incompatible.TypeParameterCount
        }

        if (!areCompatibleModalities(expectClassSymbol.modality, actualClass.modality)) {
            return Incompatible.Modality
        }

        if (expectClassSymbol.visibility != actualClass.visibility) {
            return Incompatible.Visibility
        }

        val substitutor = createExpectActualTypeParameterSubstitutor(
            expectTypeParameterSymbols,
            actualTypeParameterSymbols,
            parentSubstitutor
        )

        areCompatibleTypeParameters(expectTypeParameterSymbols, actualTypeParameterSymbols, substitutor).let {
            if (it != ExpectActualCompatibility.Compatible) {
                return it
            }
        }

        // Subtract kotlin.Any from supertypes because it's implicitly added if no explicit supertype is specified,
        // and not added if an explicit supertype _is_ specified
        val expectSupertypes = expectClassSymbol.superTypes.filterNot { it.typeConstructor().isAnyConstructor() }
        val actualSupertypes = actualClass.superTypes.filterNot { it.typeConstructor().isAnyConstructor() }
        if (
            expectSupertypes.map { substitutor.safeSubstitute(it) }.any { expectSupertype ->
                actualSupertypes.none { actualSupertype ->
                    areCompatibleExpectActualTypes(expectSupertype, actualSupertype)
                }
            }
        ) {
            return Incompatible.Supertypes
        }

        areCompatibleClassScopes(expectClassSymbol, actualClass, substitutor).let {
            if (it != ExpectActualCompatibility.Compatible) {
                return it
            }
        }

        return ExpectActualCompatibility.Compatible
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleClassScopes(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
        substitutor: TypeSubstitutorMarker,
    ): ExpectActualCompatibility<*> {
        val unfulfilled = arrayListOf<Pair<DeclarationSymbolMarker, Map<Incompatible<*>, List<DeclarationSymbolMarker?>>>>()

        val actualMembersByName = actualClassSymbol.collectAllMembers(isActualDeclaration = true).groupBy { it.name }

        outer@ for (expectMember in expectClassSymbol.collectAllMembers(isActualDeclaration = false)) {
            if (expectMember is CallableSymbolMarker && expectMember.shouldSkipMatching(expectClassSymbol)) continue

            val actualMembers = actualMembersByName[expectMember.name]?.filter { actualMember ->
                expectMember is CallableSymbolMarker && actualMember is CallableSymbolMarker ||
                        expectMember is RegularClassSymbolMarker && actualMember is RegularClassSymbolMarker
            }.orEmpty()

            matchSingleExpectAgainstPotentialActuals(
                expectMember,
                actualMembers,
                substitutor,
                expectClassSymbol,
                actualClassSymbol,
                unfulfilled
            )
        }

        if (expectClassSymbol.classKind == ClassKind.ENUM_CLASS) {
            val aEntries = expectClassSymbol.collectEnumEntryNames()
            val bEntries = actualClassSymbol.collectEnumEntryNames()

            if (!bEntries.containsAll(aEntries)) return Incompatible.EnumEntries
        }

        // TODO: check static scope?

        if (unfulfilled.isEmpty()) return ExpectActualCompatibility.Compatible

        return Incompatible.ClassScopes(unfulfilled)
    }

    context(ExpectActualMatchingContext<*>)
    private fun matchSingleExpectAgainstPotentialActuals(
        expectMember: DeclarationSymbolMarker,
        actualMembers: List<DeclarationSymbolMarker>,
        substitutor: TypeSubstitutorMarker?,
        expectClassSymbol: RegularClassSymbolMarker?,
        actualClassSymbol: RegularClassSymbolMarker?,
        unfulfilled: MutableList<Pair<DeclarationSymbolMarker, Map<Incompatible<*>, List<DeclarationSymbolMarker?>>>>?
    ) {
        val mapping = actualMembers.keysToMap { actualMember ->
            when (expectMember) {
                is CallableSymbolMarker -> areCompatibleCallables(
                    expectMember,
                    actualMember as CallableSymbolMarker,
                    substitutor,
                    expectClassSymbol,
                    actualClassSymbol
                )

                is RegularClassSymbolMarker -> {
                    val parentSubstitutor = substitutor?.takeIf { !innerClassesCapturesOuterTypeParameters }
                    areCompatibleClassifiers(
                        expectMember,
                        actualMember as ClassLikeSymbolMarker,
                        parentSubstitutor
                    )
                }
                else -> error("Unsupported declaration: $expectMember ($actualMembers)")
            }
        }

        val incompatibilityMap = mutableMapOf<Incompatible<*>, MutableList<DeclarationSymbolMarker>>()
        for ((actualMember, compatibility) in mapping) {
            when (compatibility) {
                ExpectActualCompatibility.Compatible -> {
                    onMatchedMembers(expectMember, actualMember)
                    return
                }

                is Incompatible -> incompatibilityMap.getOrPut(compatibility) { SmartList() }.add(actualMember)
            }
        }

        unfulfilled?.add(expectMember to incompatibilityMap)
        onMismatchedMembersFromClassScope(expectMember, incompatibilityMap)
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleCallables(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        parentSubstitutor: TypeSubstitutorMarker?,
        expectContainingClass: RegularClassSymbolMarker?,
        actualContainingClass: RegularClassSymbolMarker?,
    ): ExpectActualCompatibility<*> {
        require(
            (expectDeclaration is ConstructorSymbolMarker && actualDeclaration is ConstructorSymbolMarker) ||
                    expectDeclaration.callableId.callableName == actualDeclaration.callableId.callableName
        ) {
            "This function should be invoked only for declarations with the same name: $expectDeclaration, $actualDeclaration"
        }
        require((expectDeclaration.dispatchReceiverType == null) == (actualDeclaration.dispatchReceiverType == null)) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $expectDeclaration, $actualDeclaration"
        }

        if (expectDeclaration is FunctionSymbolMarker != actualDeclaration is FunctionSymbolMarker) {
            return Incompatible.CallableKind
        }

        if (
            enumConstructorsAreAlwaysCompatible &&
            expectContainingClass?.classKind == ClassKind.ENUM_CLASS &&
            actualContainingClass?.classKind == ClassKind.ENUM_CLASS &&
            expectDeclaration is ConstructorSymbolMarker &&
            actualDeclaration is ConstructorSymbolMarker
        ) {
            return ExpectActualCompatibility.Compatible
        }

        val expectedReceiverType = expectDeclaration.extensionReceiverType
        val actualReceiverType = actualDeclaration.extensionReceiverType
        if ((expectedReceiverType != null) != (actualReceiverType != null)) {
            return Incompatible.ParameterShape
        }

        val expectedValueParameters = expectDeclaration.valueParameters
        val actualValueParameters = actualDeclaration.valueParameters
        if (!valueParametersCountCompatible(expectDeclaration, actualDeclaration, expectedValueParameters, actualValueParameters)) {
            return Incompatible.ParameterCount
        }

        val expectedTypeParameters = expectDeclaration.typeParameters
        val actualTypeParameters = actualDeclaration.typeParameters
        if (expectedTypeParameters.size != actualTypeParameters.size) {
            return Incompatible.TypeParameterCount
        }

        val substitutor = createExpectActualTypeParameterSubstitutor(
            expectedTypeParameters,
            actualTypeParameters,
            parentSubstitutor
        )

        if (
            !areCompatibleTypeLists(
                expectedValueParameters.toTypeList(substitutor),
                actualValueParameters.toTypeList(createEmptySubstitutor())
            ) ||
            !areCompatibleExpectActualTypes(
                expectedReceiverType?.let { substitutor.safeSubstitute(it) },
                actualReceiverType
            )
        ) {
            return Incompatible.ParameterTypes
        }

        if (shouldCheckReturnTypesOfCallables) {
            if (!areCompatibleExpectActualTypes(substitutor.safeSubstitute(expectDeclaration.returnType), actualDeclaration.returnType)) {
                return Incompatible.ReturnType
            }
        }

        if (actualDeclaration.hasStableParameterNames && !equalsBy(expectedValueParameters, actualValueParameters) { it.name }) {
            return Incompatible.ParameterNames
        }

        if (!equalsBy(expectedTypeParameters, actualTypeParameters) { it.name }) {
            return Incompatible.TypeParameterNames
        }

        val expectModality = expectDeclaration.modality
        val actualModality = actualDeclaration.modality
        if (
            !areCompatibleModalities(
                expectModality,
                actualModality,
                expectContainingClass?.modality,
                actualContainingClass?.modality
            )
        ) {
            return Incompatible.Modality
        }

        if (!areDeclarationsWithCompatibleVisibilities(expectDeclaration.visibility, expectModality, actualDeclaration.visibility)) {
            return Incompatible.Visibility
        }

        areCompatibleTypeParameters(expectedTypeParameters, actualTypeParameters, substitutor).let {
            if (it != ExpectActualCompatibility.Compatible) {
                return it
            }
        }

        if (!equalsBy(expectedValueParameters, actualValueParameters) { it.isVararg }) {
            return Incompatible.ValueParameterVararg
        }

        // Adding noinline/crossinline to parameters is disallowed, except if the expected declaration was not inline at all
        if (expectDeclaration is SimpleFunctionSymbolMarker && expectDeclaration.isInline) {
            if (expectedValueParameters.indices.any { i -> !expectedValueParameters[i].isNoinline && actualValueParameters[i].isNoinline }) {
                return Incompatible.ValueParameterNoinline
            }
            if (expectedValueParameters.indices.any { i -> !expectedValueParameters[i].isCrossinline && actualValueParameters[i].isCrossinline }) {
                return Incompatible.ValueParameterCrossinline
            }
        }

        when {
            expectDeclaration is FunctionSymbolMarker && actualDeclaration is FunctionSymbolMarker -> areCompatibleFunctions(
                expectDeclaration,
                actualDeclaration
            ).let { if (it != ExpectActualCompatibility.Compatible) return it }

            expectDeclaration is PropertySymbolMarker && actualDeclaration is PropertySymbolMarker -> areCompatibleProperties(
                expectDeclaration,
                actualDeclaration
            ).let { if (it != ExpectActualCompatibility.Compatible) return it }

            expectDeclaration is EnumEntrySymbolMarker && actualDeclaration is EnumEntrySymbolMarker -> {
                // do nothing, entries are matched only by name
            }

            else -> error("Unsupported declarations: $expectDeclaration, $actualDeclaration")
        }

        return ExpectActualCompatibility.Compatible
    }

    context(ExpectActualMatchingContext<*>)
    private fun valueParametersCountCompatible(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        expectValueParameters: List<ValueParameterSymbolMarker>,
        actualValueParameters: List<ValueParameterSymbolMarker>,
    ): Boolean {
        if (expectValueParameters.size == actualValueParameters.size) return true

        return if (expectDeclaration.isAnnotationConstructor() && actualDeclaration.isAnnotationConstructor()) {
            expectValueParameters.isEmpty() && actualValueParameters.all { it.hasDefaultValue }
        } else {
            false
        }
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleTypeLists(
        expectedTypes: List<KotlinTypeMarker?>,
        actualTypes: List<KotlinTypeMarker?>,
    ): Boolean {
        for (i in expectedTypes.indices) {
            if (!areCompatibleExpectActualTypes(expectedTypes[i], actualTypes[i])) {
                return false
            }
        }
        return true
    }

    private fun areCompatibleModalities(
        expectModality: Modality?,
        actualModality: Modality?,
        expectContainingClassModality: Modality? = null,
        actualContainingClassModality: Modality? = null,
    ): Boolean {
        val expectEffectiveModality = effectiveModality(expectModality, expectContainingClassModality)
        val actualEffectiveModality = effectiveModality(actualModality, actualContainingClassModality)

        return actualEffectiveModality in compatibleModalityMap.getValue(expectEffectiveModality)
    }

    /*
     * If containing class is final then all declarations in it effectively final
     */
    private fun effectiveModality(declarationModality: Modality?, containingClassModality: Modality?): Modality? {
        return when (containingClassModality) {
            Modality.FINAL -> Modality.FINAL
            else -> declarationModality
        }
    }

    /*
     * Key is expect modality, value is a set of compatible actual modalities
     */
    private val compatibleModalityMap: EnumMap<Modality, EnumSet<Modality>> = enumMapOf(
        Modality.ABSTRACT to enumSetOf(Modality.ABSTRACT),
        Modality.OPEN to enumSetOf(Modality.OPEN),
        Modality.FINAL to enumSetOf(Modality.OPEN, Modality.FINAL),
        Modality.SEALED to enumSetOf(Modality.SEALED),
    )

    private fun areDeclarationsWithCompatibleVisibilities(
        expectVisibility: Visibility,
        expectModality: Modality?,
        actualVisibility: Visibility,
    ): Boolean {
        val compare = Visibilities.compare(expectVisibility, actualVisibility)
        return if (expectModality != Modality.FINAL) {
            // For overridable declarations visibility should match precisely, see KT-19664
            compare == 0
        } else {
            // For non-overridable declarations actuals are allowed to have more permissive visibility
            compare != null && compare <= 0
        }
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleTypeParameters(
        expectTypeParameterSymbols: List<TypeParameterSymbolMarker>,
        actualTypeParameterSymbols: List<TypeParameterSymbolMarker>,
        substitutor: TypeSubstitutorMarker,
    ): ExpectActualCompatibility<*> {
        for (i in expectTypeParameterSymbols.indices) {
            val expectBounds = expectTypeParameterSymbols[i].bounds
            val actualBounds = actualTypeParameterSymbols[i].bounds
            if (
                expectBounds.size != actualBounds.size ||
                !areCompatibleTypeLists(expectBounds.map { substitutor.safeSubstitute(it) }, actualBounds)
            ) {
                return Incompatible.TypeParameterUpperBounds
            }
        }

        if (!equalsBy(expectTypeParameterSymbols, actualTypeParameterSymbols) { it.variance }) {
            return Incompatible.TypeParameterVariance
        }

        // Removing "reified" from an expected function's type parameter is fine
        if (
            expectTypeParameterSymbols.indices.any { i ->
                !expectTypeParameterSymbols[i].isReified && actualTypeParameterSymbols[i].isReified
            }
        ) {
            return Incompatible.TypeParameterReified
        }

        return ExpectActualCompatibility.Compatible
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleFunctions(
        expectFunction: CallableSymbolMarker,
        actualFunction: CallableSymbolMarker,
    ): ExpectActualCompatibility<*> {
        if (!equalBy(expectFunction, actualFunction) { f -> f.isSuspend }) {
            return Incompatible.FunctionModifiersDifferent
        }

        if (
            expectFunction.isInfix && !actualFunction.isInfix ||
            expectFunction.isInline && !actualFunction.isInline ||
            expectFunction.isOperator && !actualFunction.isOperator
        ) {
            return Incompatible.FunctionModifiersNotSubset
        }

        return ExpectActualCompatibility.Compatible
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleProperties(
        expected: PropertySymbolMarker,
        actual: PropertySymbolMarker,
    ): ExpectActualCompatibility<*> {
        return when {
            !equalBy(expected, actual) { p -> p.isVar } -> Incompatible.PropertyKind
            !equalBy(expected, actual) { p -> p.isLateinit } -> Incompatible.PropertyLateinitModifier
            expected.isConst && !actual.isConst -> Incompatible.PropertyConstModifier
            !arePropertySettersWithCompatibleVisibilities(expected, actual) -> Incompatible.PropertySetterVisibility
            else -> ExpectActualCompatibility.Compatible
        }
    }

    context(ExpectActualMatchingContext<*>)
    private fun arePropertySettersWithCompatibleVisibilities(
        expected: PropertySymbolMarker,
        actual: PropertySymbolMarker,
    ): Boolean {
        val expectedSetter = expected.setter ?: return true
        val actualSetter = actual.setter ?: return true
        return areDeclarationsWithCompatibleVisibilities(expectedSetter.visibility, expectedSetter.modality, actualSetter.visibility)
    }

    // ---------------------------------------- Utils ----------------------------------------

    context(ExpectActualMatchingContext<*>)
    private fun List<ValueParameterSymbolMarker>.toTypeList(substitutor: TypeSubstitutorMarker): List<KotlinTypeMarker> {
        return this.map { substitutor.safeSubstitute(it.returnType) }
    }

    private inline fun <T, K> equalsBy(first: List<T>, second: List<T>, selector: (T) -> K): Boolean {
        for (i in first.indices) {
            if (selector(first[i]) != selector(second[i])) return false
        }

        return true
    }

    private inline fun <T, K> equalBy(first: T, second: T, selector: (T) -> K): Boolean =
        selector(first) == selector(second)

    context(ExpectActualMatchingContext<*>)
    private val DeclarationSymbolMarker.name: Name
        get() = when (this) {
            is ConstructorSymbolMarker -> SpecialNames.INIT
            is ValueParameterSymbolMarker -> parameterName
            is CallableSymbolMarker -> callableId.callableName
            is RegularClassSymbolMarker -> classId.shortClassName
            is TypeAliasSymbolMarker -> classId.shortClassName
            is TypeParameterSymbolMarker -> parameterName
            else -> error("Unsupported declaration: $this")
        }
}
