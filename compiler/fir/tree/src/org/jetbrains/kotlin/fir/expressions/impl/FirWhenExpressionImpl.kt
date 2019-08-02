/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.references.FirStubReference
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

class FirWhenExpressionImpl(
    psiElement: PsiElement?,
    override var subject: FirExpression? = null,
    override var subjectVariable: FirVariable<*>? = null,
    override var calleeReference: FirReference = FirStubReference()
) : FirWhenExpression(psiElement) {
    override val branches = mutableListOf<FirWhenBranch>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        transformSubject(transformer, data)
        branches.transformInplace(transformer, data)
        return super.transformChildren(transformer, data)
    }

    override fun <D> transformBranches(transformer: FirTransformer<D>, data: D): FirWhenExpression {
        branches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformSubject(transformer: FirTransformer<D>, data: D): FirWhenExpression {
        if (subjectVariable != null) {
            subjectVariable = subjectVariable?.transformSingle(transformer, data)
        } else {
            subject = subject?.transformSingle(transformer, data)
        }
        return this
    }
}