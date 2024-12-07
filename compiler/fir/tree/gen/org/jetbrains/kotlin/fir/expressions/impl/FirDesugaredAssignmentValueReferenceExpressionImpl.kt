/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace

internal class FirDesugaredAssignmentValueReferenceExpressionImpl(
    override val source: KtSourceElement?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override val expressionRef: FirExpressionRef<FirExpression>,
) : FirDesugaredAssignmentValueReferenceExpression() {
    @OptIn(UnresolvedExpressionTypeAccess::class)
    override val coneTypeOrNull: ConeKotlinType?
        get() = expressionRef.value.coneTypeOrNull

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDesugaredAssignmentValueReferenceExpressionImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirDesugaredAssignmentValueReferenceExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        require(newConeTypeOrNull == coneTypeOrNull) { "${javaClass.simpleName}.replaceConeTypeOrNull() called with invalid type '${newConeTypeOrNull}'. Current type is '$coneTypeOrNull'" }
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }
}
