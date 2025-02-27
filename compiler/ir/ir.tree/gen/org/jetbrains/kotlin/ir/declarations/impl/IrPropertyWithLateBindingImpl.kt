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
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrPropertyWithLateBindingImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    modality: Modality,
    isFakeOverride: Boolean,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
) : IrPropertyWithLateBinding() {
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
    override val containerSource: DeserializedContainerSource?
        get() = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    override var overriddenSymbols: List<IrPropertySymbol> by overriddenSymbolsAttribute
    override var isVar: Boolean by isVarAttribute
    override var isConst: Boolean by isConstAttribute
    override var isLateinit: Boolean by isLateinitAttribute
    override var isDelegated: Boolean by isDelegatedAttribute
    override var isExpect: Boolean by isExpectAttribute
    override var backingField: IrField? by backingFieldAttribute
    override var getter: IrSimpleFunction? by getterAttribute
    override var setter: IrSimpleFunction? by setterAttribute
    override val isBound: Boolean
        get() = _symbol != null


    init {
        preallocateStorage(11)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(visibilityAttribute, visibility)
        initAttribute(modalityAttribute, modality)
        if (isDelegated) setFlagInternal(isDelegatedAttribute, true)
        if (isExpect) setFlagInternal(isExpectAttribute, true)
        if (isLateinit) setFlagInternal(isLateinitAttribute, true)
        if (isConst) setFlagInternal(isConstAttribute, true)
        if (isFakeOverride) setFlagInternal(isFakeOverrideAttribute, true)
        if (isVar) setFlagInternal(isVarAttribute, true)
        if (isExternal) setFlagInternal(isExternalAttribute, true)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrPropertyWithLateBindingImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrPropertyWithLateBindingImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrPropertyWithLateBindingImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrPropertyWithLateBindingImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrPropertyWithLateBindingImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrPropertyWithLateBindingImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrPropertyWithLateBindingImpl::class.java, 6, "name", null)
        @JvmStatic private val isExternalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 63, "isExternal")
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrPropertyWithLateBindingImpl::class.java, 10, "visibility", null)
        @JvmStatic private val modalityAttribute = IrIndexBasedAttributeRegistry.createAttr<Modality>(IrPropertyWithLateBindingImpl::class.java, 14, "modality", null)
        @JvmStatic private val isFakeOverrideAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 61, "isFakeOverride")
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrPropertyWithLateBindingImpl::class.java, 13, "metadata", null)
        @JvmStatic private val overriddenSymbolsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrPropertySymbol>>(IrPropertyWithLateBindingImpl::class.java, 16, "overriddenSymbols", emptyList())
        @JvmStatic private val isVarAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 62, "isVar")
        @JvmStatic private val isConstAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 60, "isConst")
        @JvmStatic private val isLateinitAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 59, "isLateinit")
        @JvmStatic private val isDelegatedAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 57, "isDelegated")
        @JvmStatic private val isExpectAttribute = IrIndexBasedAttributeRegistry.createFlag(IrPropertyWithLateBindingImpl::class.java, 58, "isExpect")
        @JvmStatic private val backingFieldAttribute = IrIndexBasedAttributeRegistry.createAttr<IrField?>(IrPropertyWithLateBindingImpl::class.java, 7, "backingField", null)
        @JvmStatic private val getterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction?>(IrPropertyWithLateBindingImpl::class.java, 9, "getter", null)
        @JvmStatic private val setterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction?>(IrPropertyWithLateBindingImpl::class.java, 15, "setter", null)
    }

    private var _symbol: IrPropertySymbol? = null

    override fun acquireSymbol(symbol: IrPropertySymbol): IrPropertyWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }
}
