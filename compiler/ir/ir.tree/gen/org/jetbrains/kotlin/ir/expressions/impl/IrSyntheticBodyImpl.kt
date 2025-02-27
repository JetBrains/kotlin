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
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrSyntheticBodyImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    kind: IrSyntheticBodyKind,
) : IrSyntheticBody() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var kind: IrSyntheticBodyKind by kindAttribute

    init {
        preallocateStorage(3)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(kindAttribute, kind)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrSyntheticBodyImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrSyntheticBodyImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrSyntheticBodyImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val kindAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSyntheticBodyKind>(IrSyntheticBodyImpl::class.java, 9, "kind", null)
    }
}
