/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirBinaryLogicExpression(psi: PsiElement?) : FirUnknownTypeExpression(psi) {
    abstract val leftOperand: FirExpression
    abstract val rightOperand: FirExpression
    abstract val kind: OperationKind

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return visitor.visitBinaryLogicExpression(this, data)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super.acceptChildren(visitor, data)
        leftOperand.accept(visitor, data)
        rightOperand.accept(visitor, data)
    }

    abstract fun <D> transformLeftOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression
    abstract fun <D> transformRightOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression
    abstract fun <D> transformRestChildren(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression

    enum class OperationKind(val token: String) {
        AND("&&"), OR("||")
    }
}