/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types


import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.utils.isError
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId

fun ConeClassifierLookupTag.constructType(
    typeArguments: Array<out ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isMarkedNullable, attributes)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, isMarkedNullable, attributes)
        else -> error("! ${this::class}")
    }
}

fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: Array<out ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this, typeArguments, isMarkedNullable, attributes)
}

fun ClassId.toLookupTag(): ConeClassLikeLookupTagImpl {
    return ConeClassLikeLookupTagImpl(this)
}

fun ClassId.constructClassLikeType(
    typeArguments: Array<out ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isMarkedNullable, attributes)
}

fun FirTypeParameterSymbol.constructType(
    typeArguments: Array<ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeTypeParameterType {
    return ConeTypeParameterTypeImpl(this.toLookupTag(), isMarkedNullable, attributes)
}

fun FirClassifierSymbol<*>.constructType(
    typeArguments: Array<ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty,
    // required if fir is not initialized (throw otherwise)
    status: FirDeclarationStatus? = null
): ConeRigidType {
    return when (this) {
        is FirTypeParameterSymbol -> ConeTypeParameterTypeImpl(this.toLookupTag(), isMarkedNullable, attributes)
        is FirClassLikeSymbol<*> if (status ?: fir.status).isError -> constructErrorType(typeArguments, isMarkedNullable)
        is FirClassLikeSymbol<*> -> constructType(typeArguments, isMarkedNullable, attributes)
    }
}

private fun FirClassLikeSymbol<*>.constructType(
    typeArguments: Array<ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isMarkedNullable, attributes)
}

private fun FirClassLikeSymbol<*>.constructErrorType(
    typeArguments: Array<ConeTypeProjection> = ConeTypeProjection.EMPTY_ARRAY,
    isMarkedNullable: Boolean = false,
): ConeErrorUnionType {
    check(!isMarkedNullable)
    check(typeArguments.isEmpty())
    return ConeErrorUnionType(
        StandardTypes.Nothing,
        CEClassifierType(this.toLookupTag())
    )
}

fun FirClassSymbol<*>.constructStarProjectedType(
    typeParameterNumber: Int = typeParameterSymbols.size,
    isMarkedNullable: Boolean = false,
): ConeRigidType {
    return if (isError) {
        ConeErrorUnionType(
            StandardTypes.Nothing,
            CEClassifierType(toLookupTag())
        )
    } else {
        ConeClassLikeTypeImpl(
            toLookupTag(),
            Array(typeParameterNumber) { ConeStarProjection },
            isMarkedNullable
        )
    }
}
