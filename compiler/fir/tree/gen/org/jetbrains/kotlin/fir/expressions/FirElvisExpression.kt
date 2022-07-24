/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

abstract class FirElvisExpression : FirExpression(), FirResolvable {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val calleeReference: FirReference
    abstract val lhs: FirExpression
    abstract val rhs: FirExpression


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract fun replaceLhs(newLhs: FirExpression)

    abstract fun replaceRhs(newRhs: FirExpression)
}

inline fun <D> FirElvisExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirElvisExpression 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirElvisExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirElvisExpression 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirElvisExpression.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirElvisExpression 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }

inline fun <D> FirElvisExpression.transformLhs(transformer: FirTransformer<D>, data: D): FirElvisExpression 
     = apply { replaceLhs(lhs.transform(transformer, data)) }

inline fun <D> FirElvisExpression.transformRhs(transformer: FirTransformer<D>, data: D): FirElvisExpression 
     = apply { replaceRhs(rhs.transform(transformer, data)) }
