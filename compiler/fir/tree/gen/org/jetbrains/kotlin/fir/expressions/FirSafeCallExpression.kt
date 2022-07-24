/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirSafeCallExpression : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val receiver: FirExpression
    abstract val checkedSubjectRef: FirExpressionRef<FirCheckedSafeCallSubject>
    abstract val selector: FirStatement


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceReceiver(newReceiver: FirExpression)

    abstract fun replaceSelector(newSelector: FirStatement)
}

inline fun <D> FirSafeCallExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirSafeCallExpression 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirSafeCallExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirSafeCallExpression 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirSafeCallExpression.transformReceiver(transformer: FirTransformer<D>, data: D): FirSafeCallExpression 
     = apply { replaceReceiver(receiver.transform(transformer, data)) }

inline fun <D> FirSafeCallExpression.transformSelector(transformer: FirTransformer<D>, data: D): FirSafeCallExpression 
     = apply { replaceSelector(selector.transform(transformer, data)) }
