/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.IrType

class IrReturnableBlockImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrReturnableBlockSymbol,
    override var origin: IrStatementOrigin? = null,
) : IrReturnableBlock() {
    override val statements: MutableList<IrStatement> = ArrayList(2)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        symbol: IrReturnableBlockSymbol,
        origin: IrStatementOrigin?,
        statements: List<IrStatement>,
    ) : this(startOffset, endOffset, type, symbol, origin) {
        this.statements.addAll(statements)
    }

    init {
        symbol.bind(this)
    }
}