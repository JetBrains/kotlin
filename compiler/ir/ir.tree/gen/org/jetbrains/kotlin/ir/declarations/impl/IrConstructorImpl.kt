/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrConstructorImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    containerSource: DeserializedContainerSource?,
    isInline: Boolean,
    isExpect: Boolean,
    symbol: IrConstructorSymbol,
    isPrimary: Boolean,
) : IrConstructor() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var isExternal: Boolean by isExternalAttribute
    override var visibility: DescriptorVisibility by visibilityAttribute
    override var typeParameters: List<IrTypeParameter> by typeParametersAttribute
    override val containerSource: DeserializedContainerSource? by containerSourceAttribute
    override var metadata: MetadataSource? by metadataAttribute
    override var isInline: Boolean by isInlineAttribute
    override var isExpect: Boolean by isExpectAttribute
    override var returnType: IrType by returnTypeAttribute
    override var body: IrBody? by bodyAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor
        get() = symbol.descriptor

    override val symbol: IrConstructorSymbol by symbolAttribute
    override var isPrimary: Boolean by isPrimaryAttribute

    init {
        preallocateStorage(11)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(visibilityAttribute, visibility)
        initAttribute(containerSourceAttribute, containerSource)
        initAttribute(symbolAttribute, symbol)
        if (isExpect) setFlagInternal(isExpectAttribute, true)
        if (isPrimary) setFlagInternal(isPrimaryAttribute, true)
        if (isInline) setFlagInternal(isInlineAttribute, true)
        if (isExternal) setFlagInternal(isExternalAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstructorImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrConstructorImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrConstructorImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrConstructorImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrConstructorImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrConstructorImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrConstructorImpl::class.java, 6, "name", null)
        @JvmStatic private val isExternalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrConstructorImpl::class.java, 63, "isExternal")
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrConstructorImpl::class.java, 10, "visibility", null)
        @JvmStatic private val typeParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrTypeParameter>>(IrConstructorImpl::class.java, 7, "typeParameters", emptyList())
        @JvmStatic private val containerSourceAttribute = IrIndexBasedAttributeRegistry.createAttr<DeserializedContainerSource?>(IrConstructorImpl::class.java, 11, "containerSource", null)
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrConstructorImpl::class.java, 13, "metadata", null)
        @JvmStatic private val isInlineAttribute = IrIndexBasedAttributeRegistry.createFlag(IrConstructorImpl::class.java, 62, "isInline")
        @JvmStatic private val isExpectAttribute = IrIndexBasedAttributeRegistry.createFlag(IrConstructorImpl::class.java, 58, "isExpect")
        @JvmStatic private val returnTypeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrConstructorImpl::class.java, 15, "returnType", null)
        @JvmStatic private val bodyAttribute = IrIndexBasedAttributeRegistry.createAttr<IrBody?>(IrConstructorImpl::class.java, 9, "body", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrConstructorSymbol>(IrConstructorImpl::class.java, 12, "symbol", null)
        @JvmStatic private val isPrimaryAttribute = IrIndexBasedAttributeRegistry.createFlag(IrConstructorImpl::class.java, 61, "isPrimary")
    }
}
