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
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol

/**
 * The type arguments are the ones of the constructed class followed by the ones of the constructor.
 */
open class IrConstructorCallBuilder @PublishedApi internal constructor(
    override val symbol: IrConstructorSymbol,
) : IrFunctionAccessExpressionBuilder() {
    var source: SourceElement = SourceElement.NO_SOURCE

    var constructorTypeArgumentsCount: Int = 0

    init {
        type = symbol.owner.returnType
        initializeShape(this)
    }
}

@PublishedApi
internal fun build(builder: IrConstructorCallBuilder): IrConstructorCall {
    val node = IrConstructorCallImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
        source = builder.source,
        constructorTypeArgumentsCount = builder.constructorTypeArgumentsCount,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildConstructorCall(symbol: IrConstructorSymbol, builder: IrConstructorCallBuilder.() -> Unit = {}): IrConstructorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrConstructorCallBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irConstructorCall(symbol: IrConstructorSymbol, builder: IrConstructorCallBuilder.() -> Unit = {}): IrConstructorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(constructorCallBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.constructorCallBuilder(symbol: IrConstructorSymbol): IrConstructorCallBuilder =
    IrConstructorCallBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
