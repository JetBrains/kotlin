/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability

abstract class FirWrappedExpressionWithSmartcastBuilder<E : FirExpression> {
    lateinit var originalExpression: E
    lateinit var smartcastType: FirTypeRef
    lateinit var typesFromSmartCast: Collection<ConeKotlinType>
    lateinit var smartcastStability: SmartcastStability

    abstract fun build(): E
}

abstract class FirWrappedExpressionWithSmartcastToNothingBuilder<E : FirExpression> : FirWrappedExpressionWithSmartcastBuilder<E>() {
    lateinit var smartcastTypeWithoutNullableNothing: FirTypeRef
}
