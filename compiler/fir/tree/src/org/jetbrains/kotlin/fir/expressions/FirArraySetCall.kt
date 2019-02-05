/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirArraySetCall : @VisitedSupertype FirCall, FirAssignment {
    // NB: arguments of this thing are indexes AND rvalue
    val indexes: List<FirExpression>

    override val arguments get() = indexes + rValue

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitArraySetCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (index in indexes) {
            index.accept(visitor, data)
        }
        acceptAnnotations(visitor, data)
        super<FirAssignment>.acceptChildren(visitor, data)
    }
}