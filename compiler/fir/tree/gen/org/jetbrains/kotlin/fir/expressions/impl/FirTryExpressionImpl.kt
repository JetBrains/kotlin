/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirTryExpressionImpl(
    override val source: KtSourceElement?,
    override var coneTypeOrNull: ConeKotlinType?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var calleeReference: FirReference,
    override var tryBlock: FirBlock,
    override val catches: MutableList<FirCatch>,
    override var finallyBlock: FirBlock?,
) : FirTryExpression() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        calleeReference.accept(visitor, data)
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        transformCalleeReference(transformer, data)
        transformTryBlock(transformer, data)
        transformCatches(transformer, data)
        transformFinallyBlock(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        calleeReference = calleeReference.transform(transformer, data)
        return this
    }

    override fun <D> transformTryBlock(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        tryBlock = tryBlock.transform(transformer, data)
        return this
    }

    override fun <D> transformCatches(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        catches.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformFinallyBlock(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        finallyBlock = finallyBlock?.transform(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirTryExpressionImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }
}
