/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.utils.WeakPair
import org.jetbrains.kotlin.fir.utils.component1
import org.jetbrains.kotlin.fir.utils.component2

fun ConeClassLikeType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType,
): ConeClassLikeType {
    if (this is ConeClassLikeTypeImpl) {
        val (cachedSession, cachedExpandedType) = cachedExpandedType
        if (cachedSession === useSiteSession && cachedExpandedType != null) {
            return cachedExpandedType
        }

        val computedExpandedType = fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
        this.cachedExpandedType = WeakPair(useSiteSession, computedExpandedType)
        return computedExpandedType
    }

    return fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
}

fun ConeKotlinType.fullyExpandedType(
    useSiteSession: FirSession
): ConeKotlinType = when (this) {
    is ConeFlexibleType ->
        ConeFlexibleType(lowerBound.fullyExpandedType(useSiteSession), upperBound.fullyExpandedType(useSiteSession))
    is ConeClassLikeType -> fullyExpandedType(useSiteSession)
    else -> this
}

private fun ConeClassLikeType.fullyExpandedTypeNoCache(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType?,
): ConeClassLikeType {
    val directExpansionType = directExpansionType(useSiteSession, expandedConeType) ?: return this
    return directExpansionType.fullyExpandedType(useSiteSession, expandedConeType)
}

fun ConeClassLikeType.directExpansionType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType,
): ConeClassLikeType? {
    val typeAliasSymbol = lookupTag.toSymbol(useSiteSession) as? FirTypeAliasSymbol ?: return null
    val typeAlias = typeAliasSymbol.fir

    val resultType = expandedConeType(typeAlias)?.applyNullabilityFrom(useSiteSession, this) ?: return null

    if (resultType.typeArguments.isEmpty()) return resultType
    return mapTypeAliasArguments(typeAlias, this, resultType, useSiteSession) as? ConeClassLikeType
}

private fun ConeClassLikeType.applyNullabilityFrom(
    session: FirSession,
    abbreviation: ConeClassLikeType
): ConeClassLikeType {
    if (abbreviation.isMarkedNullable) return withNullability(ConeNullability.NULLABLE, session.typeContext)
    return this
}

private fun mapTypeAliasArguments(
    typeAlias: FirTypeAlias,
    abbreviatedType: ConeClassLikeType,
    resultingType: ConeClassLikeType,
    useSiteSession: FirSession,
): ConeKotlinType {
    if (typeAlias.typeParameters.isNotEmpty() && abbreviatedType.typeArguments.isEmpty()) {
        return resultingType.lookupTag.constructClassType(emptyArray(), resultingType.isNullable)
    }
    val typeAliasMap = typeAlias.typeParameters.map { it.symbol }.zip(abbreviatedType.typeArguments).toMap()

    val substitutor = object : AbstractConeSubstitutor() {
        override val typeInferenceContext: ConeInferenceContext
            get() = useSiteSession.inferenceComponents.ctx

        override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
            return null
        }

        override fun substituteArgument(projection: ConeTypeProjection): ConeTypeProjection? {
            val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
            val symbol = (type as? ConeTypeParameterType)?.lookupTag?.symbol ?: return super.substituteArgument(projection)
            val mappedProjection = typeAliasMap[symbol] ?: return super.substituteArgument(projection)
            val mappedType = (mappedProjection as? ConeKotlinTypeProjection)?.type.updateNullabilityIfNeeded(type)
                ?: return mappedProjection

            return when (mappedProjection.kind + projection.kind) {
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(mappedType)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(mappedType)
                ProjectionKind.INVARIANT -> mappedType
            }
        }
    }

    return substitutor.substituteOrSelf(resultingType)
}
