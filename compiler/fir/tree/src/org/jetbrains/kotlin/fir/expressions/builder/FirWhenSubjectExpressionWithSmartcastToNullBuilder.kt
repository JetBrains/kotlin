/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpressionWithSmartcastToNull
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenSubjectExpressionWithSmartcastToNullImpl

class FirWhenSubjectExpressionWithSmartcastToNullBuilder : FirWrappedExpressionWithSmartcastToNullBuilder<FirWhenSubjectExpression>() {
    fun build(): FirWhenSubjectExpressionWithSmartcastToNull {
        return FirWhenSubjectExpressionWithSmartcastToNullImpl(
            originalExpression,
            smartcastType,
            typesFromSmartCast,
            smartcastStability,
            smartcastTypeWithoutNullableNothing
        )
    }
}

fun buildWhenSubjectExpressionWithSmartcastToNull(init: FirWhenSubjectExpressionWithSmartcastToNullBuilder.() -> Unit): FirWhenSubjectExpressionWithSmartcastToNull {
    return FirWhenSubjectExpressionWithSmartcastToNullBuilder().apply(init).build()
}
