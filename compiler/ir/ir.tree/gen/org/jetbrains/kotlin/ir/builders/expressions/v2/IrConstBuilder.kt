/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ir.builders.expressions.v2

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl

class IrConstBuilder @PublishedApi internal constructor(
    val kind: IrConstKind,
    val value: Any?,
) : IrExpressionBuilder()

@PublishedApi
internal fun build(builder: IrConstBuilder): IrConst {
    val node = IrConstImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        kind = builder.kind,
        value = builder.value,
    )
    return node
}
