/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl

fun ConeClassLikeType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = FirTypeAlias::expandedConeType,
): ConeClassLikeType {
    if (this is ConeClassLikeTypeImpl) {
        val expandedTypeAndSession = cachedExpandedType
        if (expandedTypeAndSession != null && expandedTypeAndSession.first === useSiteSession) {
            return expandedTypeAndSession.second
        }

        val computedExpandedType = fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
        cachedExpandedType = Pair(useSiteSession, computedExpandedType)
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

    val resultType = expandedConeType(typeAlias)?.applyNullabilityFrom(this) ?: return null

    if (resultType.typeArguments.isEmpty()) return resultType
    return mapTypeAliasArguments(typeAlias, this, resultType) as? ConeClassLikeType
}

private fun ConeClassLikeType.applyNullabilityFrom(abbreviation: ConeClassLikeType): ConeClassLikeType {
    if (abbreviation.isMarkedNullable) return withNullability(ConeNullability.NULLABLE)
    return this
}

private fun mapTypeAliasArguments(
    typeAlias: FirTypeAlias,
    abbreviatedType: ConeClassLikeType,
    resultingType: ConeClassLikeType,
): ConeKotlinType {
    val typeAliasMap = typeAlias.typeParameters.map { it.symbol }.zip(abbreviatedType.typeArguments).toMap()

    val substitutor = object : AbstractConeSubstitutor() {
        override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
            return null
        }

        override fun substituteArgument(projection: ConeTypeProjection): ConeTypeProjection? {
            val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
            val symbol = (type as? ConeTypeParameterType)?.lookupTag?.toSymbol() ?: return super.substituteArgument(projection)
            val mappedProjection = typeAliasMap[symbol] ?: return super.substituteArgument(projection)
            val mappedType = (mappedProjection as? ConeKotlinTypeProjection)?.type ?: return mappedProjection

            @Suppress("MoveVariableDeclarationIntoWhen")
            val resultingKind = mappedProjection.kind + projection.kind
            return when (resultingKind) {
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(mappedType)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(mappedType)
                ProjectionKind.INVARIANT -> mappedType
            }
        }
    }

    return substitutor.substituteOrSelf(resultingType)
}
