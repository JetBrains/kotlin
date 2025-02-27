/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrLocalDelegatedPropertyReferenceImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    symbol: IrLocalDelegatedPropertySymbol,
    delegate: IrVariableSymbol,
    getter: IrSimpleFunctionSymbol,
    setter: IrSimpleFunctionSymbol?,
) : IrLocalDelegatedPropertyReference() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override val typeArguments: MutableList<IrType?> by typeArgumentsAttribute
    override var symbol: IrLocalDelegatedPropertySymbol by symbolAttribute
    override var delegate: IrVariableSymbol by delegateAttribute
    override var getter: IrSimpleFunctionSymbol by getterAttribute
    override var setter: IrSimpleFunctionSymbol? by setterAttribute

    init {
        preallocateStorage(10)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(typeArgumentsAttribute, ArrayList(0))
        initAttribute(originAttribute, origin)
        initAttribute(typeAttribute, type)
        initAttribute(getterAttribute, getter)
        initAttribute(delegateAttribute, delegate)
        initAttribute(symbolAttribute, symbol)
        initAttribute(setterAttribute, setter)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrLocalDelegatedPropertyReferenceImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrLocalDelegatedPropertyReferenceImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrLocalDelegatedPropertyReferenceImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrLocalDelegatedPropertyReferenceImpl::class.java, 7, "type", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrLocalDelegatedPropertyReferenceImpl::class.java, 4, "origin", null)
        @JvmStatic private val typeArgumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrType?>>(IrLocalDelegatedPropertyReferenceImpl::class.java, 3, "typeArguments", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrLocalDelegatedPropertySymbol>(IrLocalDelegatedPropertyReferenceImpl::class.java, 12, "symbol", null)
        @JvmStatic private val delegateAttribute = IrIndexBasedAttributeRegistry.createAttr<IrVariableSymbol>(IrLocalDelegatedPropertyReferenceImpl::class.java, 10, "delegate", null)
        @JvmStatic private val getterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunctionSymbol>(IrLocalDelegatedPropertyReferenceImpl::class.java, 9, "getter", null)
        @JvmStatic private val setterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunctionSymbol?>(IrLocalDelegatedPropertyReferenceImpl::class.java, 15, "setter", null)
    }
}
