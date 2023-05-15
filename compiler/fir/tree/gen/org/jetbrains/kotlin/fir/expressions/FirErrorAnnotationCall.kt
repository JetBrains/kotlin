/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorAnnotationCall : FirAnnotationCall(), FirDiagnosticHolder {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val useSiteTarget: AnnotationUseSiteTarget?
    abstract override val annotationTypeRef: FirTypeRef
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val argumentList: FirArgumentList
    abstract override val calleeReference: FirReference
    abstract override val annotationResolvePhase: FirAnnotationResolvePhase
    abstract override val diagnostic: ConeDiagnostic
    abstract override val argumentMapping: FirAnnotationArgumentMapping

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorAnnotationCall(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformErrorAnnotationCall(this, data) as E

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceUseSiteTarget(newUseSiteTarget: AnnotationUseSiteTarget?)

    abstract override fun replaceAnnotationTypeRef(newAnnotationTypeRef: FirTypeRef)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun replaceArgumentList(newArgumentList: FirArgumentList)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract override fun replaceAnnotationResolvePhase(newAnnotationResolvePhase: FirAnnotationResolvePhase)

    abstract override fun replaceArgumentMapping(newArgumentMapping: FirAnnotationArgumentMapping)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorAnnotationCall

    abstract override fun <D> transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirErrorAnnotationCall

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirErrorAnnotationCall

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirErrorAnnotationCall
}
