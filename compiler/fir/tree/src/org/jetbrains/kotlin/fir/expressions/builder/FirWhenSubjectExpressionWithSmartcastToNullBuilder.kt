/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpressionWithSmartcastToNothing
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenSubjectExpressionWithSmartcastToNothingImpl

class FirWhenSubjectExpressionWithSmartcastToNothingBuilder : FirWrappedExpressionWithSmartcastToNothingBuilder<FirWhenSubjectExpression>() {
    override fun build(): FirWhenSubjectExpressionWithSmartcastToNothing {
        return FirWhenSubjectExpressionWithSmartcastToNothingImpl(
            originalExpression,
            smartcastType,
            typesFromSmartCast,
            smartcastStability,
            smartcastTypeWithoutNullableNothing
        )
    }
}

fun buildWhenSubjectExpressionWithSmartcastToNothing(init: FirWhenSubjectExpressionWithSmartcastToNothingBuilder.() -> Unit): FirWhenSubjectExpressionWithSmartcastToNothing {
    return FirWhenSubjectExpressionWithSmartcastToNothingBuilder().apply(init).build()
}
