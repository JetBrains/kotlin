/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrFieldImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    symbol: IrFieldSymbol,
    type: IrType,
    isFinal: Boolean,
    isStatic: Boolean,
) : IrField() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var isExternal: Boolean by isExternalAttribute
    override var visibility: DescriptorVisibility by visibilityAttribute
    override var metadata: MetadataSource? by metadataAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override val symbol: IrFieldSymbol by symbolAttribute
    override var type: IrType by typeAttribute
    override var isFinal: Boolean by isFinalAttribute
    override var isStatic: Boolean by isStaticAttribute
    override var initializer: IrExpressionBody? by initializerAttribute
    override var correspondingPropertySymbol: IrPropertySymbol? by correspondingPropertySymbolAttribute

    init {
        preallocateStorage(10)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(typeAttribute, type)
        initAttribute(visibilityAttribute, visibility)
        initAttribute(symbolAttribute, symbol)
        if (isFinal) setFlagInternal(isFinalAttribute, true)
        if (isStatic) setFlagInternal(isStaticAttribute, true)
        if (isExternal) setFlagInternal(isExternalAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrFieldImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrFieldImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrFieldImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrFieldImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrFieldImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrFieldImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrFieldImpl::class.java, 6, "name", null)
        @JvmStatic private val isExternalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFieldImpl::class.java, 63, "isExternal")
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrFieldImpl::class.java, 10, "visibility", null)
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrFieldImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFieldSymbol>(IrFieldImpl::class.java, 12, "symbol", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrFieldImpl::class.java, 7, "type", null)
        @JvmStatic private val isFinalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFieldImpl::class.java, 61, "isFinal")
        @JvmStatic private val isStaticAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFieldImpl::class.java, 62, "isStatic")
        @JvmStatic private val initializerAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpressionBody?>(IrFieldImpl::class.java, 9, "initializer", null)
        @JvmStatic private val correspondingPropertySymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrPropertySymbol?>(IrFieldImpl::class.java, 17, "correspondingPropertySymbol", null)
    }
}
