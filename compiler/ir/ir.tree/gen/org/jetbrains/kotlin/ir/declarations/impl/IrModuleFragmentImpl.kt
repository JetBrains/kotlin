/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name

class IrModuleFragmentImpl(
    descriptor: ModuleDescriptor,
) : IrModuleFragment() {
    override var startOffset: Int
        get() = UNDEFINED_OFFSET
        set(value) {
            error("Mutation of startOffset is not supported for this class.")
        }

    override var endOffset: Int
        get() = UNDEFINED_OFFSET
        set(value) {
            error("Mutation of endOffset is not supported for this class.")
        }

    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override val descriptor: ModuleDescriptor by descriptorAttribute
    override val name: Name
        get() = descriptor.name

    override val files: MutableList<IrFile> by filesAttribute

    init {
        preallocateStorage(5)
        initAttribute(filesAttribute, ArrayList())
        initAttribute(descriptorAttribute, descriptor)
    }
    companion object {
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrModuleFragmentImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val descriptorAttribute = IrIndexBasedAttributeRegistry.createAttr<ModuleDescriptor>(IrModuleFragmentImpl::class.java, 8, "descriptor", null)
        @JvmStatic private val filesAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrFile>>(IrModuleFragmentImpl::class.java, 3, "files", null)
    }
}
