/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirThrowExpression : FirExpression {
    val exception: FirExpression

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitThrowExpression(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        exception.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}