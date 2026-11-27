/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass
@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.expressions.v2

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl

open class IrErrorExpressionBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var description: String
}

@PublishedApi
internal fun build(builder: IrErrorExpressionBuilder): IrErrorExpression {
    val node = IrErrorExpressionImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        description = builder.description,
    )
    return node
}

inline fun buildErrorExpression(builder: IrErrorExpressionBuilder.() -> Unit): IrErrorExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrErrorExpressionBuilder().apply(builder))
}

inline fun IrBuilder.irErrorExpression(builder: IrErrorExpressionBuilder.() -> Unit): IrErrorExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(errorExpressionBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.errorExpressionBuilder(): IrErrorExpressionBuilder =
    IrErrorExpressionBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
