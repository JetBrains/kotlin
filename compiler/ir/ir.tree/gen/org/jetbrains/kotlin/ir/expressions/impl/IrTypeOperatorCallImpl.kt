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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrTypeOperatorCallImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    operator: IrTypeOperator,
    argument: IrExpression,
    typeOperand: IrType,
) : IrTypeOperatorCall() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var operator: IrTypeOperator by operatorAttribute
    override var argument: IrExpression by argumentAttribute
    override var typeOperand: IrType by typeOperandAttribute

    init {
        preallocateStorage(6)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(operatorAttribute, operator)
        initAttribute(argumentAttribute, argument)
        initAttribute(typeOperandAttribute, typeOperand)
        initAttribute(typeAttribute, type)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeOperatorCallImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrTypeOperatorCallImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrTypeOperatorCallImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrTypeOperatorCallImpl::class.java, 7, "type", null)
        @JvmStatic private val operatorAttribute = IrIndexBasedAttributeRegistry.createAttr<IrTypeOperator>(IrTypeOperatorCallImpl::class.java, 3, "operator", null)
        @JvmStatic private val argumentAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression>(IrTypeOperatorCallImpl::class.java, 4, "argument", null)
        @JvmStatic private val typeOperandAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrTypeOperatorCallImpl::class.java, 5, "typeOperand", null)
    }
}
