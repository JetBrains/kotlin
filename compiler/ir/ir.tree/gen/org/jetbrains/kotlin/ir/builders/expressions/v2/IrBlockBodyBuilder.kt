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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

class IrBlockBodyBuilder @PublishedApi internal constructor() : IrBodyBuilder() {
    val statements: MutableList<IrStatement> = []
}

@PublishedApi
internal fun IrFactory.build(builder: IrBlockBodyBuilder): IrBlockBody {
    val node = createBlockBody(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
    )
    node.statements.addAll(builder.statements)
    return node
}

inline fun IrFactory.irBlockBody(builder: IrBlockBodyBuilder.() -> Unit = {}): IrBlockBody {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrBlockBodyBuilder().apply(builder))
}
