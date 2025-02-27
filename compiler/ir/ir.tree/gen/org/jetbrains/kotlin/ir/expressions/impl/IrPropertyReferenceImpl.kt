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
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrPropertyReferenceImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    symbol: IrPropertySymbol,
    field: IrFieldSymbol?,
    getter: IrSimpleFunctionSymbol?,
    setter: IrSimpleFunctionSymbol?,
) : IrPropertyReference() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override val typeArguments: MutableList<IrType?> by typeArgumentsAttribute
    override var symbol: IrPropertySymbol by symbolAttribute
    override var field: IrFieldSymbol? by fieldAttribute
    override var getter: IrSimpleFunctionSymbol? by getterAttribute
    override var setter: IrSimpleFunctionSymbol? by setterAttribute

    init {
        preallocateStorage(10)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(typeArgumentsAttribute, ArrayList(0))
        initAttribute(originAttribute, origin)
        initAttribute(fieldAttribute, field)
        initAttribute(typeAttribute, type)
        initAttribute(getterAttribute, getter)
        initAttribute(symbolAttribute, symbol)
        initAttribute(setterAttribute, setter)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrPropertyReferenceImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrPropertyReferenceImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrPropertyReferenceImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrPropertyReferenceImpl::class.java, 7, "type", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrPropertyReferenceImpl::class.java, 4, "origin", null)
        @JvmStatic private val typeArgumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrType?>>(IrPropertyReferenceImpl::class.java, 3, "typeArguments", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrPropertySymbol>(IrPropertyReferenceImpl::class.java, 12, "symbol", null)
        @JvmStatic private val fieldAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFieldSymbol?>(IrPropertyReferenceImpl::class.java, 5, "field", null)
        @JvmStatic private val getterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunctionSymbol?>(IrPropertyReferenceImpl::class.java, 9, "getter", null)
        @JvmStatic private val setterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunctionSymbol?>(IrPropertyReferenceImpl::class.java, 15, "setter", null)
    }
}
