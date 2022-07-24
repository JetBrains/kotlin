/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnnotationImpl(
    override val source: KtSourceElement?,
    override val useSiteTarget: AnnotationUseSiteTarget?,
    override var annotationTypeRef: FirTypeRef,
    override var argumentMapping: FirAnnotationArgumentMapping,
    override val typeArguments: MutableList<FirTypeProjection>,
) : FirAnnotation() {
    override val typeRef: FirTypeRef get() = annotationTypeRef
    override val annotations: List<FirAnnotation> get() = emptyList()

    override val elementKind get() = FirElementKind.Annotation

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef) {
        annotationTypeRef = newAnnotationTypeRef
    }

    override fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping) {
        argumentMapping = newArgumentMapping
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }
}
