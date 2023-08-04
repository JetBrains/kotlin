/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnnotation : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract val useSiteTarget: AnnotationUseSiteTarget?
    abstract val annotationTypeRef: FirTypeRef
    abstract val argumentMapping: FirAnnotationArgumentMapping
    abstract val typeArguments: List<FirTypeProjection>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitAnnotation(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformAnnotation(this, data) as E

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceUseSiteTarget(newUseSiteTarget: AnnotationUseSiteTarget?)

    abstract fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef)

    abstract fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotation

    abstract fun <D> transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotation

    abstract fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirAnnotation
}
