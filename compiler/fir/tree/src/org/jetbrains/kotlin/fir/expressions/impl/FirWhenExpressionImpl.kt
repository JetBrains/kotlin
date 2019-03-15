/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirWhenExpressionImpl(
    session: FirSession,
    psiElement: PsiElement?,
    override var subject: FirExpression? = null,
    override var subjectVariable: FirVariable? = null
) : FirAbstractExpression(session, psiElement), FirWhenExpression {
    override val branches = mutableListOf<FirWhenBranch>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        if (subjectVariable != null) {
            subjectVariable = subjectVariable?.transformSingle(transformer, data)
        } else {
            subject = subject?.transformSingle(transformer, data)
        }
        branches.transformInplace(transformer, data)
        return super<FirAbstractExpression>.transformChildren(transformer, data)
    }
}