/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDoWhileLoop : FirLoop() {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val block: FirBlock
    abstract override val condition: FirExpression
    abstract override val label: FirLabel?


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceBlock(newBlock: FirBlock)

    abstract override fun replaceCondition(newCondition: FirExpression)

    abstract override fun replaceLabel(newLabel: FirLabel?)
}

inline fun <D> FirDoWhileLoop.transformAnnotations(transformer: FirTransformer<D>, data: D): FirDoWhileLoop 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirDoWhileLoop.transformBlock(transformer: FirTransformer<D>, data: D): FirDoWhileLoop 
     = apply { replaceBlock(block.transform(transformer, data)) }

inline fun <D> FirDoWhileLoop.transformCondition(transformer: FirTransformer<D>, data: D): FirDoWhileLoop 
     = apply { replaceCondition(condition.transform(transformer, data)) }

inline fun <D> FirDoWhileLoop.transformLabel(transformer: FirTransformer<D>, data: D): FirDoWhileLoop 
     = apply { replaceLabel(label?.transform(transformer, data)) }
