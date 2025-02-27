/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrLocalDelegatedPropertyImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    symbol: IrLocalDelegatedPropertySymbol,
    type: IrType,
    isVar: Boolean,
) : IrLocalDelegatedProperty() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var metadata: MetadataSource? by metadataAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptorWithAccessors
        get() = symbol.descriptor

    override val symbol: IrLocalDelegatedPropertySymbol by symbolAttribute
    override var type: IrType by typeAttribute
    override var isVar: Boolean by isVarAttribute
    override var delegate: IrVariable by delegateAttribute
    override var getter: IrSimpleFunction by getterAttribute
    override var setter: IrSimpleFunction? by setterAttribute

    init {
        preallocateStorage(11)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(typeAttribute, type)
        initAttribute(symbolAttribute, symbol)
        if (isVar) setFlagInternal(isVarAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrLocalDelegatedPropertyImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrLocalDelegatedPropertyImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrLocalDelegatedPropertyImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrLocalDelegatedPropertyImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrLocalDelegatedPropertyImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrLocalDelegatedPropertyImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrLocalDelegatedPropertyImpl::class.java, 6, "name", null)
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrLocalDelegatedPropertyImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrLocalDelegatedPropertySymbol>(IrLocalDelegatedPropertyImpl::class.java, 12, "symbol", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrLocalDelegatedPropertyImpl::class.java, 7, "type", null)
        @JvmStatic private val isVarAttribute = IrIndexBasedAttributeRegistry.createFlag(IrLocalDelegatedPropertyImpl::class.java, 62, "isVar")
        @JvmStatic private val delegateAttribute = IrIndexBasedAttributeRegistry.createAttr<IrVariable>(IrLocalDelegatedPropertyImpl::class.java, 10, "delegate", null)
        @JvmStatic private val getterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction>(IrLocalDelegatedPropertyImpl::class.java, 9, "getter", null)
        @JvmStatic private val setterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction?>(IrLocalDelegatedPropertyImpl::class.java, 15, "setter", null)
    }
}
