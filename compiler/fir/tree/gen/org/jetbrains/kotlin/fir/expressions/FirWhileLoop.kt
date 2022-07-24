/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirWhileLoop : FirLoop() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val label: FirLabel?
    abstract override val condition: FirExpression
    abstract override val block: FirBlock


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceLabel(newLabel: FirLabel?)

    abstract override fun replaceCondition(newCondition: FirExpression)

    abstract override fun replaceBlock(newBlock: FirBlock)
}

inline fun <D> FirWhileLoop.transformAnnotations(transformer: FirTransformer<D>, data: D): FirWhileLoop  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirWhileLoop.transformLabel(transformer: FirTransformer<D>, data: D): FirWhileLoop  = 
    apply { replaceLabel(label?.transform(transformer, data)) }

inline fun <D> FirWhileLoop.transformCondition(transformer: FirTransformer<D>, data: D): FirWhileLoop  = 
    apply { replaceCondition(condition.transform(transformer, data)) }

inline fun <D> FirWhileLoop.transformBlock(transformer: FirTransformer<D>, data: D): FirWhileLoop  = 
    apply { replaceBlock(block.transform(transformer, data)) }
