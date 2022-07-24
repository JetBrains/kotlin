/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTryExpression : FirExpression(), FirResolvable {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val calleeReference: FirReference
    abstract val tryBlock: FirBlock
    abstract val catches: List<FirCatch>
    abstract val finallyBlock: FirBlock?


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract fun replaceTryBlock(newTryBlock: FirBlock)

    abstract fun replaceCatches(newCatches: List<FirCatch>)

    abstract fun replaceFinallyBlock(newFinallyBlock: FirBlock?)
}

inline fun <D> FirTryExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirTryExpression  = 
    apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirTryExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirTryExpression  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirTryExpression.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirTryExpression  = 
    apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }

inline fun <D> FirTryExpression.transformTryBlock(transformer: FirTransformer<D>, data: D): FirTryExpression  = 
    apply { replaceTryBlock(tryBlock.transform(transformer, data)) }

inline fun <D> FirTryExpression.transformCatches(transformer: FirTransformer<D>, data: D): FirTryExpression  = 
    apply { replaceCatches(catches.transform(transformer, data)) }

inline fun <D> FirTryExpression.transformFinallyBlock(transformer: FirTransformer<D>, data: D): FirTryExpression  = 
    apply { replaceFinallyBlock(finallyBlock?.transform(transformer, data)) }
