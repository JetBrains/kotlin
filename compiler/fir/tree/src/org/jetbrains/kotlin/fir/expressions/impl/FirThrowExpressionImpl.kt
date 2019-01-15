/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirThrowExpressionImpl(
    session: FirSession,
    psi: PsiElement?,
    override var exception: FirExpression
) : FirAbstractExpression(session, psi), FirThrowExpression {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        exception = exception.transformSingle(transformer, data)
        return super<FirAbstractExpression>.transformChildren(transformer, data)
    }
}