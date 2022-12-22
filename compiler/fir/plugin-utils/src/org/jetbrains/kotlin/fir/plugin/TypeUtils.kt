/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId

public fun ClassId.createConeType(
    session: FirSession,
    typeArguments: Array<ConeTypeProjection> = emptyArray(),
    nullable: Boolean = false
): ConeClassLikeType {
    val symbol = session.symbolProvider.getClassLikeSymbolByClassId(this) as? FirClassSymbol<*>

    return when {
        symbol != null -> when {
            typeArguments.isEmpty() -> symbol.constructStarProjectedType(isNullable = nullable)
            else -> symbol.constructType(typeArguments, nullable)
        }
        else -> ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(this),
            typeArguments,
            nullable
        )
    }
}
