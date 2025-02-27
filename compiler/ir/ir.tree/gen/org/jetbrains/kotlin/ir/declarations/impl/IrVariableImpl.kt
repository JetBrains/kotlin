/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.name.Name

class IrVariableImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    type: IrType,
    symbol: IrVariableSymbol,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
) : IrVariable() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory
        get() = error("Create IrVariableImpl directly")

    override var name: Name by nameAttribute
    override var type: IrType by typeAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptor
        get() = symbol.descriptor

    override val symbol: IrVariableSymbol by symbolAttribute
    override var isVar: Boolean by isVarAttribute
    override var isConst: Boolean by isConstAttribute
    override var isLateinit: Boolean by isLateinitAttribute
    override var initializer: IrExpression? by initializerAttribute

    init {
        preallocateStorage(8)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(nameAttribute, name)
        initAttribute(typeAttribute, type)
        initAttribute(symbolAttribute, symbol)
        if (isLateinit) setFlagInternal(isLateinitAttribute, true)
        if (isConst) setFlagInternal(isConstAttribute, true)
        if (isVar) setFlagInternal(isVarAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrVariableImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrVariableImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrVariableImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrVariableImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrVariableImpl::class.java, 4, "origin", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrVariableImpl::class.java, 6, "name", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrVariableImpl::class.java, 7, "type", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrVariableSymbol>(IrVariableImpl::class.java, 12, "symbol", null)
        @JvmStatic private val isVarAttribute = IrIndexBasedAttributeRegistry.createFlag(IrVariableImpl::class.java, 62, "isVar")
        @JvmStatic private val isConstAttribute = IrIndexBasedAttributeRegistry.createFlag(IrVariableImpl::class.java, 60, "isConst")
        @JvmStatic private val isLateinitAttribute = IrIndexBasedAttributeRegistry.createFlag(IrVariableImpl::class.java, 59, "isLateinit")
        @JvmStatic private val initializerAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression?>(IrVariableImpl::class.java, 9, "initializer", null)
    }
}
