/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId

fun ConeClassifierLookupTag.constructType(
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean
): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, isNullable)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, isNullable)
        else -> error("! ${this::class}")
    }
}

fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean,
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(this, typeArguments, isNullable)
}

fun ClassId.constructClassLikeType(
    typeArguments: Array<out ConeTypeProjection>,
    isNullable: Boolean,
): ConeClassLikeType {
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(this), typeArguments, isNullable)
}

