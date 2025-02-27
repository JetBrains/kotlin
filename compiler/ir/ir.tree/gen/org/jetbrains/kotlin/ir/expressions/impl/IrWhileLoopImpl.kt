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
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrWhileLoopImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
) : IrWhileLoop() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override var body: IrExpression? by bodyAttribute
    override var condition: IrExpression by conditionAttribute
    override var label: String? by labelAttribute

    init {
        preallocateStorage(5)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(typeAttribute, type)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrWhileLoopImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrWhileLoopImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrWhileLoopImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrWhileLoopImpl::class.java, 7, "type", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrWhileLoopImpl::class.java, 4, "origin", null)
        @JvmStatic private val bodyAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression?>(IrWhileLoopImpl::class.java, 9, "body", null)
        @JvmStatic private val conditionAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression>(IrWhileLoopImpl::class.java, 3, "condition", null)
        @JvmStatic private val labelAttribute = IrIndexBasedAttributeRegistry.createAttr<String?>(IrWhileLoopImpl::class.java, 5, "label", null)
    }
}
