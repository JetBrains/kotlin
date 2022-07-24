/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAugmentedArraySetCall : FirPureAbstractElement(), FirStatement {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract val lhsGetCall: FirFunctionCall
    abstract val rhs: FirExpression
    abstract val operation: FirOperation
    abstract val calleeReference: FirReference
    abstract val arrayAccessSource: KtSourceElement?


    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceLhsGetCall(newLhsGetCall: FirFunctionCall)

    abstract fun replaceRhs(newRhs: FirExpression)

    abstract fun replaceCalleeReference(newCalleeReference: FirReference)
}

inline fun <D> FirAugmentedArraySetCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirAugmentedArraySetCall 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirAugmentedArraySetCall.transformLhsGetCall(transformer: FirTransformer<D>, data: D): FirAugmentedArraySetCall 
     = apply { replaceLhsGetCall(lhsGetCall.transform(transformer, data)) }

inline fun <D> FirAugmentedArraySetCall.transformRhs(transformer: FirTransformer<D>, data: D): FirAugmentedArraySetCall 
     = apply { replaceRhs(rhs.transform(transformer, data)) }

inline fun <D> FirAugmentedArraySetCall.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirAugmentedArraySetCall 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }
