/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.IrSourceElement
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name

class IrModuleFragmentImpl(
    override val descriptor: ModuleDescriptor,
) : IrModuleFragment() {
    override val sourceLocation: IrSourceElement
        get() = IrSourceElement(UNDEFINED_OFFSET, UNDEFINED_OFFSET)

    override var attributeOwnerId: IrElement = this

    override val name: Name
        get() = descriptor.name

    override val files: MutableList<IrFile> = ArrayList()
}
