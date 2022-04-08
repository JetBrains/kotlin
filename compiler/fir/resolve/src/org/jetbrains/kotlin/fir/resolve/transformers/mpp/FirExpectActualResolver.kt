/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Modality.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.enumMapOf
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

object FirExpectActualResolver {
    @OptIn(ExperimentalStdlibApi::class)
    fun findExpectForActual(
        actualSymbol: FirBasedSymbol<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): ExpectForActualData? {
        return when (actualSymbol) {
            is FirCallableSymbol<*> -> {
                val callableId = actualSymbol.callableId
                val classId = callableId.classId
                var parentSubstitutor: ConeSubstitutor? = null
                var expectContainingClass: FirRegularClassSymbol? = null
                var actualContainingClass: FirRegularClassSymbol? = null
                val candidates = when {
                    classId != null -> {
                        expectContainingClass = useSiteSession.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)?.let {
                            it.fullyExpandedClass(it.moduleData.session)
                        }
                        actualContainingClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
                            ?.fullyExpandedClass(useSiteSession)

                        val expectTypeParameters = expectContainingClass?.typeParameterSymbols.orEmpty()
                        val actualTypeParameters = actualContainingClass
                            ?.typeParameterSymbols
                            .orEmpty()
                        parentSubstitutor = createTypeParameterSubstitutor(expectTypeParameters, actualTypeParameters, useSiteSession)
                        when (actualSymbol) {
                            is FirConstructorSymbol -> expectContainingClass?.getConstructors(scopeSession)
                            else -> expectContainingClass?.getMembers(callableId.callableName, scopeSession)
                        }.orEmpty()
                    }
                    callableId.isLocal -> return null
                    else -> {
                        val scope = FirPackageMemberScope(callableId.packageName, useSiteSession, useSiteSession.dependenciesSymbolProvider)
                        mutableListOf<FirCallableSymbol<*>>().apply {
                            scope.processFunctionsByName(callableId.callableName) { add(it) }
                            scope.processPropertiesByName(callableId.callableName) { add(it) }
                        }
                    }
                }
                candidates.filter { expectSymbol ->
                    actualSymbol != expectSymbol && expectSymbol.isExpect
                }.groupBy { expectDeclaration ->
                    areCompatibleCallables(
                        expectDeclaration,
                        actualSymbol,
                        useSiteSession,
                        parentSubstitutor,
                        expectContainingClass,
                        actualContainingClass
                    )
                }
            }
            is FirClassLikeSymbol<*> -> {
                val expectClassSymbol = useSiteSession.dependenciesSymbolProvider
                    .getClassLikeSymbolByClassId(actualSymbol.classId) as? FirRegularClassSymbol ?: return null
                val compatibility = areCompatibleClassifiers(expectClassSymbol, actualSymbol, useSiteSession, scopeSession)
                mapOf(compatibility to listOf(expectClassSymbol))
            }
            else -> null
        }
    }

    private fun areCompatibleClassifiers(
        expectClassSymbol: FirRegularClassSymbol,
        actualClassLikeSymbol: FirClassLikeSymbol<*>,
        actualSession: FirSession,
        scopeSession: ScopeSession
    ): ExpectActualCompatibility<FirBasedSymbol<*>> {
        // Can't check FQ names here because nested expected class may be implemented via actual typealias's expansion with the other FQ name
        assert(expectClassSymbol.classId.shortClassName == actualClassLikeSymbol.classId.shortClassName) {
            "This function should be invoked only for declarations with the same name: $expectClassSymbol, $actualClassLikeSymbol"
        }

        val actualClass = when (actualClassLikeSymbol) {
            is FirRegularClassSymbol -> actualClassLikeSymbol
            is FirTypeAliasSymbol -> actualClassLikeSymbol.resolvedExpandedTypeRef.coneType.fullyExpandedType(actualSession)
                .toSymbol(actualSession) as? FirRegularClassSymbol
                ?: return ExpectActualCompatibility.Compatible // do not report extra error on erroneous typealias
            else -> throw IllegalArgumentException("Incorrect actual classifier for $expectClassSymbol: $actualClassLikeSymbol")
        }

        if (expectClassSymbol.classKind != actualClass.classKind) return ExpectActualCompatibility.Incompatible.ClassKind

        if (!equalBy(expectClassSymbol, actualClass) { listOf(it.isCompanion, it.isInner, it.isInline /*|| it.isValue*/) }) {
            return ExpectActualCompatibility.Incompatible.ClassModifiers
        }

        val expectTypeParameterSymbols = expectClassSymbol.typeParameterSymbols
        val actualTypeParameterSymbols = actualClass.typeParameterSymbols
        if (expectTypeParameterSymbols.size != actualTypeParameterSymbols.size) {
            return ExpectActualCompatibility.Incompatible.TypeParameterCount
        }

        if (!areCompatibleModalities(expectClassSymbol.modality, actualClass.modality)) {
            return ExpectActualCompatibility.Incompatible.Modality
        }

        if (expectClassSymbol.visibility != actualClass.visibility) {
            return ExpectActualCompatibility.Incompatible.Visibility
        }

        val substitutor = createTypeParameterSubstitutor(expectTypeParameterSymbols, actualTypeParameterSymbols, actualSession)

        val expectSession = expectClassSymbol.moduleData.session
        areCompatibleTypeParameters(expectTypeParameterSymbols, actualTypeParameterSymbols, actualSession, expectSession, substitutor).let {
            if (it != ExpectActualCompatibility.Compatible) {
                return it
            }
        }

        // Subtract kotlin.Any from supertypes because it's implicitly added if no explicit supertype is specified,
        // and not added if an explicit supertype _is_ specified
        val expectSupertypes = expectClassSymbol.superConeTypes.filterNot { it.classId == actualSession.builtinTypes.anyType.id }
        val actualSupertypes = actualClass.superConeTypes.filterNot { it.classId == actualSession.builtinTypes.anyType.id }
        if (
            expectSupertypes.map(substitutor::substituteOrSelf).any { expectSupertype ->
                actualSupertypes.none { actualSupertype ->
                    areCompatibleTypes(expectSupertype, actualSupertype, expectSession, actualSession)
                }
            }
        ) {
            return ExpectActualCompatibility.Incompatible.Supertypes
        }

        areCompatibleClassScopes(expectClassSymbol, actualClass, actualSession, scopeSession, substitutor).let {
            if (it != ExpectActualCompatibility.Compatible) {
                return it
            }
        }

        return ExpectActualCompatibility.Compatible
    }

    private fun areCompatibleClassScopes(
        expectClassSymbol: FirRegularClassSymbol,
        actualClassSymbol: FirRegularClassSymbol,
        actualSession: FirSession,
        scopeSession: ScopeSession,
        substitutor: ConeSubstitutor
    ): ExpectActualCompatibility<FirBasedSymbol<*>> {
        val unfulfilled =
            mutableListOf<Pair<FirBasedSymbol<*>, Map<ExpectActualCompatibility.Incompatible<FirBasedSymbol<*>>, MutableCollection<FirBasedSymbol<*>>>>>()

        val allActualMembers = actualClassSymbol.getMembers(scopeSession, actualSession)
        val actualMembersByName = allActualMembers.groupBy { it.name }
        val actualConstructors = allActualMembers.filterIsInstance<FirConstructorSymbol>()

        outer@ for (expectMember in expectClassSymbol.getMembers(scopeSession)) {
            // if (expectMember is CallableMemberDescriptor && !expectMember.kind.isReal) continue

            val actualMembers = when (expectMember) {
                is FirConstructorSymbol -> actualConstructors
                else -> actualMembersByName[expectMember.name]?.filter { actualMember ->
                    expectMember is FirRegularClassSymbol && actualMember is FirRegularClassSymbol ||
                            expectMember is FirCallableSymbol<*> && actualMember is FirCallableSymbol<*>
                }.orEmpty()
            }

            val mapping = actualMembers.keysToMap { actualMember ->
                when (expectMember) {
                    is FirCallableSymbol<*> ->
                        areCompatibleCallables(
                            expectMember,
                            actualMember as FirCallableSymbol<*>,
                            actualSession,
                            substitutor,
                            expectClassSymbol,
                            actualClassSymbol
                        )
                    is FirRegularClassSymbol ->
                        areCompatibleClassifiers(expectMember, actualMember as FirRegularClassSymbol, actualSession, scopeSession)
                    else -> throw UnsupportedOperationException("Unsupported declaration: $expectMember ($actualMembers)")
                }
            }
            if (mapping.values.any { it == ExpectActualCompatibility.Compatible }) continue

            val incompatibilityMap =
                mutableMapOf<ExpectActualCompatibility.Incompatible<FirBasedSymbol<*>>, MutableCollection<FirBasedSymbol<*>>>()
            for ((declaration, compatibility) in mapping) {
                when (compatibility) {
                    ExpectActualCompatibility.Compatible -> continue@outer
                    is ExpectActualCompatibility.Incompatible -> incompatibilityMap.getOrPut(compatibility) { SmartList() }.add(declaration)
                }
            }

            unfulfilled.add(expectMember to incompatibilityMap)
        }

        if (expectClassSymbol.classKind == ClassKind.ENUM_CLASS) {
            val expectEntries = expectClassSymbol.fir.collectEnumEntries().map { it.name }
            val actualEntries = actualClassSymbol.fir.collectEnumEntries().map { it.name }

            if (!actualEntries.containsAll(expectEntries)) {
                return ExpectActualCompatibility.Incompatible.EnumEntries
            }
        }

        // TODO: check static scope?

        if (unfulfilled.isEmpty()) return ExpectActualCompatibility.Compatible

        return ExpectActualCompatibility.Incompatible.ClassScopes(unfulfilled)
    }

    private fun areCompatibleCallables(
        expectDeclaration: FirCallableSymbol<*>,
        actualDeclaration: FirCallableSymbol<*>,
        actualSession: FirSession,
        parentSubstitutor: ConeSubstitutor?,
        expectContainingClass: FirRegularClassSymbol?,
        actualContainingClass: FirRegularClassSymbol?,
    ): ExpectActualCompatibility<FirBasedSymbol<*>> {
        assert(
            (expectDeclaration is FirConstructorSymbol && actualDeclaration is FirConstructorSymbol) ||
                    expectDeclaration.callableId.callableName == actualDeclaration.callableId.callableName
        ) {
            "This function should be invoked only for declarations with the same name: $expectDeclaration, $actualDeclaration"
        }
        assert((expectDeclaration.dispatchReceiverType == null) == (actualDeclaration.dispatchReceiverType == null)) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $expectDeclaration, $actualDeclaration"
        }

        val expectSession = expectDeclaration.moduleData.session

        if (
            expectDeclaration is FirConstructorSymbol &&
            actualDeclaration is FirConstructorSymbol &&
            expectContainingClass?.classKind == ClassKind.ENUM_CLASS &&
            actualContainingClass?.classKind == ClassKind.ENUM_CLASS
        ) {
            return ExpectActualCompatibility.Compatible
        }

        if (expectDeclaration is FirNamedFunctionSymbol != actualDeclaration is FirNamedFunctionSymbol) {
            return ExpectActualCompatibility.Incompatible.CallableKind
        }

        val expectedReceiverType = expectDeclaration.resolvedReceiverTypeRef
        val actualReceiverType = actualDeclaration.resolvedReceiverTypeRef
        if ((expectedReceiverType != null) != (actualReceiverType != null)) {
            return ExpectActualCompatibility.Incompatible.ParameterShape
        }

        val expectedValueParameters = expectDeclaration.valueParameterSymbols
        val actualValueParameters = actualDeclaration.valueParameterSymbols
        if (!valueParametersCountCompatible(expectDeclaration, actualDeclaration, expectedValueParameters, actualValueParameters)) {
            return ExpectActualCompatibility.Incompatible.ParameterCount
        }

        val expectedTypeParameters = expectDeclaration.typeParameterSymbols
        val actualTypeParameters = actualDeclaration.typeParameterSymbols
        if (expectedTypeParameters.size != actualTypeParameters.size) {
            return ExpectActualCompatibility.Incompatible.TypeParameterCount
        }

        val substitutor = createTypeParameterSubstitutor(expectedTypeParameters, actualTypeParameters, actualSession, parentSubstitutor)

        if (
            !areCompatibleTypeLists(
                expectedValueParameters.toTypeList(substitutor),
                actualValueParameters.toTypeList(ConeSubstitutor.Empty),
                expectSession,
                actualSession
            ) ||
            !areCompatibleTypes(
                expectedReceiverType?.coneType?.let { substitutor.substituteOrSelf(it) },
                actualReceiverType?.coneType,
                expectSession,
                actualSession
            )
        ) {
            return ExpectActualCompatibility.Incompatible.ParameterTypes
        }
        if (
            !areCompatibleTypes(
                substitutor.substituteOrSelf(expectDeclaration.resolvedReturnTypeRef.coneType),
                actualDeclaration.resolvedReturnTypeRef.coneType,
                expectSession,
                actualSession
            )
        ) {
            return ExpectActualCompatibility.Incompatible.ReturnType
        }

        // TODO: implement hasStableParameterNames calculation
        // if (actualDeclaration.hasStableParameterNames() && !equalsBy(expectedValueParameters, actualValueParameters, ValueParameterDescriptor::getName)) return Incompatible.ParameterNames

        if (!equalsBy(expectedTypeParameters, actualTypeParameters) { it.name }) {
            return ExpectActualCompatibility.Incompatible.TypeParameterNames
        }

        if (
            !areCompatibleModalities(
                expectDeclaration.modality,
                actualDeclaration.modality,
                expectContainingClass?.modality,
                actualContainingClass?.modality
            )
        ) {
            if (expectDeclaration.dispatchReceiverType?.isAny != true) {
                return ExpectActualCompatibility.Incompatible.Modality
            }
        }

        if (!areDeclarationsWithCompatibleVisibilities(expectDeclaration.resolvedStatus, actualDeclaration.resolvedStatus)) {
            return ExpectActualCompatibility.Incompatible.Visibility
        }

        areCompatibleTypeParameters(expectedTypeParameters, actualTypeParameters, actualSession, expectSession, substitutor).let {
            if (it != ExpectActualCompatibility.Compatible) {
                return it
            }
        }

        if (!equalsBy(expectedValueParameters, actualValueParameters) { it.isVararg }) {
            return ExpectActualCompatibility.Incompatible.ValueParameterVararg
        }

        // Adding noinline/crossinline to parameters is disallowed, except if the expected declaration was not inline at all
        if (expectDeclaration is FirNamedFunctionSymbol && expectDeclaration.isInline) {
            if (expectedValueParameters.indices.any { i -> !expectedValueParameters[i].isNoinline && actualValueParameters[i].isNoinline }) {
                return ExpectActualCompatibility.Incompatible.ValueParameterNoinline
            }
            if (expectedValueParameters.indices.any { i -> !expectedValueParameters[i].isCrossinline && actualValueParameters[i].isCrossinline }) {
                return ExpectActualCompatibility.Incompatible.ValueParameterCrossinline
            }
        }

        when {
            expectDeclaration is FirNamedFunctionSymbol && actualDeclaration is FirNamedFunctionSymbol -> areCompatibleFunctions(
                expectDeclaration,
                actualDeclaration
            ).let { if (it != ExpectActualCompatibility.Compatible) return it }

            expectDeclaration is FirConstructorSymbol && actualDeclaration is FirConstructorSymbol -> areCompatibleFunctions(
                expectDeclaration,
                actualDeclaration
            ).let { if (it != ExpectActualCompatibility.Compatible) return it }

            expectDeclaration is FirPropertySymbol && actualDeclaration is FirPropertySymbol -> areCompatibleProperties(
                expectDeclaration,
                actualDeclaration
            ).let { if (it != ExpectActualCompatibility.Compatible) return it }

            else -> throw AssertionError("Unsupported declarations: $expectDeclaration, $actualDeclaration")
        }

        return ExpectActualCompatibility.Compatible
    }

    private fun createTypeParameterSubstitutor(
        expectedTypeParameters: List<FirTypeParameterSymbol>,
        actualTypeParameters: List<FirTypeParameterSymbol>,
        useSiteSession: FirSession,
        parentSubstitutor: ConeSubstitutor? = null
    ): ConeSubstitutor {
        val substitution = expectedTypeParameters.zip(actualTypeParameters).associate { (expectedParameterSymbol, actualParameterSymbol) ->
            expectedParameterSymbol to actualParameterSymbol.toLookupTag().constructType(emptyArray(), isNullable = false)
        }
        val substitutor = ConeSubstitutorByMap(
            substitution,
            useSiteSession
        )
        if (parentSubstitutor == null) {
            return substitutor
        }
        return substitutor.chain(parentSubstitutor)
    }

    private fun valueParametersCountCompatible(
        expectDeclaration: FirCallableSymbol<*>,
        actualDeclaration: FirCallableSymbol<*>,
        expectValueParameters: List<FirValueParameterSymbol>,
        actualValueParameters: List<FirValueParameterSymbol>
    ): Boolean {
        if (expectValueParameters.size == actualValueParameters.size) return true

        return if (
            expectDeclaration.isAnnotationConstructor(expectDeclaration.moduleData.session) &&
            actualDeclaration.isAnnotationConstructor(actualDeclaration.moduleData.session)
        ) {
            expectValueParameters.isEmpty() && actualValueParameters.all { it.hasDefaultValue }
        } else {
            false
        }
    }

    private fun areCompatibleTypeLists(
        expectedTypes: List<ConeKotlinType?>,
        actualTypes: List<ConeKotlinType?>,
        expectSession: FirSession,
        actualSession: FirSession
    ): Boolean {
        for (i in expectedTypes.indices) {
            if (!areCompatibleTypes(expectedTypes[i], actualTypes[i], expectSession, actualSession)) {
                return false
            }
        }
        return true
    }

    private fun areCompatibleTypes(
        expectedType: ConeKotlinType?,
        actualType: ConeKotlinType?,
        expectSession: FirSession,
        actualSession: FirSession
    ): Boolean {
        if (expectedType == null) return actualType == null
        if (actualType == null) return false

        val typeCheckerContext = ConeInferenceContextForExpectActual(expectSession, actualSession).newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = true
        )
        return AbstractTypeChecker.equalTypes(
            typeCheckerContext,
            expectedType,
            actualType
        )
    }

    private fun areCompatibleModalities(
        expectModality: Modality?,
        actualModality: Modality?,
        expectContainingClassModality: Modality? = null,
        actualContainingClassModality: Modality? = null
    ): Boolean {
        val expectEffectiveModality = effectiveModality(expectModality, expectContainingClassModality)
        val actualEffectiveModality = effectiveModality(actualModality, actualContainingClassModality)

        val result = actualEffectiveModality in compatibleModalityMap.getValue(expectEffectiveModality)
        return result
    }

    /*
     * If containing class is final then all declarations in it effectively final
     */
    private fun effectiveModality(declarationModality: Modality?, containingClassModality: Modality?): Modality? {
        return when (containingClassModality) {
            FINAL -> FINAL
            else -> declarationModality
        }
    }

    /*
     * Key is expect modality, value is a set of compatible actual modalities
     */
    private val compatibleModalityMap: EnumMap<Modality, EnumSet<Modality>> = enumMapOf(
        ABSTRACT to enumSetOf(ABSTRACT, OPEN),
        OPEN to enumSetOf(OPEN),
        FINAL to enumSetOf(ABSTRACT, OPEN, FINAL),
        SEALED to enumSetOf(SEALED),
    )

    private fun areDeclarationsWithCompatibleVisibilities(
        expectStatus: FirDeclarationStatus,
        actualStatus: FirDeclarationStatus
    ): Boolean {
        val compare = Visibilities.compare(expectStatus.visibility, actualStatus.visibility)
        return if (expectStatus.modality != FINAL) {
            // For overridable declarations visibility should match precisely, see KT-19664
            compare == 0
        } else {
            // For non-overridable declarations actuals are allowed to have more permissive visibility
            compare != null && compare <= 0
        }
    }

    private fun areCompatibleTypeParameters(
        expectTypeParameterSymbols: List<FirTypeParameterSymbol>,
        actualTypeParameterSymbols: List<FirTypeParameterSymbol>,
        actualSession: FirSession,
        expectSession: FirSession,
        substitutor: ConeSubstitutor
    ): ExpectActualCompatibility<FirBasedSymbol<*>> {
        for (i in expectTypeParameterSymbols.indices) {
            val expectBounds = expectTypeParameterSymbols[i].resolvedBounds.map { it.coneType }
            val actualBounds = actualTypeParameterSymbols[i].resolvedBounds.map { it.coneType }
            if (
                expectBounds.size != actualBounds.size ||
                !areCompatibleTypeLists(expectBounds.map(substitutor::substituteOrSelf), actualBounds, expectSession, actualSession)
            ) {
                return ExpectActualCompatibility.Incompatible.TypeParameterUpperBounds
            }
        }

        if (!equalsBy(expectTypeParameterSymbols, actualTypeParameterSymbols) { it.variance }) {
            return ExpectActualCompatibility.Incompatible.TypeParameterVariance
        }

        // Removing "reified" from an expected function's type parameter is fine
        if (
            expectTypeParameterSymbols.indices.any { i ->
                !expectTypeParameterSymbols[i].isReified && actualTypeParameterSymbols[i].isReified
            }
        ) {
            return ExpectActualCompatibility.Incompatible.TypeParameterReified
        }

        return ExpectActualCompatibility.Compatible
    }

    private fun areCompatibleFunctions(
        expectFunction: FirCallableSymbol<*>,
        actualFunction: FirCallableSymbol<*>
    ): ExpectActualCompatibility<FirBasedSymbol<*>> {
        if (!equalBy(expectFunction, actualFunction) { f -> f.isSuspend }) {
            return ExpectActualCompatibility.Incompatible.FunctionModifiersDifferent
        }

        if (
            expectFunction.isExternal && !actualFunction.isExternal ||
            expectFunction.isInfix && !actualFunction.isInfix ||
            expectFunction.isInline && !actualFunction.isInline ||
            expectFunction.isOperator && !actualFunction.isOperator ||
            expectFunction.isTailRec && !actualFunction.isTailRec
        ) {
            return ExpectActualCompatibility.Incompatible.FunctionModifiersNotSubset
        }

        return ExpectActualCompatibility.Compatible
    }

    private fun areCompatibleProperties(
        expected: FirPropertySymbol,
        actual: FirPropertySymbol,
    ): ExpectActualCompatibility<FirBasedSymbol<*>> {
        return when {
            !equalBy(expected, actual) { p -> p.isVar } -> ExpectActualCompatibility.Incompatible.PropertyKind
            !equalBy(expected, actual) { p -> p.isLateInit } -> ExpectActualCompatibility.Incompatible.PropertyLateinitModifier
            expected.isConst && !actual.isConst -> ExpectActualCompatibility.Incompatible.PropertyConstModifier
            else -> ExpectActualCompatibility.Compatible
        }
    }

    // ---------------------------------------- Utils ----------------------------------------

    private class ConeInferenceContextForExpectActual(val expectSession: FirSession, val actualSession: FirSession) : ConeInferenceContext {
        override val session: FirSession
            get() = actualSession

        override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
            if (c1 !is ConeClassifierLookupTag || c2 !is ConeClassifierLookupTag) {
                return c1 == c2
            }
            return isExpectedClassAndActualTypeAlias(c1, c2) ||
                    isExpectedClassAndActualTypeAlias(c2, c1) ||
                    c1 == c2
        }

        // For example, expectedTypeConstructor may be the expected class kotlin.text.StringBuilder, while actualTypeConstructor
        // is java.lang.StringBuilder. For the purposes of type compatibility checking, we must consider these types equal here.
        // Note that the case of an "actual class" works as expected though, because the actual class by definition has the same FQ name
        // as the corresponding expected class, so their type constructors are equal as per AbstractClassTypeConstructor#equals
        private fun isExpectedClassAndActualTypeAlias(
            expectLookupTag: ConeClassifierLookupTag,
            actualLookupTag: ConeClassifierLookupTag
        ): Boolean {
            val expectDeclaration = expectLookupTag.toClassLikeDeclaration(expectSession) ?: return false
            val actualDeclaration = actualLookupTag.toClassLikeDeclaration(actualSession) ?: return false

            if (!expectDeclaration.isExpect) return false
            val expectClassId = when (expectDeclaration) {
                is FirRegularClassSymbol -> expectDeclaration.classId
                is FirTypeAliasSymbol -> expectDeclaration.resolvedExpandedTypeRef.coneType.classId
                else -> null
            } ?: return false
            return expectClassId == actualDeclaration.classId
        }

        private fun ConeClassifierLookupTag.toClassLikeDeclaration(session: FirSession): FirClassLikeSymbol<*>? {
            return this.toSymbol(session) as? FirClassLikeSymbol<*>
        }
    }

    private fun List<FirValueParameterSymbol>.toTypeList(substitutor: ConeSubstitutor): List<ConeKotlinType> {
        return this.map { substitutor.substituteOrSelf(it.resolvedReturnTypeRef.coneType) }
    }

    private val FirCallableSymbol<*>.valueParameterSymbols: List<FirValueParameterSymbol>
        get() = (this as? FirFunctionSymbol<*>)?.valueParameterSymbols ?: emptyList()

    private inline fun <T, K> equalsBy(first: List<T>, second: List<T>, selector: (T) -> K): Boolean {
        for (i in first.indices) {
            if (selector(first[i]) != selector(second[i])) return false
        }

        return true
    }

    private inline fun <T, K> equalBy(first: T, second: T, selector: (T) -> K): Boolean =
        selector(first) == selector(second)

    private fun FirClassSymbol<*>.getMembers(
        scopeSession: ScopeSession,
        session: FirSession = moduleData.session
    ): Collection<FirBasedSymbol<*>> {
        val scope = defaultType().scope(useSiteSession = session, scopeSession, FakeOverrideTypeCalculator.DoNothing)
            ?: return emptyList()
        return mutableListOf<FirBasedSymbol<*>>().apply {
            for (name in scope.getCallableNames()) {
                scope.getMembersTo(this, name)
            }
            for (name in declarationSymbols.mapNotNull { (it as? FirRegularClassSymbol)?.classId?.shortClassName }) {
                addIfNotNull(scope.getSingleClassifier(name) as? FirRegularClassSymbol)
            }
            getConstructorsTo(this, scope)
        }
    }

    private fun FirClassSymbol<*>.getConstructors(
        scopeSession: ScopeSession,
        session: FirSession = moduleData.session
    ): Collection<FirConstructorSymbol> {
        return mutableListOf<FirConstructorSymbol>().apply {
            getConstructorsTo(this, unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false))
        }
    }

    private fun getConstructorsTo(destination: MutableList<in FirConstructorSymbol>, scope: FirTypeScope) {
        scope.getDeclaredConstructors().mapTo(destination) { it }
    }

    private fun FirClassSymbol<*>.getMembers(name: Name, scopeSession: ScopeSession): Collection<FirCallableSymbol<*>> {
        val scope = defaultType().scope(useSiteSession = moduleData.session, scopeSession, FakeOverrideTypeCalculator.DoNothing)
            ?: return emptyList()
        return mutableListOf<FirCallableSymbol<*>>().apply {
            scope.getMembersTo(this, name)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirTypeScope.getMembersTo(
        destination: MutableList<in FirCallableSymbol<*>>,
        name: Name,
    ) {
        processFunctionsByName(name) { destination.add(it) }
        processPropertiesByName(name) { destination.add(it) }
    }

    private val FirBasedSymbol<*>.name: Name
        get() = when (this) {
            is FirCallableSymbol<*> -> name
            is FirRegularClassSymbol -> classId.shortClassName
            else -> error("Should not be here")
        }
}
