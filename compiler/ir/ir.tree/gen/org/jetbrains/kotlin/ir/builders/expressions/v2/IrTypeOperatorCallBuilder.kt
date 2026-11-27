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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType

class IrTypeOperatorCallBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var operator: IrTypeOperator

    lateinit var argument: IrExpression

    lateinit var typeOperand: IrType
}

@PublishedApi
internal fun build(builder: IrTypeOperatorCallBuilder): IrTypeOperatorCall {
    val node = IrTypeOperatorCallImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        operator = builder.operator,
        argument = builder.argument,
        typeOperand = builder.typeOperand,
    )
    return node
}

inline fun buildTypeOperatorCall(builder: IrTypeOperatorCallBuilder.() -> Unit): IrTypeOperatorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrTypeOperatorCallBuilder().apply(builder))
}

inline fun IrBuilder.irTypeOperatorCall(builder: IrTypeOperatorCallBuilder.() -> Unit): IrTypeOperatorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(typeOperatorCallBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.typeOperatorCallBuilder(): IrTypeOperatorCallBuilder =
    IrTypeOperatorCallBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
