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
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirVarargArgumentsExpressionImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType

@FirBuilderDsl
class FirVarargArgumentsExpressionBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    val arguments: MutableList<FirExpression> = mutableListOf()
    var coneElementTypeOrNull: ConeKotlinType? = null

    override fun build(): FirVarargArgumentsExpression {
        return FirVarargArgumentsExpressionImpl(
            source,
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            arguments,
            coneElementTypeOrNull,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVarargArgumentsExpression(init: FirVarargArgumentsExpressionBuilder.() -> Unit = {}): FirVarargArgumentsExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirVarargArgumentsExpressionBuilder().apply(init).build()
}
