/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.util.BirImplementationDetail
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.Name

abstract class BirModuleFragment() : BirImplElementBase(), BirElement {
    abstract val descriptor: ModuleDescriptor

    abstract val name: Name

    abstract val files: BirChildElementList<BirFile>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        files.acceptChildren(visitor, data)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirModuleFragment

    companion object : BirElementClass<BirModuleFragment>(BirModuleFragment::class.java, 67, true)
}
