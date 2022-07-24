/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirBreakExpression : FirLoopJump() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val target: FirTarget<FirLoop>


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)
}

inline fun <D> FirBreakExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirBreakExpression 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirBreakExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirBreakExpression 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }
