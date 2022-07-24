/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirClassReferenceExpression : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val classTypeRef: FirTypeRef


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceClassTypeRef(newClassTypeRef: FirTypeRef)
}

inline fun <D> FirClassReferenceExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirClassReferenceExpression  = 
    apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirClassReferenceExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirClassReferenceExpression  = 
    apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirClassReferenceExpression.transformClassTypeRef(transformer: FirTransformer<D>, data: D): FirClassReferenceExpression  = 
    apply { replaceClassTypeRef(classTypeRef.transform(transformer, data)) }
