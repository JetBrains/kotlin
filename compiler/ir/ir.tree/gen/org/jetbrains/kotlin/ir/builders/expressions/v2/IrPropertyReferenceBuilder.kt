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
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

class IrPropertyReferenceBuilder @PublishedApi internal constructor(
    override val symbol: IrPropertySymbol,
) : IrCallableReferenceBuilder<IrPropertySymbol>() {
    var field: IrFieldSymbol? = null

    var getter: IrSimpleFunctionSymbol? = null

    var setter: IrSimpleFunctionSymbol? = null
}

@PublishedApi
internal fun build(builder: IrPropertyReferenceBuilder): IrPropertyReference {
    val node = IrPropertyReferenceImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
        field = builder.field,
        getter = builder.getter,
        setter = builder.setter,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildPropertyReference(symbol: IrPropertySymbol, builder: IrPropertyReferenceBuilder.() -> Unit): IrPropertyReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrPropertyReferenceBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irPropertyReference(symbol: IrPropertySymbol, builder: IrPropertyReferenceBuilder.() -> Unit): IrPropertyReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(propertyReferenceBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.propertyReferenceBuilder(symbol: IrPropertySymbol): IrPropertyReferenceBuilder =
    IrPropertyReferenceBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
