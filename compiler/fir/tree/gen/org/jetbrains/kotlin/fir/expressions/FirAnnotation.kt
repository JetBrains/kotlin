/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnnotation : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val useSiteTarget: AnnotationUseSiteTarget?
    abstract val annotationTypeRef: FirTypeRef
    abstract val argumentMapping: FirAnnotationArgumentMapping
    abstract val typeArguments: List<FirTypeProjection>


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef)

    abstract fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)
}

inline fun <D> FirAnnotation.transformTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotation 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirAnnotation.transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotation 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirAnnotation.transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotation 
     = apply { replaceAnnotationTypeRef(annotationTypeRef.transform(transformer, data)) }

inline fun <D> FirAnnotation.transformArgumentMapping(transformer: FirTransformer<D>, data: D): FirAnnotation 
     = apply { replaceArgumentMapping(argumentMapping.transform(transformer, data)) }

inline fun <D> FirAnnotation.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirAnnotation 
     = apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }
