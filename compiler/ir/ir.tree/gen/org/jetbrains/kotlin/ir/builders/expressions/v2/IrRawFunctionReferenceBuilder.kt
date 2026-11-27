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
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class IrRawFunctionReferenceBuilder @PublishedApi internal constructor(
    override val symbol: IrFunctionSymbol,
) : IrDeclarationReferenceBuilder()

@PublishedApi
internal fun build(builder: IrRawFunctionReferenceBuilder): IrRawFunctionReference {
    val node = IrRawFunctionReferenceImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
    )
    return node
}

inline fun buildRawFunctionReference(symbol: IrFunctionSymbol, builder: IrRawFunctionReferenceBuilder.() -> Unit): IrRawFunctionReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrRawFunctionReferenceBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irRawFunctionReference(symbol: IrFunctionSymbol, builder: IrRawFunctionReferenceBuilder.() -> Unit): IrRawFunctionReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(rawFunctionReferenceBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.rawFunctionReferenceBuilder(symbol: IrFunctionSymbol): IrRawFunctionReferenceBuilder =
    IrRawFunctionReferenceBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
