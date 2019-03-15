/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

@BaseTransformedType
interface FirNamedFunction : @VisitedSupertype FirFunction, FirCallableMember {
    val isOperator: Boolean get() = status.isOperator

    val isInfix: Boolean get() = status.isInfix

    val isInline: Boolean get() = status.isInline

    val isTailRec: Boolean get() = status.isTailRec

    val isExternal: Boolean get() = status.isExternal

    val isSuspend: Boolean get() = status.isSuspend

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitNamedFunction(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirCallableMember>.acceptChildren(visitor, data)
        for (parameter in valueParameters) {
            parameter.accept(visitor, data)
        }
        body?.accept(visitor, data)
    }
}