/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.expressions.IrConstKind

interface FirConstExpression<T> : FirExpression {
    val kind: IrConstKind<T>
    val value: T

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return visitor.visitConstExpression(this, data)
    }
}