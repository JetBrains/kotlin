/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class FirJavaExternalAnnotation @FirImplementationDetail constructor(
    override var annotationTypeRef: FirTypeRef,
    override var argumentMapping: FirAnnotationArgumentMapping,
) : FirAnnotation() {
    override val useSiteTarget: AnnotationUseSiteTarget? get() = null
    override val source: KtSourceElement? get() = null

    override val typeArguments: List<FirTypeProjection>
        get() = emptyList()

    @OptIn(UnresolvedExpressionTypeAccess::class)
    override val coneTypeOrNull: ConeKotlinType?
        get() = annotationTypeRef.coneTypeOrNull

    override val annotations: List<FirAnnotation>
        get() = emptyList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotationTypeRef.accept(visitor, data)
        argumentMapping.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirJavaExternalAnnotation {
        transformAnnotationTypeRef(transformer, data)
        argumentMapping = argumentMapping.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirJavaExternalAnnotation {
        return this
    }

    override fun <D> transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirJavaExternalAnnotation {
        annotationTypeRef = annotationTypeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirJavaExternalAnnotation {
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {}

    override fun replaceUseSiteTarget(newUseSiteTarget: AnnotationUseSiteTarget?) {}

    override fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef) {
        annotationTypeRef = newAnnotationTypeRef
    }

    override fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping) {
        argumentMapping = newArgumentMapping
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {}
}

@FirBuilderDsl
class FirJavaExternalAnnotationBuilder {
    lateinit var annotationTypeRef: FirTypeRef
    lateinit var argumentMapping: FirAnnotationArgumentMapping

    @OptIn(FirImplementationDetail::class)
    fun build(): FirJavaExternalAnnotation = FirJavaExternalAnnotation(
        annotationTypeRef,
        argumentMapping,
    )
}

@OptIn(ExperimentalContracts::class)
inline fun buildJavaExternalAnnotation(init: FirJavaExternalAnnotationBuilder.() -> Unit): FirJavaExternalAnnotation {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return FirJavaExternalAnnotationBuilder().apply(init).build()
}
