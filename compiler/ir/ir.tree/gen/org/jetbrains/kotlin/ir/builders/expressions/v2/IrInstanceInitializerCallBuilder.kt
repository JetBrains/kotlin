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
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol

class IrInstanceInitializerCallBuilder @PublishedApi internal constructor(
    val classSymbol: IrClassSymbol,
) : IrExpressionBuilder()

@PublishedApi
internal fun build(builder: IrInstanceInitializerCallBuilder): IrInstanceInitializerCall {
    val node = IrInstanceInitializerCallImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        classSymbol = builder.classSymbol,
    )
    return node
}

inline fun buildInstanceInitializerCall(classSymbol: IrClassSymbol, builder: IrInstanceInitializerCallBuilder.() -> Unit): IrInstanceInitializerCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrInstanceInitializerCallBuilder(classSymbol).apply(builder))
}

inline fun IrBuilder.irInstanceInitializerCall(classSymbol: IrClassSymbol, builder: IrInstanceInitializerCallBuilder.() -> Unit = {}): IrInstanceInitializerCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(instanceInitializerCallBuilder(classSymbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.instanceInitializerCallBuilder(classSymbol: IrClassSymbol): IrInstanceInitializerCallBuilder =
    IrInstanceInitializerCallBuilder(classSymbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.unitType
    }
