/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirSmartCastExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.types.SmartcastStability

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirSmartCastExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override lateinit var typeRef: FirTypeRef
    lateinit var originalExpression: FirExpression
    lateinit var typesFromSmartCast: Collection<ConeKotlinType>
    lateinit var smartcastType: FirTypeRef
    var smartcastTypeWithoutNullableNothing: FirTypeRef? = null
    lateinit var smartcastStability: SmartcastStability

    override fun build(): FirSmartCastExpression {
        return FirSmartCastExpressionImpl(
            source,
            annotations,
            typeRef,
            originalExpression,
            typesFromSmartCast,
            smartcastType,
            smartcastTypeWithoutNullableNothing,
            smartcastStability,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSmartCastExpression(init: FirSmartCastExpressionBuilder.() -> Unit): FirSmartCastExpression {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirSmartCastExpressionBuilder().apply(init).build()
}
