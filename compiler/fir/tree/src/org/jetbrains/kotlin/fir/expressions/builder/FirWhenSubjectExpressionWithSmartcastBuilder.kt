/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenSubjectExpressionWithSmartcastImpl

class FirWhenSubjectExpressionWithSmartcastBuilder : FirWrappedExpressionWithSmartcastBuilder<FirWhenSubjectExpression>() {
    fun build(): FirWhenSubjectExpressionWithSmartcast {
        return FirWhenSubjectExpressionWithSmartcastImpl(originalExpression, smartcastType, typesFromSmartCast, smartcastStability)
    }
}

fun buildWhenSubjectExpressionWithSmartcast(init: FirWhenSubjectExpressionWithSmartcastBuilder.() -> Unit): FirWhenSubjectExpressionWithSmartcast {
    return FirWhenSubjectExpressionWithSmartcastBuilder().apply(init).build()
}
