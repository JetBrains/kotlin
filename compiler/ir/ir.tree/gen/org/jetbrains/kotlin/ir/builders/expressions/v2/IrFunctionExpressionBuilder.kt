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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl

class IrFunctionExpressionBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var origin: IrStatementOrigin

    lateinit var function: IrSimpleFunction
}

@PublishedApi
internal fun build(builder: IrFunctionExpressionBuilder): IrFunctionExpression {
    val node = IrFunctionExpressionImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        function = builder.function,
    )
    return node
}

inline fun buildFunctionExpression(builder: IrFunctionExpressionBuilder.() -> Unit): IrFunctionExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrFunctionExpressionBuilder().apply(builder))
}

inline fun IrBuilder.irFunctionExpression(builder: IrFunctionExpressionBuilder.() -> Unit): IrFunctionExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(functionExpressionBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.functionExpressionBuilder(): IrFunctionExpressionBuilder =
    IrFunctionExpressionBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
