/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeFunctionTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

inline fun <K, V, VA : V> MutableMap<K, V>.getOrPut(key: K, defaultValue: (K) -> VA, postCompute: (VA) -> Unit): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue(key)
        put(key, answer)
        postCompute(answer)
        answer
    } else {
        value
    }
}

fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): ConeClassifierSymbol? =
    useSiteSession.getService(FirSymbolProvider::class).getSymbolByLookupTag(this)

fun ConeAbbreviatedType.directExpansionType(useSiteSession: FirSession): ConeClassLikeType? =
    abbreviationLookupTag
        .toSymbol(useSiteSession)
        ?.safeAs<FirTypeAliasSymbol>()?.fir?.expandedConeType

fun ConeClassifierLookupTag.toSymbol(useSiteSession: FirSession): ConeClassifierSymbol? =
    when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeTypeParameterSymbol -> this
        else -> error("sealed")
    }

fun ConeClassifierSymbol.constructType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterSymbol -> {
            ConeTypeParameterTypeImpl(this, isNullable)
        }
        is ConeClassSymbol -> {
            ConeClassTypeImpl(this.toLookupTag(), typeArguments, isNullable)
        }
        is FirTypeAliasSymbol -> {
            ConeAbbreviatedTypeImpl(
                abbreviationLookupTag = this.toLookupTag(),
                typeArguments = typeArguments,
                isNullable = isNullable
            )
        }
        else -> error("!")
    }
}

fun ConeClassifierSymbol.constructType(parts: List<FirQualifierPart>, isNullable: Boolean): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)

fun ConeKotlinType.toTypeProjection(variance: Variance): ConeKotlinTypeProjection =
    when (variance) {
        Variance.INVARIANT -> this
        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(this)
        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(this)
    }

private fun List<FirQualifierPart>.toTypeProjections(): Array<ConeKotlinTypeProjection> = flatMap {
    it.typeArguments.map { typeArgument ->
        when (typeArgument) {
            is FirStarProjection -> ConeStarProjection
            is FirTypeProjectionWithVariance -> {
                val type = (typeArgument.typeRef as FirResolvedTypeRef).type
                type.toTypeProjection(typeArgument.variance)
            }
            else -> error("!")
        }
    }
}.toTypedArray()


fun <T : ConeKotlinType> T.withNullability(nullability: ConeNullability): T {
    if (this.nullability == nullability) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassTypeImpl -> ConeClassTypeImpl(lookupTag, typeArguments, nullability.isNullable) as T
        is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
            abbreviationLookupTag,
            typeArguments,
            nullability.isNullable
        ) as T
        is ConeFunctionTypeImpl -> ConeFunctionTypeImpl(receiverType, parameterTypes, returnType, lookupTag, nullability.isNullable) as T
        is ConeTypeParameterTypeImpl -> ConeTypeParameterTypeImpl(lookupTag, nullability.isNullable) as T
        is ConeFlexibleType -> ConeFlexibleType(lowerBound.withNullability(nullability), upperBound.withNullability(nullability)) as T
        else -> TODO("FIX KOTLIN COMPILER")
    }
}


fun <T : ConeKotlinType> T.withArguments(arguments: Array<ConeKotlinTypeProjection>): T {
    if (this.typeArguments === arguments) {
        return this
    }

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassTypeImpl -> ConeClassTypeImpl(lookupTag, arguments, nullability.isNullable) as T
        is ConeAbbreviatedTypeImpl -> ConeAbbreviatedTypeImpl(
            abbreviationLookupTag,
            arguments,
            nullability.isNullable
        ) as T
        //is ConeFunctionTypeImpl -> ConeFunctionTypeImpl(receiverType, parameterTypes, returnType, lookupTag, nullability.isNullable) as T
        else -> error("Not supported: $this: ${this.render()}")
    }
}
