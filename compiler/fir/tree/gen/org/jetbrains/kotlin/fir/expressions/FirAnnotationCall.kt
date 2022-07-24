/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnnotationCall : FirAnnotation(), FirCall, FirResolvable {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val useSiteTarget: AnnotationUseSiteTarget?
    abstract override val annotationTypeRef: FirTypeRef
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val argumentList: FirArgumentList
    abstract override val calleeReference: FirReference
    abstract override val argumentMapping: FirAnnotationArgumentMapping


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping)
}

inline fun <D> FirAnnotationCall.transformTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotationCall 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirAnnotationCall.transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotationCall 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirAnnotationCall.transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotationCall 
     = apply { replaceAnnotationTypeRef(annotationTypeRef.transform(transformer, data)) }

inline fun <D> FirAnnotationCall.transformTypeArguments(transformer: FirTransformer<D>, data: D): FirAnnotationCall 
     = apply { replaceTypeArguments(typeArguments.transform(transformer, data)) }

inline fun <D> FirAnnotationCall.transformArgumentList(transformer: FirTransformer<D>, data: D): FirAnnotationCall 
     = apply { replaceArgumentList(argumentList.transform(transformer, data)) }

inline fun <D> FirAnnotationCall.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirAnnotationCall 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }
