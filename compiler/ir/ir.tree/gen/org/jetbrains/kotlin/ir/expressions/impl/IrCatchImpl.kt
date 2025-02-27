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
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrCatchImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    catchParameter: IrVariable,
    origin: IrStatementOrigin?,
) : IrCatch() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var catchParameter: IrVariable by catchParameterAttribute
    override var result: IrExpression by resultAttribute
    override var origin: IrStatementOrigin? by originAttribute

    init {
        preallocateStorage(5)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(catchParameterAttribute, catchParameter)
        initAttribute(originAttribute, origin)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrCatchImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrCatchImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrCatchImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val catchParameterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrVariable>(IrCatchImpl::class.java, 3, "catchParameter", null)
        @JvmStatic private val resultAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression>(IrCatchImpl::class.java, 5, "result", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrCatchImpl::class.java, 4, "origin", null)
    }
}
