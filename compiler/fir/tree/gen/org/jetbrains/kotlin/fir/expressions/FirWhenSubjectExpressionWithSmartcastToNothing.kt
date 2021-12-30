/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirWhenSubjectExpressionWithSmartcastToNothing : FirWhenSubjectExpression(), FirWrappedExpressionWithSmartcastToNothing<FirWhenSubjectExpression> {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val whenRef: FirExpressionRef<FirWhenExpression>
    abstract override val originalExpression: FirWhenSubjectExpression
    abstract override val typesFromSmartCast: Collection<ConeKotlinType>
    abstract override val originalType: FirTypeRef
    abstract override val smartcastType: FirTypeRef
    abstract override val isStable: Boolean
    abstract override val smartcastStability: SmartcastStability
    abstract override val smartcastTypeWithoutNullableNothing: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWhenSubjectExpressionWithSmartcastToNothing(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformWhenSubjectExpressionWithSmartcastToNothing(this, data) as E

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirWhenSubjectExpressionWithSmartcastToNothing
}
