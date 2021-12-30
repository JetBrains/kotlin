/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.types.SmartcastStability

class FirWhenSubjectExpressionWithSmartcastToNothingImpl(
    override var originalExpression: FirWhenSubjectExpression,
    override val smartcastType: FirTypeRef,
    override val typesFromSmartCast: Collection<ConeKotlinType>,
    override val smartcastStability: SmartcastStability,
    override val smartcastTypeWithoutNullableNothing: FirTypeRef
) : FirWhenSubjectExpressionWithSmartcastToNothing() {
    init {
        assert(originalExpression.typeRef is FirResolvedTypeRef)
    }

    override val source: KtSourceElement? get() = originalExpression.source
    override val annotations: List<FirAnnotation> get() = originalExpression.annotations
    override val whenRef: FirExpressionRef<FirWhenExpression> get() = originalExpression.whenRef
    override val originalType: FirTypeRef get() = originalExpression.typeRef
    override val isStable: Boolean get() = smartcastStability == SmartcastStability.STABLE_VALUE

    override val typeRef: FirTypeRef get() = if (isStable) smartcastType else originalExpression.typeRef

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirWhenSubjectExpressionWithSmartcastToNothingImpl {
        throw IllegalStateException()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        originalExpression.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirWhenSubjectExpressionWithSmartcastToNothingImpl {
        originalExpression = originalExpression.transform(transformer, data)
        return this
    }
}
