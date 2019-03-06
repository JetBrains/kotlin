/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirVariable : @VisitedSupertype FirDeclaration, FirTypedDeclaration, FirCallableDeclaration, FirNamedDeclaration, FirStatement {
    val isVar: Boolean

    val isVal: Boolean
        get() = !isVar

    val initializer: FirExpression?

    val delegate: FirExpression?

    override val symbol: FirVariableSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        super<FirCallableDeclaration>.acceptChildren(visitor, data)
    }
}