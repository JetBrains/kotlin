/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.types.SmartcastStability

@OptIn(UnresolvedExpressionTypeAccess::class)
internal class FirSmartCastExpressionImpl(
    @property:UnresolvedExpressionTypeAccess
    override var coneTypeOrNull: ConeKotlinType?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var originalExpression: FirExpression,
    override val typesFromSmartCast: Collection<ConeKotlinType>,
    override var smartcastType: FirTypeRef,
    override var smartcastTypeWithoutNullableNothing: FirTypeRef?,
    override val smartcastStability: SmartcastStability,
) : FirSmartCastExpression() {
    override val source: KtSourceElement? = originalExpression.source?.fakeElement(KtFakeSourceElementKind.SmartCastExpression)
    override val isStable: Boolean
        get() = smartcastStability == SmartcastStability.STABLE_VALUE

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        originalExpression.accept(visitor, data)
        smartcastType.accept(visitor, data)
        smartcastTypeWithoutNullableNothing?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSmartCastExpressionImpl {
        transformAnnotations(transformer, data)
        transformOriginalExpression(transformer, data)
        smartcastType = smartcastType.transform(transformer, data)
        smartcastTypeWithoutNullableNothing = smartcastTypeWithoutNullableNothing?.transform(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirSmartCastExpressionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOriginalExpression(transformer: FirTransformer<D>, data: D): FirSmartCastExpressionImpl {
        originalExpression = originalExpression.transform(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceOriginalExpression(newOriginalExpression: FirExpression) {
        originalExpression = newOriginalExpression
    }
}
