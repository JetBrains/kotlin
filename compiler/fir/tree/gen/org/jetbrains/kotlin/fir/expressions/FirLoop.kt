/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

sealed class FirLoop : FirPureAbstractElement(), FirStatement, FirTargetElement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val block: FirBlock
    abstract val condition: FirExpression
    abstract val label: FirLabel?


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceBlock(newBlock: FirBlock)

    abstract fun replaceCondition(newCondition: FirExpression)

    abstract fun replaceLabel(newLabel: FirLabel?)
}

inline fun <D> FirLoop.transformAnnotations(transformer: FirTransformer<D>, data: D): FirLoop 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirLoop.transformBlock(transformer: FirTransformer<D>, data: D): FirLoop 
     = apply { replaceBlock(block.transform(transformer, data)) }

inline fun <D> FirLoop.transformCondition(transformer: FirTransformer<D>, data: D): FirLoop 
     = apply { replaceCondition(condition.transform(transformer, data)) }

inline fun <D> FirLoop.transformLabel(transformer: FirTransformer<D>, data: D): FirLoop 
     = apply { replaceLabel(label?.transform(transformer, data)) }
