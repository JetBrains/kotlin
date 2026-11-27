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
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.IrSyntheticBodyImpl

class IrSyntheticBodyBuilder @PublishedApi internal constructor() : IrBodyBuilder() {
    lateinit var kind: IrSyntheticBodyKind
}

@PublishedApi
internal fun build(builder: IrSyntheticBodyBuilder): IrSyntheticBody {
    val node = IrSyntheticBodyImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        kind = builder.kind,
    )
    return node
}

inline fun buildSyntheticBody(builder: IrSyntheticBodyBuilder.() -> Unit): IrSyntheticBody {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSyntheticBodyBuilder().apply(builder))
}
