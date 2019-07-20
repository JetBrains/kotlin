/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.expressions.IrConstKind

abstract class FirConstExpression<T>(psi: PsiElement?) : FirUnknownTypeExpression(psi) {
    abstract val kind: IrConstKind<T>

    abstract val value: T

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return visitor.visitConstExpression(this, data)
    }
}