/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrFunctionWithLateBindingImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    isExternal: Boolean,
    visibility: DescriptorVisibility,
    isInline: Boolean,
    isExpect: Boolean,
    modality: Modality,
    isFakeOverride: Boolean,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
) : IrFunctionWithLateBinding() {
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
    override val containerSource: DeserializedContainerSource?
        get() = null

    override var metadata: MetadataSource? by metadataAttribute
    override var isInline: Boolean by isInlineAttribute
    override var isExpect: Boolean by isExpectAttribute
    override var returnType: IrType by returnTypeAttribute
    override var body: IrBody? by bodyAttribute
    override var modality: Modality by modalityAttribute
    override var isFakeOverride: Boolean by isFakeOverrideAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    override val symbol: IrSimpleFunctionSymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by overriddenSymbolsAttribute
    override var isTailrec: Boolean by isTailrecAttribute
    override var isSuspend: Boolean by isSuspendAttribute
    override var isOperator: Boolean by isOperatorAttribute
    override var isInfix: Boolean by isInfixAttribute
    override var correspondingPropertySymbol: IrPropertySymbol? by correspondingPropertySymbolAttribute
    override val isBound: Boolean
        get() = _symbol != null


    init {
        preallocateStorage(12)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(visibilityAttribute, visibility)
        initAttribute(modalityAttribute, modality)
        if (isInfix) setFlagInternal(isInfixAttribute, true)
        if (isOperator) setFlagInternal(isOperatorAttribute, true)
        if (isExpect) setFlagInternal(isExpectAttribute, true)
        if (isSuspend) setFlagInternal(isSuspendAttribute, true)
        if (isTailrec) setFlagInternal(isTailrecAttribute, true)
        if (isFakeOverride) setFlagInternal(isFakeOverrideAttribute, true)
        if (isInline) setFlagInternal(isInlineAttribute, true)
        if (isExternal) setFlagInternal(isExternalAttribute, true)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrFunctionWithLateBindingImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrFunctionWithLateBindingImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrFunctionWithLateBindingImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrFunctionWithLateBindingImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrFunctionWithLateBindingImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrFunctionWithLateBindingImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrFunctionWithLateBindingImpl::class.java, 6, "name", null)
        @JvmStatic private val isExternalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 63, "isExternal")
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrFunctionWithLateBindingImpl::class.java, 10, "visibility", null)
        @JvmStatic private val typeParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrTypeParameter>>(IrFunctionWithLateBindingImpl::class.java, 7, "typeParameters", emptyList())
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrFunctionWithLateBindingImpl::class.java, 13, "metadata", null)
        @JvmStatic private val isInlineAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 62, "isInline")
        @JvmStatic private val isExpectAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 58, "isExpect")
        @JvmStatic private val returnTypeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrFunctionWithLateBindingImpl::class.java, 15, "returnType", null)
        @JvmStatic private val bodyAttribute = IrIndexBasedAttributeRegistry.createAttr<IrBody?>(IrFunctionWithLateBindingImpl::class.java, 9, "body", null)
        @JvmStatic private val modalityAttribute = IrIndexBasedAttributeRegistry.createAttr<Modality>(IrFunctionWithLateBindingImpl::class.java, 14, "modality", null)
        @JvmStatic private val isFakeOverrideAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 61, "isFakeOverride")
        @JvmStatic private val overriddenSymbolsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrSimpleFunctionSymbol>>(IrFunctionWithLateBindingImpl::class.java, 16, "overriddenSymbols", emptyList())
        @JvmStatic private val isTailrecAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 60, "isTailrec")
        @JvmStatic private val isSuspendAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 59, "isSuspend")
        @JvmStatic private val isOperatorAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 57, "isOperator")
        @JvmStatic private val isInfixAttribute = IrIndexBasedAttributeRegistry.createFlag(IrFunctionWithLateBindingImpl::class.java, 56, "isInfix")
        @JvmStatic private val correspondingPropertySymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrPropertySymbol?>(IrFunctionWithLateBindingImpl::class.java, 17, "correspondingPropertySymbol", null)
    }

    private var _symbol: IrSimpleFunctionSymbol? = null

    override fun acquireSymbol(symbol: IrSimpleFunctionSymbol): IrFunctionWithLateBinding {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }
}
