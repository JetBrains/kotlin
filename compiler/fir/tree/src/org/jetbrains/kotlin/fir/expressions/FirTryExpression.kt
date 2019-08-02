/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirTryExpression(
    psi: PsiElement?
) : FirCallLikeControlFlowExpression(psi) {
    abstract val tryBlock: FirBlock

    abstract val catches: List<FirCatch>

    abstract val finallyBlock: FirBlock?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitTryExpression(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }

    abstract fun <D> transformTryBlock(transformer: FirTransformer<D>, data: D): FirTryExpression
    abstract fun <D> transformCatches(transformer: FirTransformer<D>, data: D): FirTryExpression
    abstract fun <D> transformFinallyBlock(transformer: FirTransformer<D>, data: D): FirTryExpression
}