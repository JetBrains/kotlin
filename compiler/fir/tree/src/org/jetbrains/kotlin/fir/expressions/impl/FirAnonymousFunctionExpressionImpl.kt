/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal class FirAnonymousFunctionExpressionImpl(
    override val source: KtSourceElement?,
    override var anonymousFunction: FirAnonymousFunction
) : FirAnonymousFunctionExpression() {
    override val coneTypeOrNull: ConeKotlinType?
        get() = anonymousFunction.typeRef.coneTypeOrNull

    override val annotations: List<FirAnnotation>
        get() = anonymousFunction.annotations

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        anonymousFunction.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionExpressionImpl {
        return transformAnonymousFunction(transformer, data)
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        anonymousFunction.replaceAnnotations(newAnnotations)
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionExpressionImpl {
        return this
    }

    override fun <D> transformAnonymousFunction(transformer: FirTransformer<D>, data: D): FirAnonymousFunctionExpressionImpl {
        anonymousFunction = anonymousFunction.transform(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        shouldNotBeCalled("anonymousFunction.replaceTypeRef() should be called instead")
    }
}
