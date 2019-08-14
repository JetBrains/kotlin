/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBinaryLogicExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirBinaryLogicExpressionImpl(
    psi: PsiElement?,
    override var leftOperand: FirExpression,
    override var rightOperand: FirExpression,
    override val kind: OperationKind
) : FirBinaryLogicExpression(psi) {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        transformLeftOperand(transformer, data)
        transformRightOperand(transformer, data)
        return super.transformChildren(transformer, data)
    }

    override fun <D> transformLeftOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression {
        leftOperand = leftOperand.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformRightOperand(transformer: FirTransformer<D>, data: D): FirBinaryLogicExpression {
        rightOperand = rightOperand.transformSingle(transformer, data)
        return this
    }
}