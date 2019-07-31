/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirBlockImpl(psi: PsiElement?) : FirBlock(psi) {
    override val statements = mutableListOf<FirStatement>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        statements.transformInplace(transformer, data)
        return super.transformChildren(transformer, data)
    }
}