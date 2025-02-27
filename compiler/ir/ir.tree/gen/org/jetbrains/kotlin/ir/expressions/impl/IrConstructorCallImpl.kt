/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.ir.util.parentAsClass

class IrConstructorCallImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    symbol: IrConstructorSymbol,
    source: SourceElement,
    constructorTypeArgumentsCount: Int,
) : IrConstructorCall() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override val typeArguments: MutableList<IrType?> by typeArgumentsAttribute
    override var symbol: IrConstructorSymbol by symbolAttribute
    override var source: SourceElement by sourceAttribute
    override var constructorTypeArgumentsCount: Int by constructorTypeArgumentsCountAttribute

    init {
        preallocateStorage(9)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(typeArgumentsAttribute, ArrayList(0))
        initAttribute(originAttribute, origin)
        initAttribute(constructorTypeArgumentsCountAttribute, constructorTypeArgumentsCount)
        initAttribute(typeAttribute, type)
        initAttribute(symbolAttribute, symbol)
        initAttribute(sourceAttribute, source)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstructorCallImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstructorCallImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrConstructorCallImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrConstructorCallImpl::class.java, 7, "type", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrConstructorCallImpl::class.java, 4, "origin", null)
        @JvmStatic private val typeArgumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrType?>>(IrConstructorCallImpl::class.java, 3, "typeArguments", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrConstructorSymbol>(IrConstructorCallImpl::class.java, 12, "symbol", null)
        @JvmStatic private val sourceAttribute = IrIndexBasedAttributeRegistry.createAttr<SourceElement>(IrConstructorCallImpl::class.java, 15, "source", null)
        @JvmStatic private val constructorTypeArgumentsCountAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstructorCallImpl::class.java, 5, "constructorTypeArgumentsCount", null)
    }
}
