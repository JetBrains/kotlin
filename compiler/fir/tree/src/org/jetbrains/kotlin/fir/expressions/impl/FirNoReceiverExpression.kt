/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirNoReceiverExpression : FirExpression() {
    override val source: KtSourceElement? get() = null
    override val coneTypeOrNull: ConeKotlinType? get() = null
    override val annotations: List<FirAnnotation> get() = emptyList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirNoReceiverExpression {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        throw AssertionError("Mutating annotations of FirNoReceiverExpression is not supported")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirExpression {
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {}
}
