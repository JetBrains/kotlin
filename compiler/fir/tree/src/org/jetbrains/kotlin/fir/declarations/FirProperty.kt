/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// May be should not inherit FirVariable
@BaseTransformedType
interface FirProperty : @VisitedSupertype FirDeclaration, FirCallableMember, FirVariable {
    val isConst: Boolean get() = status.isConst

    val isLateInit: Boolean get() = status.isLateInit

    // Should it be nullable or have some default?
    val getter: FirPropertyAccessor

    val setter: FirPropertyAccessor

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirCallableMember>.acceptChildren(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter.accept(visitor, data)
        setter.accept(visitor, data)
    }
}