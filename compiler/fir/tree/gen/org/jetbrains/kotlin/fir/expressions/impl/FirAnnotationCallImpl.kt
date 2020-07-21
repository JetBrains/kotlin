/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolveStatus
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAnnotationCallImpl(
    override val source: FirSourceElement?,
    override val annotations: MutableList<FirAnnotationCall>,
    override var argumentList: FirArgumentList,
    override var calleeReference: FirReference,
    override val useSiteTarget: AnnotationUseSiteTarget?,
    override var annotationTypeRef: FirTypeRef,
    override var resolveStatus: FirAnnotationResolveStatus,
) : FirAnnotationCall() {
    override val typeRef: FirTypeRef get() = annotationTypeRef

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        calleeReference.accept(visitor, data)
        annotationTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnnotationCallImpl {
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        transformCalleeReference(transformer, data)
        transformAnnotationTypeRef(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotationCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirAnnotationCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotationTypeRef(transformer: FirTransformer<D>, data: D): FirAnnotationCallImpl {
        annotationTypeRef = annotationTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceResolveStatus(newResolveStatus: FirAnnotationResolveStatus) {
        resolveStatus = newResolveStatus
    }
}
