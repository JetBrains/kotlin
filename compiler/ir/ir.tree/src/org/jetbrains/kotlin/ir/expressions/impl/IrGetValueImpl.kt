/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrGetValueImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var symbol: IrValueSymbol,
    override var origin: IrStatementOrigin?,
) : IrGetValue() {
    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}

fun IrGetValue.copyWithOffsets(newStartOffset: Int, newEndOffset: Int): IrGetValue =
    IrGetValueImpl(newStartOffset, newEndOffset, type, symbol, origin)

fun IrGetValueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrValueSymbol,
    origin: IrStatementOrigin? = null,
) = IrGetValueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
)

fun IrGetValueImpl(
    startOffset: Int,
    endOffset: Int,
    symbol: IrValueSymbol,
    origin: IrStatementOrigin? = null,
) = IrGetValueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = symbol.owner.type,
    symbol = symbol,
    origin = origin,
)
