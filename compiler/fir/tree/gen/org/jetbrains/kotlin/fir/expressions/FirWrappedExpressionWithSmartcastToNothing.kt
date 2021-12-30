/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirWrappedExpressionWithSmartcastToNothing<E : FirExpression> : FirWrappedExpressionWithSmartcast<E> {
    override val source: KtSourceElement?
    override val typeRef: FirTypeRef
    override val originalExpression: E
    override val typesFromSmartCast: Collection<ConeKotlinType>
    override val originalType: FirTypeRef
    override val smartcastType: FirTypeRef
    override val isStable: Boolean
    override val smartcastStability: SmartcastStability
    val smartcastTypeWithoutNullableNothing: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitWrappedExpressionWithSmartcastToNothing(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformWrappedExpressionWithSmartcastToNothing(this, data) as E

    override fun replaceTypeRef(newTypeRef: FirTypeRef)
}
