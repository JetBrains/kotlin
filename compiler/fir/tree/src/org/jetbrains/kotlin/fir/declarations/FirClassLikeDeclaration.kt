/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol

interface FirClassLikeDeclaration : FirMemberDeclaration {
    val symbol: ConeClassLikeSymbol
}

fun ConeClassifierSymbol.toFirClassLike(): FirClassLikeDeclaration? =
    when (this) {
        is FirClassSymbol -> this.fir
        is FirTypeAliasSymbol -> this.fir
        else -> null
    }
