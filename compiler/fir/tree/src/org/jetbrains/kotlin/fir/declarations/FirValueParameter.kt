/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
interface FirValueParameter : @VisitedSupertype FirDeclaration, FirTypedDeclaration, FirNamedDeclaration, FirVariable {
    val isCrossinline: Boolean

    val isNoinline: Boolean

    val isVararg: Boolean

    val defaultValue: FirExpression?

    override val symbol: FirVariableSymbol

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirVariable>.acceptChildren(visitor, data)
        defaultValue?.accept(visitor, data)
    }
}