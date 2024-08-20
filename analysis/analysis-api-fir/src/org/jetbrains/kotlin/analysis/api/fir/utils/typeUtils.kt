/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.captureArguments
import org.jetbrains.kotlin.fir.types.create
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.util.bfs

/**
 * Returns whether [subclass] is a strict subtype of [superclass]. Resolves [subclass] to [FirResolvePhase.SUPER_TYPES].
 */
@KaImplementationDetail
fun isSubclassOf(
    subclass: FirClass,
    superclass: FirClass,
    useSiteSession: FirSession,
    allowIndirectSubtyping: Boolean = true,
): Boolean {
    subclass.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

    if (subclass.superConeTypes.any { it.toRegularClassSymbol(useSiteSession) == superclass.symbol }) return true
    if (!allowIndirectSubtyping) return false

    subclass.superConeTypes.forEach { superType ->
        val superOfSub = superType.toRegularClassSymbol(useSiteSession) ?: return@forEach
        if (isSubclassOf(superOfSub.fir, superclass, useSiteSession, allowIndirectSubtyping = true)) return true
    }
    return false
}

/**
 * @see org.jetbrains.kotlin.analysis.api.types.KaType.abbreviation
 */
internal fun KaSymbolByFirBuilder.buildAbbreviatedType(coneType: ConeClassLikeType): KaUsualClassType? {
    return coneType.abbreviatedType?.let { abbreviatedConeType ->
        // If the resulting type is an error type, the abbreviated type couldn't be resolved. As per the contract of
        // `KaType.abbreviatedType`, we should return `null` in such cases. The user can then fall back to the expanded type.
        typeBuilder.buildKtType(abbreviatedConeType) as? KaUsualClassType
    }
}

internal fun ProjectionKind.toVariance(): Variance = when (this) {
    ProjectionKind.OUT -> Variance.OUT_VARIANCE
    ProjectionKind.IN -> Variance.IN_VARIANCE
    ProjectionKind.INVARIANT -> Variance.INVARIANT
    ProjectionKind.STAR -> error("KtStarProjectionTypeArgument should not be directly created")
}

internal enum class ConeSupertypeCalculationMode {
    /**
     * Type arguments of supertypes will be kept as-is, e.g. `Foo<A>` in `class Bar<A> : Foo<A>` will remain the same.
     */
    NO_SUBSTITUTION,

    /**
     * The type arguments of supertypes will be substituted with the actual type arguments of the original subtype.
     */
    SUBSTITUTE,

    /**
     * In addition to [SUBSTITUTE], the resulting supertypes will also be approximated.
     */
    SUBSTITUTE_AND_APPROXIMATE;

    companion object {
        fun substitution(shouldApproximate: Boolean): ConeSupertypeCalculationMode = when {
            shouldApproximate -> SUBSTITUTE_AND_APPROXIMATE
            else -> SUBSTITUTE
        }
    }
}

/**
 * Returns the strict supertypes of this [ConeKotlinType], i.e. all its supertypes except itself.
 */
internal fun ConeKotlinType.getAllStrictSupertypes(
    session: FirSession,
    calculationMode: ConeSupertypeCalculationMode,
): Sequence<ConeKotlinType> =
    listOf(this)
        .bfs { it.getDirectSupertypes(session, calculationMode).iterator() }
        .drop(1)

internal fun ConeKotlinType.getDirectSupertypes(
    session: FirSession,
    calculationMode: ConeSupertypeCalculationMode,
): Sequence<ConeKotlinType> {
    return when (this) {
        // We also need to collect those on `upperBound` due to nullability.
        is ConeFlexibleType ->
            lowerBound.getDirectSupertypes(session, calculationMode) + upperBound.getDirectSupertypes(session, calculationMode)
        is ConeDefinitelyNotNullType -> original.getDirectSupertypes(session, calculationMode).map {
            ConeDefinitelyNotNullType.create(it, session.typeContext) ?: it
        }
        is ConeIntersectionType -> intersectedTypes.asSequence().flatMap { it.getDirectSupertypes(session, calculationMode) }
        is ConeErrorType -> emptySequence()
        is ConeLookupTagBasedType -> calculateSupertypes(session, calculationMode)
        else -> emptySequence()
    }.distinct()
}

private fun ConeLookupTagBasedType.calculateSupertypes(
    session: FirSession,
    calculationMode: ConeSupertypeCalculationMode,
): Sequence<ConeKotlinType> {
    val symbol = lookupTag.toSymbol(session) ?: return emptySequence()
    val superTypes = symbol.getUnsubstitutedSupertypes(session)

    if (superTypes.isEmpty()) {
        return emptySequence()
    }

    if (calculationMode == ConeSupertypeCalculationMode.NO_SUBSTITUTION) {
        return superTypes.asSequence()
    }

    return substituteSuperTypes(symbol, superTypes, session, calculationMode)
}

private fun FirClassifierSymbol<*>.getUnsubstitutedSupertypes(session: FirSession): List<ConeKotlinType> {
    return when (this) {
        is FirAnonymousObjectSymbol -> resolvedSuperTypes
        is FirRegularClassSymbol -> resolvedSuperTypes
        is FirTypeAliasSymbol -> fullyExpandedClass(session)?.resolvedSuperTypes ?: emptyList()
        is FirTypeParameterSymbol -> resolvedBounds.map { it.coneType }
        else -> emptyList()
    }
}

private fun ConeLookupTagBasedType.substituteSuperTypes(
    symbol: FirClassifierSymbol<*>,
    superTypes: List<ConeKotlinType>,
    session: FirSession,
    calculationMode: ConeSupertypeCalculationMode,
): Sequence<ConeKotlinType> {
    val typeParameterSymbols = symbol.typeParameterSymbols ?: return superTypes.asSequence()
    val argumentTypes = (session.typeContext.captureArguments(this, CaptureStatus.FROM_EXPRESSION)?.toList()
        ?: this.typeArguments.mapNotNull { it.type })

    if (typeParameterSymbols.size != argumentTypes.size) {
        // Should not happen in valid code
        return emptySequence()
    }

    val substitutor = substitutorByMap(typeParameterSymbols.zip(argumentTypes).toMap(), session)
    return superTypes.asSequence().map {
        val type = substitutor.substituteOrSelf(it)
        if (calculationMode == ConeSupertypeCalculationMode.SUBSTITUTE_AND_APPROXIMATE) {
            session.typeApproximator.approximateToSuperType(
                type,
                TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            ) ?: type
        } else {
            type
        }.withNullability(nullability, session.typeContext)
    }
}

internal fun <C : ConeKotlinType, T : KaType> createTypePointer(
    coneType: C,
    builder: KaSymbolByFirBuilder,
    typeFactory: (C, KaSymbolByFirBuilder) -> T?,
): KaTypePointer<T> {
    val coneTypePointer = coneType.createPointer(builder)
    return KaGenericTypePointer(coneTypePointer, typeFactory)
}

private class KaGenericTypePointer<C : ConeKotlinType, T : KaType>(
    private val coneTypePointer: ConeTypePointer<C>,
    private val typeFactory: (C, KaSymbolByFirBuilder) -> T?
) : KaTypePointer<T> {
    override fun restore(session: KaSession): T? {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        return typeFactory(coneType, session.firSymbolBuilder)
    }
}
