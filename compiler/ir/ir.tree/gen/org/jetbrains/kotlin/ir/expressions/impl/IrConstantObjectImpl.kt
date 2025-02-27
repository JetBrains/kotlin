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
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstantValue
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.utils.SmartList

class IrConstantObjectImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    constructor: IrConstructorSymbol,
) : IrConstantObject() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var constructor: IrConstructorSymbol by constructorAttribute
    override val valueArguments: MutableList<IrConstantValue> by valueArgumentsAttribute
    override val typeArguments: MutableList<IrType> by typeArgumentsAttribute

    init {
        preallocateStorage(6)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(typeArgumentsAttribute, SmartList())
        initAttribute(valueArgumentsAttribute, SmartList())
        initAttribute(typeAttribute, type)
        initAttribute(constructorAttribute, constructor)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstantObjectImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstantObjectImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrConstantObjectImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrConstantObjectImpl::class.java, 7, "type", null)
        @JvmStatic private val constructorAttribute = IrIndexBasedAttributeRegistry.createAttr<IrConstructorSymbol>(IrConstantObjectImpl::class.java, 22, "constructor", null)
        @JvmStatic private val valueArgumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrConstantValue>>(IrConstantObjectImpl::class.java, 4, "valueArguments", null)
        @JvmStatic private val typeArgumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrType>>(IrConstantObjectImpl::class.java, 3, "typeArguments", null)
    }
}
