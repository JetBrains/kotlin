/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrGetFieldImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFieldSymbol,
    superQualifierSymbol: IrClassSymbol?,
    origin: IrStatementOrigin?,
) : IrGetField() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var symbol: IrFieldSymbol by symbolAttribute
    override var superQualifierSymbol: IrClassSymbol? by superQualifierSymbolAttribute
    override var receiver: IrExpression? by receiverAttribute
    override var origin: IrStatementOrigin? by originAttribute

    init {
        preallocateStorage(6)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(superQualifierSymbolAttribute, superQualifierSymbol)
        initAttribute(typeAttribute, type)
        initAttribute(symbolAttribute, symbol)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrGetFieldImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrGetFieldImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrGetFieldImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrGetFieldImpl::class.java, 7, "type", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFieldSymbol>(IrGetFieldImpl::class.java, 12, "symbol", null)
        @JvmStatic private val superQualifierSymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrClassSymbol?>(IrGetFieldImpl::class.java, 5, "superQualifierSymbol", null)
        @JvmStatic private val receiverAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression?>(IrGetFieldImpl::class.java, 6, "receiver", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrGetFieldImpl::class.java, 4, "origin", null)
    }
}
