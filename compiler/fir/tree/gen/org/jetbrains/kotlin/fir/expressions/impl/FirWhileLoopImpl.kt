/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirWhileLoopImpl(
    override val source: FirSourceElement?,
    override var condition: FirExpression
) : FirWhileLoop(), FirAbstractLoop, FirAbstractAnnotatedElement {
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override var label: FirLabel? = null
    override lateinit var block: FirBlock

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        label?.accept(visitor, data)
        condition.accept(visitor, data)
        block.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirWhileLoopImpl {
        transformCondition(transformer, data)
        transformBlock(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: FirTransformer<D>, data: D): FirWhileLoopImpl {
        condition = condition.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformBlock(transformer: FirTransformer<D>, data: D): FirWhileLoopImpl {
        block = block.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirWhileLoopImpl {
        annotations.transformInplace(transformer, data)
        label = label?.transformSingle(transformer, data)
        return this
    }
}
