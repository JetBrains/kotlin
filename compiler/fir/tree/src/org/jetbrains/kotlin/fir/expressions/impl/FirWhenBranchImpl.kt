/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirWhenBranchImpl(
    psi: PsiElement?,
    override var condition: FirExpression,
    override var result: FirBlock
) : FirAbstractElement(psi), FirWhenBranch {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        transformCondition(transformer, data)
        result = result.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirWhenBranch {
        condition = condition.transformSingle(transformer, data)
        return this
    }
}