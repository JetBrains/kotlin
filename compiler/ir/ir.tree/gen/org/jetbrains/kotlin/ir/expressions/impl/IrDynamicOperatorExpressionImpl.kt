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
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.utils.SmartList

class IrDynamicOperatorExpressionImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    operator: IrDynamicOperator,
) : IrDynamicOperatorExpression() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var operator: IrDynamicOperator by operatorAttribute
    override var receiver: IrExpression by receiverAttribute
    override val arguments: MutableList<IrExpression> by argumentsAttribute

    init {
        preallocateStorage(6)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(operatorAttribute, operator)
        initAttribute(argumentsAttribute, SmartList())
        initAttribute(typeAttribute, type)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrDynamicOperatorExpressionImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrDynamicOperatorExpressionImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrDynamicOperatorExpressionImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrDynamicOperatorExpressionImpl::class.java, 7, "type", null)
        @JvmStatic private val operatorAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDynamicOperator>(IrDynamicOperatorExpressionImpl::class.java, 3, "operator", null)
        @JvmStatic private val receiverAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression>(IrDynamicOperatorExpressionImpl::class.java, 6, "receiver", null)
        @JvmStatic private val argumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrExpression>>(IrDynamicOperatorExpressionImpl::class.java, 4, "arguments", null)
    }
}
