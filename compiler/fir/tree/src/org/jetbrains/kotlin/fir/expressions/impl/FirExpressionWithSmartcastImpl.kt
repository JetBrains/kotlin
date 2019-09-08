/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirExpressionWithSmartcastImpl(
    override var originalExpression: FirQualifiedAccessExpression,
    typesFromSmartcast: Collection<ConeKotlinType>
) : FirExpressionWithSmartcast(originalExpression, typesFromSmartcast) {
    init {
        assert(originalExpression.typeRef is FirResolvedTypeRef)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        originalExpression = originalExpression.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        throw IllegalStateException()
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        throw IllegalStateException()
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        throw IllegalStateException()
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirQualifiedAccess {
        throw IllegalStateException()
    }
}