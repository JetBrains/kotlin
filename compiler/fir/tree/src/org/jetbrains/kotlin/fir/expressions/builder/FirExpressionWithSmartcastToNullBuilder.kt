/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcastToNothing
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionWithSmartcastToNothingImpl

class FirExpressionWithSmartcastToNothingBuilder : FirWrappedExpressionWithSmartcastToNothingBuilder<FirQualifiedAccessExpression>() {
    override fun build(): FirExpressionWithSmartcastToNothing {
        return FirExpressionWithSmartcastToNothingImpl(
            originalExpression,
            smartcastType,
            typesFromSmartCast,
            smartcastStability,
            smartcastTypeWithoutNullableNothing
        )
    }
}

inline fun buildExpressionWithSmartcastToNothing(init: FirExpressionWithSmartcastToNothingBuilder.() -> Unit): FirExpressionWithSmartcastToNothing {
    return FirExpressionWithSmartcastToNothingBuilder().apply(init).build()
}
