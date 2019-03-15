/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirReturnExpressionImpl(
    session: FirSession,
    psi: PsiElement?,
    override var result: FirExpression
) : FirAbstractExpression(session, psi), FirReturnExpression {
    override lateinit var target: FirTarget<FirFunction>

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        result = result.transformSingle(transformer, data)
        return super<FirAbstractExpression>.transformChildren(transformer, data)
    }
}