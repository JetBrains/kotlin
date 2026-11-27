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
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class IrFunctionReferenceBuilder @PublishedApi internal constructor(
    override val symbol: IrFunctionSymbol,
) : IrCallableReferenceBuilder<IrFunctionSymbol>() {
    var reflectionTarget: IrFunctionSymbol? = symbol

    init {
        initializeShape(this)
    }
}

@PublishedApi
internal fun build(builder: IrFunctionReferenceBuilder): IrFunctionReference {
    val node = IrFunctionReferenceImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
        reflectionTarget = builder.reflectionTarget,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildFunctionReference(symbol: IrFunctionSymbol, builder: IrFunctionReferenceBuilder.() -> Unit): IrFunctionReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrFunctionReferenceBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irFunctionReference(symbol: IrFunctionSymbol, builder: IrFunctionReferenceBuilder.() -> Unit): IrFunctionReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(functionReferenceBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.functionReferenceBuilder(symbol: IrFunctionSymbol): IrFunctionReferenceBuilder =
    IrFunctionReferenceBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
