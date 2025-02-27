/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrInlinedFunctionBlockImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    inlinedFunctionStartOffset: Int,
    inlinedFunctionEndOffset: Int,
    inlinedFunctionSymbol: IrFunctionSymbol?,
    inlinedFunctionFileEntry: IrFileEntry,
) : IrInlinedFunctionBlock() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override val statements: MutableList<IrStatement> by statementsAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override var inlinedFunctionStartOffset: Int by inlinedFunctionStartOffsetAttribute
    override var inlinedFunctionEndOffset: Int by inlinedFunctionEndOffsetAttribute
    override var inlinedFunctionSymbol: IrFunctionSymbol? by inlinedFunctionSymbolAttribute
    override var inlinedFunctionFileEntry: IrFileEntry by inlinedFunctionFileEntryAttribute

    init {
        preallocateStorage(9)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(inlinedFunctionStartOffsetAttribute, inlinedFunctionStartOffset)
        initAttribute(originAttribute, origin)
        initAttribute(inlinedFunctionEndOffsetAttribute, inlinedFunctionEndOffset)
        initAttribute(inlinedFunctionSymbolAttribute, inlinedFunctionSymbol)
        initAttribute(typeAttribute, type)
        initAttribute(inlinedFunctionFileEntryAttribute, inlinedFunctionFileEntry)
        initAttribute(statementsAttribute, ArrayList(2))
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrInlinedFunctionBlockImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrInlinedFunctionBlockImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrInlinedFunctionBlockImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrInlinedFunctionBlockImpl::class.java, 7, "type", null)
        @JvmStatic private val statementsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrStatement>>(IrInlinedFunctionBlockImpl::class.java, 9, "statements", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrInlinedFunctionBlockImpl::class.java, 4, "origin", null)
        @JvmStatic private val inlinedFunctionStartOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrInlinedFunctionBlockImpl::class.java, 3, "inlinedFunctionStartOffset", null)
        @JvmStatic private val inlinedFunctionEndOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrInlinedFunctionBlockImpl::class.java, 5, "inlinedFunctionEndOffset", null)
        @JvmStatic private val inlinedFunctionSymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFunctionSymbol?>(IrInlinedFunctionBlockImpl::class.java, 6, "inlinedFunctionSymbol", null)
        @JvmStatic private val inlinedFunctionFileEntryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFileEntry>(IrInlinedFunctionBlockImpl::class.java, 8, "inlinedFunctionFileEntry", null)
    }
}
