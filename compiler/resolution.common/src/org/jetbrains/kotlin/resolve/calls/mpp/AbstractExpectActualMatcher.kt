/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual

/**
 * This object is responsible for matching of expect-actual pairs.
 *
 * - If you want to report the diagnostics then the declarations needs to be checked after they are matched ([AbstractExpectActualChecker]
 *   is responsible for the checking)
 * - In all other cases you only need the "matching" data
 *
 * See `/docs/fir/k2_kmp.md` for details
 */
object AbstractExpectActualMatcher {
    fun getCallablesMatchingCompatibility(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        expectContainingClass: RegularClassSymbolMarker?,
        actualContainingClass: RegularClassSymbolMarker?,
        context: ExpectActualMatchingContext<*>,
    ): ExpectActualMatchingCompatibility = with (context) {
        val expectTypeParameters = expectContainingClass?.typeParameters.orEmpty()
        val actualTypeParameters = actualContainingClass?.typeParameters.orEmpty()
        val parentSubstitutor = (expectTypeParameters zipIfSizesAreEqual actualTypeParameters)
            ?.let { createExpectActualTypeParameterSubstitutor(it, parentSubstitutor = null) }
        getCallablesCompatibility(
            expectDeclaration,
            actualDeclaration,
            parentSubstitutor,
            expectContainingClass,
            actualContainingClass
        )
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
                unfulfilled = null,
            )
        }
    }

    fun matchClassifiers(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassLikeSymbol: ClassLikeSymbolMarker,
        context: ExpectActualMatchingContext<*>,
    ): ExpectActualMatchingCompatibility = with(context) {
        // Can't check FQ names here because nested expected class may be implemented via actual typealias's expansion with the other FQ name
        check(expectClassSymbol.name == actualClassLikeSymbol.name) {
            "This function should be invoked only for declarations with the same name: $expectClassSymbol, $actualClassLikeSymbol"
        }
        check(actualClassLikeSymbol is RegularClassSymbolMarker || actualClassLikeSymbol is TypeAliasSymbolMarker) {
            "Incorrect actual classifier for $expectClassSymbol: $actualClassLikeSymbol"
        }
        ExpectActualMatchingCompatibility.MatchedSuccessfully
    }

    context(ExpectActualMatchingContext<*>)
    private fun matchClassScopes(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
        substitutor: TypeSubstitutorMarker,
    ) {
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
                unfulfilled = null,
            )
        }

        // TODO: check static scope?
    }

    /**
     * Besides returning the matched declaration
     *
     * The function has an additional side effects:
     * - It adds mismatched members to `mismatchedMembers`
     * - It calls `onMatchedMembers` and `onMismatchedMembersFromClassScope` callbacks
     */
    context(ExpectActualMatchingContext<*>)
    internal fun matchSingleExpectAgainstPotentialActuals(
        expectMember: DeclarationSymbolMarker,
        actualMembers: List<DeclarationSymbolMarker>,
        substitutor: TypeSubstitutorMarker?,
        expectClassSymbol: RegularClassSymbolMarker?,
        actualClassSymbol: RegularClassSymbolMarker?,
        unfulfilled: MutableList<Pair<DeclarationSymbolMarker, Map<ExpectActualMatchingCompatibility.Mismatch, List<DeclarationSymbolMarker?>>>>?,
    ): DeclarationSymbolMarker? {
        val mapping = actualMembers.keysToMap { actualMember ->
            when (expectMember) {
                is CallableSymbolMarker -> getCallablesCompatibility(
                    expectMember,
                    actualMember as CallableSymbolMarker,
                    substitutor,
                    expectClassSymbol,
                    actualClassSymbol
                )

                is RegularClassSymbolMarker -> {
                    matchClassifiers(expectMember, actualMember as ClassLikeSymbolMarker, this@ExpectActualMatchingContext)
                }
                else -> error("Unsupported declaration: $expectMember ($actualMembers)")
            }
        }

        val incompatibilityMap = mutableMapOf<ExpectActualMatchingCompatibility.Mismatch, MutableList<DeclarationSymbolMarker>>()
        for ((actualMember, compatibility) in mapping) {
            when (compatibility) {
                ExpectActualMatchingCompatibility.MatchedSuccessfully -> {
                    onMatchedMembers(expectMember, actualMember, expectClassSymbol, actualClassSymbol)
                    return actualMember
                }

                is ExpectActualMatchingCompatibility.Mismatch -> incompatibilityMap.getOrPut(compatibility) { SmartList() }.add(actualMember)
            }
        }

        unfulfilled?.add(expectMember to incompatibilityMap)
        onMismatchedOrIncompatibleMembersFromClassScope(expectMember, incompatibilityMap, expectClassSymbol, actualClassSymbol)
        return null
    }

    context(ExpectActualMatchingContext<*>)
    private fun getCallablesCompatibility(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        parentSubstitutor: TypeSubstitutorMarker?,
        expectContainingClass: RegularClassSymbolMarker?,
        actualContainingClass: RegularClassSymbolMarker?,
    ): ExpectActualMatchingCompatibility {
        require(
            (expectDeclaration is ConstructorSymbolMarker && actualDeclaration is ConstructorSymbolMarker) ||
                    expectDeclaration.callableId.callableName == actualDeclaration.callableId.callableName
        ) {
            "This function should be invoked only for declarations with the same name: $expectDeclaration, $actualDeclaration"
        }
        require((expectDeclaration.dispatchReceiverType == null) == (actualDeclaration.dispatchReceiverType == null)) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $expectDeclaration, $actualDeclaration"
        }

        if (
            enumConstructorsAreAlwaysCompatible &&
            expectContainingClass?.classKind == ClassKind.ENUM_CLASS &&
            actualContainingClass?.classKind == ClassKind.ENUM_CLASS &&
            expectDeclaration is ConstructorSymbolMarker &&
            actualDeclaration is ConstructorSymbolMarker
        ) {
            return ExpectActualMatchingCompatibility.MatchedSuccessfully
        }

        val annotationMode = expectContainingClass?.classKind == ClassKind.ANNOTATION_CLASS
        return getCallablesMatchingIncompatibility(expectDeclaration, actualDeclaration, annotationMode, parentSubstitutor)
            ?: ExpectActualMatchingCompatibility.MatchedSuccessfully
    }

    context(ExpectActualMatchingContext<*>)
    private fun getCallablesMatchingIncompatibility(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        insideAnnotationClass: Boolean,
        parentSubstitutor: TypeSubstitutorMarker?,
    ): ExpectActualMatchingCompatibility.Mismatch? {
        if (expectDeclaration is FunctionSymbolMarker != actualDeclaration is FunctionSymbolMarker) {
            return ExpectActualMatchingCompatibility.CallableKind
        }

        val expectedReceiverType = expectDeclaration.extensionReceiverType
        val actualReceiverType = actualDeclaration.extensionReceiverType
        if ((expectedReceiverType != null) != (actualReceiverType != null)) {
            return ExpectActualMatchingCompatibility.ParameterShape
        }

        val expectedValueParameters = expectDeclaration.valueParameters
        val actualValueParameters = actualDeclaration.valueParameters
        if (!valueParametersCountCompatible(expectDeclaration, actualDeclaration, expectedValueParameters, actualValueParameters)) {
            return ExpectActualMatchingCompatibility.ParameterCount
        }

        val expectedTypeParameters = expectDeclaration.typeParameters
        val actualTypeParameters = actualDeclaration.typeParameters
        if (expectedTypeParameters.size != actualTypeParameters.size) {
            return ExpectActualMatchingCompatibility.FunctionTypeParameterCount
        }

        val substitutor = createExpectActualTypeParameterSubstitutor(
            (expectedTypeParameters zipIfSizesAreEqual actualTypeParameters)
                ?: error("expect/actual type parameters sizes are checked earlier"),
            parentSubstitutor
        )

        if (
            !areCompatibleTypeLists(
                expectedValueParameters.toTypeList(substitutor),
                actualValueParameters.toTypeList(createEmptySubstitutor()),
                insideAnnotationClass
            ) || !areCompatibleExpectActualTypes(
                expectedReceiverType?.let { substitutor.safeSubstitute(it) },
                actualReceiverType,
                parameterOfAnnotationComparisonMode = false
            )
        ) {
            return ExpectActualMatchingCompatibility.ParameterTypes
        }

        if (!areCompatibleTypeParameterUpperBounds(expectedTypeParameters, actualTypeParameters, substitutor)) {
            return ExpectActualMatchingCompatibility.FunctionTypeParameterUpperBounds
        }

        return null
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
        insideAnnotationClass: Boolean,
    ): Boolean {
        for (i in expectedTypes.indices) {
            if (!areCompatibleExpectActualTypes(
                    expectedTypes[i], actualTypes[i], parameterOfAnnotationComparisonMode = insideAnnotationClass
                )
            ) {
                return false
            }
        }
        return true
    }

    context(ExpectActualMatchingContext<*>)
    private fun areCompatibleTypeParameterUpperBounds(
        expectTypeParameterSymbols: List<TypeParameterSymbolMarker>,
        actualTypeParameterSymbols: List<TypeParameterSymbolMarker>,
        substitutor: TypeSubstitutorMarker,
    ): Boolean {
        for (i in expectTypeParameterSymbols.indices) {
            val expectBounds = expectTypeParameterSymbols[i].bounds
            val actualBounds = actualTypeParameterSymbols[i].bounds
            if (
                expectBounds.size != actualBounds.size ||
                !areCompatibleTypeLists(expectBounds.map { substitutor.safeSubstitute(it) }, actualBounds, insideAnnotationClass = false)
            ) {
                return false
            }
        }

        return true
    }

    // ---------------------------------------- Utils ----------------------------------------

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

    context(ExpectActualMatchingContext<*>)
    private fun List<ValueParameterSymbolMarker>.toTypeList(substitutor: TypeSubstitutorMarker): List<KotlinTypeMarker> {
        return this.map { substitutor.safeSubstitute(it.returnType) }
    }
}
