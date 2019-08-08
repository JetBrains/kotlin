/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirAbstractLoop(
    psi: PsiElement?,
    override var condition: FirExpression
) : FirAnnotatedStatement(psi), FirLoop {
    override lateinit var block: FirBlock

    override var label: FirLabel? = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        super<FirAnnotatedStatement>.acceptChildren(visitor, data)
        super<FirLoop>.acceptChildren(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        condition = condition.transformSingle(transformer, data)
        block = block.transformSingle(transformer, data)
        label = label?.transformSingle(transformer, data)
        return super<FirAnnotatedStatement>.transformChildren(transformer, data)
    }
}