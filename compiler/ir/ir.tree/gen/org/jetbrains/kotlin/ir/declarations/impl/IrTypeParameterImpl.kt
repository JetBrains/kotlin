/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class IrTypeParameterImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    symbol: IrTypeParameterSymbol,
    variance: Variance,
    index: Int,
    isReified: Boolean,
) : IrTypeParameter() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: TypeParameterDescriptor
        get() = symbol.descriptor

    override val symbol: IrTypeParameterSymbol by symbolAttribute
    override var variance: Variance by varianceAttribute
    override var index: Int by indexAttribute
    override var isReified: Boolean by isReifiedAttribute
    override var superTypes: List<IrType> by superTypesAttribute

    init {
        preallocateStorage(9)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(varianceAttribute, variance)
        initAttribute(indexAttribute, index)
        initAttribute(symbolAttribute, symbol)
        if (isReified) setFlagInternal(isReifiedAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeParameterImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeParameterImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrTypeParameterImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrTypeParameterImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrTypeParameterImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrTypeParameterImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrTypeParameterImpl::class.java, 6, "name", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrTypeParameterSymbol>(IrTypeParameterImpl::class.java, 12, "symbol", null)
        @JvmStatic private val varianceAttribute = IrIndexBasedAttributeRegistry.createAttr<Variance>(IrTypeParameterImpl::class.java, 7, "variance", null)
        @JvmStatic private val indexAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeParameterImpl::class.java, 9, "index", null)
        @JvmStatic private val isReifiedAttribute = IrIndexBasedAttributeRegistry.createFlag(IrTypeParameterImpl::class.java, 63, "isReified")
        @JvmStatic private val superTypesAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrType>>(IrTypeParameterImpl::class.java, 16, "superTypes", emptyList())
    }
}
