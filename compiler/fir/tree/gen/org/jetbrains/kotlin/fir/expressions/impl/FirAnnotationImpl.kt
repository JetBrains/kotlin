/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnnotationImpl(
    override val source: KtSourceElement?,
    override var useSiteTarget: AnnotationUseSiteTarget?,
    override var annotationTypeRef: FirTypeRef,
    override var argumentMapping: FirAnnotationArgumentMapping,
    override var typeArguments: MutableOrEmptyList<FirTypeProjection>,
) : FirAnnotation() {
    override val typeRef: FirTypeRef get() = annotationTypeRef
    override val annotations: List<FirAnnotation> get() = emptyList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotationTypeRef.accept(visitor, data)
        argumentMapping.accept(visitor, data)
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnnotationImpl {
        transformAnnotationTypeRef(transformer, data)
        argumentMapping = argumentMapping.transform(transformer, data)
        transformTypeArguments(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotationImpl {
        return this
    }

    override fun <D> transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotationImpl {
        annotationTypeRef = annotationTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirAnnotationImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun replaceUseSiteTarget(newUseSiteTarget: AnnotationUseSiteTarget?) {
        useSiteTarget = newUseSiteTarget
    }

    override fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef) {
        annotationTypeRef = newAnnotationTypeRef
    }

    override fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping) {
        argumentMapping = newArgumentMapping
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments = newTypeArguments.toMutableOrEmpty()
    }
}
