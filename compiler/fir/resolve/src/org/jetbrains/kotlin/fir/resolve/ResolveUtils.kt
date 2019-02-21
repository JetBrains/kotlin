/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.types.Variance

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

fun ConeSymbol.constructType(typeArguments: Array<ConeKotlinTypeProjection>, isNullable: Boolean): ConeKotlinType {
    return when (this) {
        is ConeTypeParameterSymbol -> {
            ConeTypeParameterTypeImpl(this, isNullable)
        }
        is ConeClassSymbol -> {
            ConeClassTypeImpl(this, typeArguments, isNullable)
        }
        is FirTypeAliasSymbol -> {
            ConeAbbreviatedTypeImpl(
                abbreviationSymbol = this as ConeClassLikeSymbol,
                typeArguments = typeArguments,
                directExpansion = fir.expandedConeType ?: ConeClassErrorType("Unresolved expansion"),
                isNullable = isNullable
            )
        }
        else -> error("!")
    }
}

fun ConeSymbol.constructType(parts: List<FirQualifierPart>, isNullable: Boolean): ConeKotlinType =
    constructType(parts.toTypeProjections(), isNullable)

private fun List<FirQualifierPart>.toTypeProjections(): Array<ConeKotlinTypeProjection> = flatMap {
    it.typeArguments.map {
        when (it) {
            is FirStarProjection -> StarProjection
            is FirTypeProjectionWithVariance -> {
                val type = (it.typeRef as FirResolvedTypeRef).type
                when (it.variance) {
                    Variance.INVARIANT -> type
                    Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(type)
                    Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(type)
                }
            }
            else -> error("!")
        }
    }
}.toTypedArray()


