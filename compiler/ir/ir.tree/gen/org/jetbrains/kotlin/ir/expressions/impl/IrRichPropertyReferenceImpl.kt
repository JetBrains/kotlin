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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrDeclarationWithAccessorsSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrRichPropertyReferenceImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    reflectionTargetSymbol: IrDeclarationWithAccessorsSymbol?,
    origin: IrStatementOrigin?,
    getterFunction: IrSimpleFunction,
    setterFunction: IrSimpleFunction?,
) : IrRichPropertyReference() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var reflectionTargetSymbol: IrDeclarationWithAccessorsSymbol? by reflectionTargetSymbolAttribute
    override val boundValues: MutableList<IrExpression> by boundValuesAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override var getterFunction: IrSimpleFunction by getterFunctionAttribute
    override var setterFunction: IrSimpleFunction? by setterFunctionAttribute

    init {
        preallocateStorage(8)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(reflectionTargetSymbolAttribute, reflectionTargetSymbol)
        initAttribute(originAttribute, origin)
        initAttribute(boundValuesAttribute, ArrayList())
        initAttribute(getterFunctionAttribute, getterFunction)
        initAttribute(typeAttribute, type)
        initAttribute(setterFunctionAttribute, setterFunction)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrRichPropertyReferenceImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrRichPropertyReferenceImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrRichPropertyReferenceImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrRichPropertyReferenceImpl::class.java, 7, "type", null)
        @JvmStatic private val reflectionTargetSymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationWithAccessorsSymbol?>(IrRichPropertyReferenceImpl::class.java, 3, "reflectionTargetSymbol", null)
        @JvmStatic private val boundValuesAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrExpression>>(IrRichPropertyReferenceImpl::class.java, 5, "boundValues", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrRichPropertyReferenceImpl::class.java, 4, "origin", null)
        @JvmStatic private val getterFunctionAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction>(IrRichPropertyReferenceImpl::class.java, 6, "getterFunction", null)
        @JvmStatic private val setterFunctionAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction?>(IrRichPropertyReferenceImpl::class.java, 8, "setterFunction", null)
    }
}
