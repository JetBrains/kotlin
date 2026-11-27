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
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol

class IrAnnotationBuilder @PublishedApi internal constructor(
    symbol: IrConstructorSymbol,
) : IrConstructorCallBuilder(symbol)

@PublishedApi
internal fun build(builder: IrAnnotationBuilder): IrAnnotation {
    val node = IrAnnotationImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        source = builder.source,
        constructorTypeArgumentsCount = builder.constructorTypeArgumentsCount,
        symbol = builder.symbol,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildAnnotation(symbol: IrConstructorSymbol, builder: IrAnnotationBuilder.() -> Unit = {}): IrAnnotation {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrAnnotationBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irAnnotation(symbol: IrConstructorSymbol, builder: IrAnnotationBuilder.() -> Unit = {}): IrAnnotation {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(annotationBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.annotationBuilder(symbol: IrConstructorSymbol): IrAnnotationBuilder =
    IrAnnotationBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
