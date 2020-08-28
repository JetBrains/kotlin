/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

fun FirClassifierSymbol<*>.constructType(
    typeArguments: Array<ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (this) {
        is FirTypeParameterSymbol -> {
            ConeTypeParameterTypeImpl(this.toLookupTag(), isNullable, attributes)
        }
        is FirClassSymbol -> {
            val errorTypeRef = typeArguments.find {
                it is ConeClassErrorType
            }
            if (errorTypeRef is ConeClassErrorType) {
                ConeClassErrorType(errorTypeRef.diagnostic)
            } else {
                ConeClassLikeTypeImpl(this.toLookupTag(), typeArguments, isNullable, attributes)
            }
        }
        is FirTypeAliasSymbol -> {
            ConeClassLikeTypeImpl(
                this.toLookupTag(),
                typeArguments = typeArguments,
                isNullable = isNullable,
                attributes = attributes
            )
        }
        else -> error("!")
    }
}
