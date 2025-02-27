/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrValueParameterImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    type: IrType,
    kind: IrParameterKind,
    isAssignable: Boolean,
    symbol: IrValueParameterSymbol,
    varargElementType: IrType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean,
) : IrValueParameter() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var type: IrType by typeAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ParameterDescriptor
        get() = symbol.descriptor

    override var kind: IrParameterKind by kindAttribute
    override var indexInParameters: Int by indexInParametersAttribute
    override var indexInOldValueParameters: Int by indexInOldValueParametersAttribute
    override var isAssignable: Boolean by isAssignableAttribute
    override val symbol: IrValueParameterSymbol by symbolAttribute
    override var varargElementType: IrType? by varargElementTypeAttribute
    override var isCrossinline: Boolean by isCrossinlineAttribute
    override var isNoinline: Boolean by isNoinlineAttribute
    override var isHidden: Boolean by isHiddenAttribute
    override var defaultValue: IrExpressionBody? by defaultValueAttribute

    init {
        preallocateStorage(11)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(typeAttribute, type)
        initAttribute(kindAttribute, kind)
        initAttribute(symbolAttribute, symbol)
        initAttribute(varargElementTypeAttribute, varargElementType)
        if (isHidden) setFlagInternal(isHiddenAttribute, true)
        if (isNoinline) setFlagInternal(isNoinlineAttribute, true)
        if (isCrossinline) setFlagInternal(isCrossinlineAttribute, true)
        if (isAssignable) setFlagInternal(isAssignableAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrValueParameterImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrValueParameterImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrValueParameterImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrValueParameterImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrValueParameterImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrValueParameterImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrValueParameterImpl::class.java, 6, "name", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrValueParameterImpl::class.java, 7, "type", null)
        @JvmStatic private val kindAttribute = IrIndexBasedAttributeRegistry.createAttr<IrParameterKind>(IrValueParameterImpl::class.java, 9, "kind", null)
        @JvmStatic private val indexInParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrValueParameterImpl::class.java, 10, "indexInParameters", -1)
        @JvmStatic private val indexInOldValueParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrValueParameterImpl::class.java, 11, "indexInOldValueParameters", -1)
        @JvmStatic private val isAssignableAttribute = IrIndexBasedAttributeRegistry.createFlag(IrValueParameterImpl::class.java, 63, "isAssignable")
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrValueParameterSymbol>(IrValueParameterImpl::class.java, 12, "symbol", null)
        @JvmStatic private val varargElementTypeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType?>(IrValueParameterImpl::class.java, 13, "varargElementType", null)
        @JvmStatic private val isCrossinlineAttribute = IrIndexBasedAttributeRegistry.createFlag(IrValueParameterImpl::class.java, 62, "isCrossinline")
        @JvmStatic private val isNoinlineAttribute = IrIndexBasedAttributeRegistry.createFlag(IrValueParameterImpl::class.java, 61, "isNoinline")
        @JvmStatic private val isHiddenAttribute = IrIndexBasedAttributeRegistry.createFlag(IrValueParameterImpl::class.java, 60, "isHidden")
        @JvmStatic private val defaultValueAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpressionBody?>(IrValueParameterImpl::class.java, 14, "defaultValue", null)
    }
}
