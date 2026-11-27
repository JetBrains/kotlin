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
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrLocalDelegatedPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

class IrLocalDelegatedPropertyReferenceBuilder @PublishedApi internal constructor(
    override val symbol: IrLocalDelegatedPropertySymbol,
) : IrCallableReferenceBuilder<IrLocalDelegatedPropertySymbol>() {
    lateinit var delegate: IrVariableSymbol

    lateinit var getter: IrSimpleFunctionSymbol

    var setter: IrSimpleFunctionSymbol? = null
}

@PublishedApi
internal fun build(builder: IrLocalDelegatedPropertyReferenceBuilder): IrLocalDelegatedPropertyReference {
    val node = IrLocalDelegatedPropertyReferenceImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
        delegate = builder.delegate,
        getter = builder.getter,
        setter = builder.setter,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildLocalDelegatedPropertyReference(symbol: IrLocalDelegatedPropertySymbol, builder: IrLocalDelegatedPropertyReferenceBuilder.() -> Unit): IrLocalDelegatedPropertyReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrLocalDelegatedPropertyReferenceBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irLocalDelegatedPropertyReference(symbol: IrLocalDelegatedPropertySymbol, builder: IrLocalDelegatedPropertyReferenceBuilder.() -> Unit): IrLocalDelegatedPropertyReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(localDelegatedPropertyReferenceBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.localDelegatedPropertyReferenceBuilder(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertyReferenceBuilder =
    IrLocalDelegatedPropertyReferenceBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
