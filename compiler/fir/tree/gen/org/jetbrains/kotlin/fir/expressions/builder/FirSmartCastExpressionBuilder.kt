/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSmartCastExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.SmartcastStability

@FirBuilderDsl
class FirSmartCastExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var originalExpression: FirExpression
    lateinit var typesFromSmartCast: Collection<ConeKotlinType>
    lateinit var smartcastType: FirTypeRef
    var smartcastTypeWithoutNullableNothing: FirTypeRef? = null
    lateinit var smartcastStability: SmartcastStability

    override fun build(): FirSmartCastExpression {
        return FirSmartCastExpressionImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            originalExpression,
            typesFromSmartCast,
            smartcastType,
            smartcastTypeWithoutNullableNothing,
            smartcastStability,
        )
    }


    @Deprecated("Modification of 'source' has no impact for FirSmartCastExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var source: KtSourceElement?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildSmartCastExpression(init: FirSmartCastExpressionBuilder.() -> Unit): FirSmartCastExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirSmartCastExpressionBuilder().apply(init).build()
}
