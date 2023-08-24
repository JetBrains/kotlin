/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

internal class FirAnonymousObjectExpressionImpl(
    override val source: KtSourceElement?,
    override var coneTypeOrNull: ConeKotlinType?,
    override var anonymousObject: FirAnonymousObject,
) : FirAnonymousObjectExpression() {
    override val annotations: List<FirAnnotation>
        get() = anonymousObject.annotations

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        anonymousObject.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnonymousObjectExpressionImpl {
        transformAnonymousObject(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        anonymousObject.replaceAnnotations(newAnnotations)
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousObjectExpressionImpl {
        return this
    }

    override fun <D> transformAnonymousObject(transformer: FirTransformer<D>, data: D): FirAnonymousObjectExpressionImpl {
        anonymousObject = anonymousObject.transform(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }
}
