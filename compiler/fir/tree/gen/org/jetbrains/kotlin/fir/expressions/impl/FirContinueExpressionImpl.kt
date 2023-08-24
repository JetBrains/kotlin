/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirTarget
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNothingTypeRef
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirContinueExpressionImpl(
    override val source: KtSourceElement?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val target: FirTarget<FirLoop>,
) : FirContinueExpression() {
    override val coneTypeOrNull: ConeKotlinType? = StandardClassIds.Nothing.constructClassLikeType()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirContinueExpressionImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirContinueExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
