/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.references.FirStubReference
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirTryExpressionImpl(
    psi: PsiElement?,
    override var tryBlock: FirBlock,
    override var finallyBlock: FirBlock?,
    override var calleeReference: FirReference = FirStubReference()
) : FirTryExpression(psi) {
    override val catches = mutableListOf<FirCatch>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        transformTryBlock(transformer, data)
        transformCatches(transformer, data)
        transformFinallyBlock(transformer, data)
        return super.transformChildren(transformer, data)
    }

    override fun <D> transformTryBlock(transformer: FirTransformer<D>, data: D): FirTryExpression {
        tryBlock = tryBlock.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCatches(transformer: FirTransformer<D>, data: D): FirTryExpression {
        catches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformFinallyBlock(transformer: FirTransformer<D>, data: D): FirTryExpression {
        finallyBlock = finallyBlock?.transformSingle(transformer, data)
        return this
    }
}