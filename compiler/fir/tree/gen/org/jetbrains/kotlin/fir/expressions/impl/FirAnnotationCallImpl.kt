/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirAnnotationCallImpl(
    override val source: FirSourceElement?,
    override val useSiteTarget: AnnotationUseSiteTarget?,
    override var annotationTypeRef: FirTypeRef
) : FirAnnotationCall(), FirCallWithArgumentList, FirAbstractAnnotatedElement {
    override val typeRef: FirTypeRef get() = annotationTypeRef
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val arguments: MutableList<FirExpression> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
        annotationTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnnotationCallImpl {
        annotations.transformInplace(transformer, data)
        transformArguments(transformer, data)
        annotationTypeRef = annotationTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirAnnotationCallImpl {
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}
