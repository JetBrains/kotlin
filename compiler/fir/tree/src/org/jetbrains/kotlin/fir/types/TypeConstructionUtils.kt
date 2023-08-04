/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId

fun ConeClassifierLookupTag.constructType(
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable, attributes)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, isNullable, attributes)
        else -> error("! ${this::class}")
    }
}

fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this, typeArguments, isNullable, attributes)
}

fun ClassId.toLookupTag(): ConeClassLikeLookupTagImpl {
    return ConeClassLikeLookupTagImpl(this)
}

fun ClassId.constructClassLikeType(
    typeArguments: Array<out ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isNullable, attributes)
}

fun FirClassifierSymbol<*>.constructType(
    typeArguments: Array<ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is FirTypeParameterSymbol -> ConeTypeParameterTypeImpl(this.toLookupTag(), isNullable, attributes)
        is FirClassLikeSymbol<*> -> constructType(typeArguments, isNullable, attributes)
    }
}

fun FirClassLikeSymbol<*>.constructType(
    typeArguments: Array<ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isNullable, attributes)
}

fun FirClassSymbol<*>.constructStarProjectedType(
    typeParameterNumber: Int = typeParameterSymbols.size,
    isNullable: Boolean = false
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(
        toLookupTag(),
        Array(typeParameterNumber) { ConeStarProjection },
        isNullable
    )
}
