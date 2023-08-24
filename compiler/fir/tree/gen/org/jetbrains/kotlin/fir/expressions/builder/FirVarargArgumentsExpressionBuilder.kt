/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirVarargArgumentsExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirVarargArgumentsExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    val arguments: MutableList<FirExpression> = mutableListOf()
    lateinit var varargElementType: FirTypeRef

    override fun build(): FirVarargArgumentsExpression {
        return FirVarargArgumentsExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            arguments,
            varargElementType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVarargArgumentsExpression(init: FirVarargArgumentsExpressionBuilder.() -> Unit): FirVarargArgumentsExpression {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirVarargArgumentsExpressionBuilder().apply(init).build()
}
