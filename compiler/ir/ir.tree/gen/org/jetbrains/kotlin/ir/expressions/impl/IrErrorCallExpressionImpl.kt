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
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.utils.SmartList

class IrErrorCallExpressionImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    description: String,
) : IrErrorCallExpression() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var description: String by descriptionAttribute
    override var explicitReceiver: IrExpression? by explicitReceiverAttribute
    override val arguments: MutableList<IrExpression> by argumentsAttribute

    init {
        preallocateStorage(5)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(descriptionAttribute, description)
        initAttribute(argumentsAttribute, SmartList())
        initAttribute(typeAttribute, type)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrErrorCallExpressionImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrErrorCallExpressionImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrErrorCallExpressionImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrErrorCallExpressionImpl::class.java, 7, "type", null)
        @JvmStatic private val descriptionAttribute = IrIndexBasedAttributeRegistry.createAttr<String>(IrErrorCallExpressionImpl::class.java, 3, "description", null)
        @JvmStatic private val explicitReceiverAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression?>(IrErrorCallExpressionImpl::class.java, 5, "explicitReceiver", null)
        @JvmStatic private val argumentsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrExpression>>(IrErrorCallExpressionImpl::class.java, 4, "arguments", null)
    }
}
