/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirWrappedDelegateExpressionImpl(
    psi: PsiElement?,
    override var expression: FirExpression
) : FirWrappedDelegateExpression(psi) {
    override lateinit var delegateProvider: FirExpression

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        expression = expression.transformSingle(transformer, data)
        delegateProvider = delegateProvider.transformSingle(transformer, data)

        return super.transformChildren(transformer, data)
    }
}