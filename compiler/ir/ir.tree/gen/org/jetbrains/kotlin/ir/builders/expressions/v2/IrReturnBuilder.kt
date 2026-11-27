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
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol

class IrReturnBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var value: IrExpression

    lateinit var returnTargetSymbol: IrReturnTargetSymbol
}

@PublishedApi
internal fun build(builder: IrReturnBuilder): IrReturn {
    val node = IrReturnImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        value = builder.value,
        returnTargetSymbol = builder.returnTargetSymbol,
    )
    return node
}

inline fun buildReturn(builder: IrReturnBuilder.() -> Unit): IrReturn {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrReturnBuilder().apply(builder))
}

inline fun IrBuilderWithScope.irReturn(builder: IrReturnBuilder.() -> Unit): IrReturn {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(returnBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilderWithScope.returnBuilder(): IrReturnBuilder =
    IrReturnBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.nothingType
        setReturnTargetFromScope(it)
    }
