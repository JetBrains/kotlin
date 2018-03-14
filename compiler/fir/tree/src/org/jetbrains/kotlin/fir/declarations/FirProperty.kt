/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.visitors.FirVisitor

// May be should not inherit FirVariable
interface FirProperty : @VisitedSupertype FirDeclaration, FirCallableMember, FirVariable {
    // Should it be nullable or have some default?
    val getter: FirPropertyAccessor

    val setter: FirPropertyAccessor

    val delegate: FirExpression?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        super<FirCallableMember>.acceptChildren(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter.accept(visitor, data)
        setter.accept(visitor, data)
    }
}