/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.name.Name

class IrEnumEntryImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    symbol: IrEnumEntrySymbol,
) : IrEnumEntry() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override val symbol: IrEnumEntrySymbol by symbolAttribute
    override var initializerExpression: IrExpressionBody? by initializerExpressionAttribute
    override var correspondingClass: IrClass? by correspondingClassAttribute

    init {
        preallocateStorage(8)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(symbolAttribute, symbol)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrEnumEntryImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrEnumEntryImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrEnumEntryImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrEnumEntryImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrEnumEntryImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrEnumEntryImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrEnumEntryImpl::class.java, 6, "name", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrEnumEntrySymbol>(IrEnumEntryImpl::class.java, 12, "symbol", null)
        @JvmStatic private val initializerExpressionAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpressionBody?>(IrEnumEntryImpl::class.java, 7, "initializerExpression", null)
        @JvmStatic private val correspondingClassAttribute = IrIndexBasedAttributeRegistry.createAttr<IrClass?>(IrEnumEntryImpl::class.java, 9, "correspondingClass", null)
    }
}
