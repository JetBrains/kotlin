/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirDoWhileLoopImpl(psi: PsiElement?, condition: FirExpression) : FirAbstractLoop(psi, condition), FirDoWhileLoop {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        block = block.transformSingle(transformer, data)
        condition = condition.transformSingle(transformer, data)
        label = label?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        return this
    }
}