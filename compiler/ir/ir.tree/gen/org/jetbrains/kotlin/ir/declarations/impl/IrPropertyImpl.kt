/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrPropertyImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
    isFakeOverride: Boolean,
    containerSource: DeserializedContainerSource?,
    symbol: IrPropertySymbol,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
) : IrProperty() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var isExternal: Boolean by isExternalAttribute
    override var visibility: DescriptorVisibility by visibilityAttribute
    override var modality: Modality by modalityAttribute
    override var isFakeOverride: Boolean by isFakeOverrideAttribute
    override var metadata: MetadataSource? by metadataAttribute
    override val containerSource: DeserializedContainerSource? by containerSourceAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override val symbol: IrPropertySymbol by symbolAttribute
    override var overriddenSymbols: List<IrPropertySymbol> by overriddenSymbolsAttribute
    override var isVar: Boolean by isVarAttribute
    override var isConst: Boolean by isConstAttribute
    override var isLateinit: Boolean by isLateinitAttribute
    override var isDelegated: Boolean by isDelegatedAttribute
    override var isExpect: Boolean by isExpectAttribute
    override var backingField: IrField? by backingFieldAttribute
    override var getter: IrSimpleFunction? by getterAttribute
    override var setter: IrSimpleFunction? by setterAttribute

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
        initAttribute(modalityAttribute, modality)
        if (isDelegated) setFlagInternal(isDelegatedAttribute, true)
        if (isExpect) setFlagInternal(isExpectAttribute, true)
        if (isLateinit) setFlagInternal(isLateinitAttribute, true)
        if (isConst) setFlagInternal(isConstAttribute, true)
        if (isFakeOverride) setFlagInternal(isFakeOverrideAttribute, true)
        if (isVar) setFlagInternal(isVarAttribute, true)
        if (isExternal) setFlagInternal(isExternalAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrPropertyImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrPropertyImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrPropertyImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrPropertyImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrPropertyImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrPropertyImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrPropertyImpl::class.java, 6, "name", null)
        @JvmStatic private val isExternalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 63, "isExternal")
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrPropertyImpl::class.java, 10, "visibility", null)
        @JvmStatic private val modalityAttribute = IrIndexBasedAttributeRegistry.createAttr<Modality>(IrPropertyImpl::class.java, 14, "modality", null)
        @JvmStatic private val isFakeOverrideAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 61, "isFakeOverride")
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrPropertyImpl::class.java, 13, "metadata", null)
        @JvmStatic private val containerSourceAttribute = IrIndexBasedAttributeRegistry.createAttr<DeserializedContainerSource?>(IrPropertyImpl::class.java, 11, "containerSource", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrPropertySymbol>(IrPropertyImpl::class.java, 12, "symbol", null)
        @JvmStatic private val overriddenSymbolsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrPropertySymbol>>(IrPropertyImpl::class.java, 16, "overriddenSymbols", emptyList())
        @JvmStatic private val isVarAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 62, "isVar")
        @JvmStatic private val isConstAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 60, "isConst")
        @JvmStatic private val isLateinitAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 59, "isLateinit")
        @JvmStatic private val isDelegatedAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 57, "isDelegated")
        @JvmStatic private val isExpectAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyImpl::class.java, 58, "isExpect")
        @JvmStatic private val backingFieldAttribute = IrIndexBasedAttributeRegistry.createAttr<IrField?>(IrPropertyImpl::class.java, 7, "backingField", null)
        @JvmStatic private val getterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction?>(IrPropertyImpl::class.java, 9, "getter", null)
        @JvmStatic private val setterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction?>(IrPropertyImpl::class.java, 15, "setter", null)
    }
}
