/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirAbstractLoop : FirLoop, FirAbstractAnnotatedElement {
    override val psi: PsiElement?
    override val annotations: MutableList<FirAnnotationCall>
    override var block: FirBlock
    override var condition: FirExpression
    override var label: FirLabel?
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        block.accept(visitor, data)
        condition.accept(visitor, data)
        label?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAbstractLoop {
        transformBlock(transformer, data)
        transformCondition(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirAbstractLoop {
        block = block.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirAbstractLoop {
        condition = condition.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirAbstractLoop {
        annotations.transformInplace(transformer, data)
        label = label?.transformSingle(transformer, data)
        return this
    }
}
