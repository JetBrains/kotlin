/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirWhenBranchImpl(
    session: FirSession,
    psi: PsiElement?,
    override var condition: FirExpression,
    override var result: FirBlock
) : FirAbstractElement(session, psi), FirWhenBranch {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        condition = condition.transformSingle(transformer, data)
        result = result.transformSingle(transformer, data)
        return this
    }
}