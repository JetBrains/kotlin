/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrTypeAliasImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    visibility: DescriptorVisibility,
    symbol: IrTypeAliasSymbol,
    isActual: Boolean,
    expandedType: IrType,
) : IrTypeAlias() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var visibility: DescriptorVisibility by visibilityAttribute
    override var typeParameters: List<IrTypeParameter> by typeParametersAttribute
    override var metadata: MetadataSource? by metadataAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: TypeAliasDescriptor
        get() = symbol.descriptor

    override val symbol: IrTypeAliasSymbol by symbolAttribute
    override var isActual: Boolean by isActualAttribute
    override var expandedType: IrType by expandedTypeAttribute

    init {
        preallocateStorage(10)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(expandedTypeAttribute, expandedType)
        initAttribute(visibilityAttribute, visibility)
        initAttribute(symbolAttribute, symbol)
        if (isActual) setFlagInternal(isActualAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeAliasImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeAliasImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrTypeAliasImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrTypeAliasImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrTypeAliasImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrTypeAliasImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrTypeAliasImpl::class.java, 6, "name", null)
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrTypeAliasImpl::class.java, 10, "visibility", null)
        @JvmStatic private val typeParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrTypeParameter>>(IrTypeAliasImpl::class.java, 7, "typeParameters", emptyList())
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrTypeAliasImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrTypeAliasSymbol>(IrTypeAliasImpl::class.java, 12, "symbol", null)
        @JvmStatic private val isActualAttribute = IrIndexBasedAttributeRegistry.createFlag(IrTypeAliasImpl::class.java, 63, "isActual")
        @JvmStatic private val expandedTypeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrTypeAliasImpl::class.java, 9, "expandedType", null)
    }
}
