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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrSuspensionPointImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    suspensionPointIdParameter: IrVariable,
    result: IrExpression,
    resumeResult: IrExpression,
) : IrSuspensionPoint() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var suspensionPointIdParameter: IrVariable by suspensionPointIdParameterAttribute
    override var result: IrExpression by resultAttribute
    override var resumeResult: IrExpression by resumeResultAttribute

    init {
        preallocateStorage(6)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(suspensionPointIdParameterAttribute, suspensionPointIdParameter)
        initAttribute(resumeResultAttribute, resumeResult)
        initAttribute(resultAttribute, result)
        initAttribute(typeAttribute, type)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrSuspensionPointImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrSuspensionPointImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrSuspensionPointImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrSuspensionPointImpl::class.java, 7, "type", null)
        @JvmStatic private val suspensionPointIdParameterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrVariable>(IrSuspensionPointImpl::class.java, 3, "suspensionPointIdParameter", null)
        @JvmStatic private val resultAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression>(IrSuspensionPointImpl::class.java, 5, "result", null)
        @JvmStatic private val resumeResultAttribute = IrIndexBasedAttributeRegistry.createAttr<IrExpression>(IrSuspensionPointImpl::class.java, 4, "resumeResult", null)
    }
}
