/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.name.Name

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.moduleFragment]
 */
abstract class BirModuleFragment : BirImplElementBase(), BirElement {
    abstract val name: Name
    @ObsoleteDescriptorBasedAPI
    abstract val descriptor: ModuleDescriptor
    abstract val files: BirChildElementList<BirFile>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        files.acceptChildren(visitor, data)
    }

    companion object : BirElementClass(BirModuleFragment::class.java, 66)
}
