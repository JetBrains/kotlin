/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirCall : FirExpression {
    val arguments: List<FirExpression>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitCall(this, data)

    override fun <D> acceptChildren(visitor: FirVisitor<Unit, D>, data: D) {
        for (argument in arguments) {
            argument.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }
}