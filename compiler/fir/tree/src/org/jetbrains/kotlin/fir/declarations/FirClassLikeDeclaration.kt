/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirClassLikeDeclaration<F : FirClassLikeDeclaration<F>> :
    @VisitedSupertype FirMemberDeclaration, FirStatement, FirSymbolOwner<F> {
    override val symbol: FirClassLikeSymbol<F>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirMemberDeclaration>.accept(visitor, data)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirMemberDeclaration>.acceptChildren(visitor, data)
    }
}

fun ConeClassifierSymbol.toFirClassLike(): FirClassLikeDeclaration<*>? =
    when (this) {
        is FirClassSymbol -> this.fir
        is FirTypeAliasSymbol -> this.fir
        else -> null
    }
