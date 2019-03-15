/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirTryExpressionImpl(
    session: FirSession,
    psi: PsiElement?,
    override var tryBlock: FirBlock,
    override var finallyBlock: FirBlock?
) : FirAbstractExpression(session, psi), FirTryExpression {
    override val catches = mutableListOf<FirCatch>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        tryBlock = tryBlock.transformSingle(transformer, data)
        finallyBlock = finallyBlock?.transformSingle(transformer, data)
        catches.transformInplace(transformer, data)
        return super<FirAbstractExpression>.transformChildren(transformer, data)
    }
}