/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcastToNull
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionWithSmartcastToNullImpl

class FirExpressionWithSmartcastToNullBuilder : FirWrappedExpressionWithSmartcastToNullBuilder<FirQualifiedAccessExpression>() {
    fun build(): FirExpressionWithSmartcastToNull {
        return FirExpressionWithSmartcastToNullImpl(
            originalExpression,
            smartcastType,
            typesFromSmartCast,
            smartcastStability,
            smartcastTypeWithoutNullableNothing
        )
    }
}

inline fun buildExpressionWithSmartcastToNull(init: FirExpressionWithSmartcastToNullBuilder.() -> Unit): FirExpressionWithSmartcastToNull {
    return FirExpressionWithSmartcastToNullBuilder().apply(init).build()
}
