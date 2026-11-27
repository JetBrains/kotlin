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
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol

class IrSetFieldBuilder @PublishedApi internal constructor(
    override val symbol: IrFieldSymbol,
) : IrFieldAccessExpressionBuilder() {
    lateinit var value: IrExpression
}

@PublishedApi
internal fun build(builder: IrSetFieldBuilder): IrSetField {
    val node = IrSetFieldImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
        superQualifierSymbol = builder.superQualifierSymbol,
        origin = builder.origin,
    )
    node.receiver = builder.receiver
    node.value = builder.value
    return node
}

inline fun buildSetField(symbol: IrFieldSymbol, builder: IrSetFieldBuilder.() -> Unit): IrSetField {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSetFieldBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irSetField(symbol: IrFieldSymbol, builder: IrSetFieldBuilder.() -> Unit): IrSetField {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(setFieldBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.setFieldBuilder(symbol: IrFieldSymbol): IrSetFieldBuilder =
    IrSetFieldBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.unitType
    }
