/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrSyntheticBodyImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var kind: IrSyntheticBodyKind,
) : IrSyntheticBody()

fun IrSyntheticBodyImpl(
    startOffset: Int,
    endOffset: Int,
    kind: IrSyntheticBodyKind,
) = IrSyntheticBodyImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    kind = kind,
)
