/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcastToNull
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionWithSmartcastToNullImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability

class FirExpressionWithSmartcastToNullBuilder {
    lateinit var originalExpression: FirQualifiedAccessExpression
    lateinit var smartcastType: FirTypeRef
    lateinit var typesFromSmartCast: Collection<ConeKotlinType>
    lateinit var smartcastStability: SmartcastStability
    lateinit var smartcastTypeWithoutNullableNothing: FirTypeRef

    fun build(): FirExpressionWithSmartcastToNull {
        return FirExpressionWithSmartcastToNullImpl(
            originalExpression,
            smartcastType,
            typesFromSmartCast,
            smartcastStability,
            smartcastTypeWithoutNullableNothing,
            listOf()
        )
    }
}

inline fun buildExpressionWithSmartcastToNull(init: FirExpressionWithSmartcastToNullBuilder.() -> Unit): FirExpressionWithSmartcastToNull {
    return FirExpressionWithSmartcastToNullBuilder().apply(init).build()
}
