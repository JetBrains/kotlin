/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirNamedFunction : @VisitedSupertype FirFunction, FirCallableMember {
    val isOperator: Boolean

    val isInfix: Boolean

    val isInline: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitNamedFunction(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        super<FirCallableMember>.acceptChildren(visitor, data)
        super<FirFunction>.acceptChildren(visitor, data)
    }
}