/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirCatchImpl(
    psi: PsiElement?,
    override var parameter: FirValueParameter,
    override var block: FirBlock
) : FirAbstractElement(psi), FirCatch {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        transformParameter(transformer, data)
        transformBlock(transformer, data)
        return this
    }

    override fun <D> transformParameter(transformer: FirTransformer<D>, data: D): FirCatch {
        parameter = parameter.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirCatch {
        block = block.transformSingle(transformer, data)
        return this
    }
}