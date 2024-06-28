/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name

// TODO: should be generated again after KT-68314 is fixed
class IrModuleFragmentImpl(override val descriptor: ModuleDescriptor) : IrModuleFragment() {
    constructor(descriptor: ModuleDescriptor, irBuiltins: IrBuiltIns) : this(descriptor) {
        initializeIrBuiltins(irBuiltins)
    }

    fun initializeIrBuiltins(irBuiltins: IrBuiltIns) {
        require(_irBuiltins == null) { "irBuiltins already initialized" }
        _irBuiltins = irBuiltins
    }

    private var _irBuiltins: IrBuiltIns? = null
    override val irBuiltins: IrBuiltIns
        get() = _irBuiltins ?: error("irBuiltins not initialized")

    override val startOffset: Int
        get() = UNDEFINED_OFFSET

    override val endOffset: Int
        get() = UNDEFINED_OFFSET

    override val name: Name
        get() = descriptor.name

    override val files: MutableList<IrFile> = ArrayList()
}
