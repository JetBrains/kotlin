/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl

fun FirClassSymbol<*>.constructStarProjectedType(typeParameterNumber: Int): ConeClassLikeType {
    return ConeClassLikeTypeImpl(
        toLookupTag(),
        Array(typeParameterNumber) { ConeStarProjection },
        isNullable = false
    )
}