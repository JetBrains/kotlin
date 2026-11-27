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
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl

class IrCompositeBuilder @PublishedApi internal constructor() : IrContainerExpressionBuilder()

@PublishedApi
internal fun build(builder: IrCompositeBuilder): IrComposite {
    val node = IrCompositeImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
    )
    node.statements.addAll(builder.statements)
    return node
}

inline fun buildComposite(builder: IrCompositeBuilder.() -> Unit): IrComposite {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrCompositeBuilder().apply(builder))
}
