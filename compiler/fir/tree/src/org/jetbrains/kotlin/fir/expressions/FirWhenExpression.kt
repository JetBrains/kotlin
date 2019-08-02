/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirWhenExpression(psi: PsiElement?) : FirCallLikeControlFlowExpression(psi) {
    abstract val subject: FirExpression?

    // when (val subjectVariable = subject()) { ... }
    abstract val subjectVariable: FirVariable<*>?

    abstract val branches: List<FirWhenBranch>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitWhenExpression(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        subjectVariable?.accept(visitor, data) ?: subject?.accept(visitor, data)
        for (branch in branches) {
            branch.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }

    abstract fun <D> transformBranches(transformer: FirTransformer<D>, data: D): FirWhenExpression

    abstract fun <D> transformSubject(transformer: FirTransformer<D>, data: D): FirWhenExpression
}