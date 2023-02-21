/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

@PrettyIrDsl
class IrWorldBuilder(private val irBuiltins: IrBuiltIns) {

    private val moduleBuilders = mutableListOf<IrModuleFragmentBuilder>()

    private val symbolContext = SymbolContextImpl()

    @IrNodeBuilderDsl
    fun irModuleFragment(block: IrElementBuilderClosure<IrModuleFragmentBuilder>) {
        val moduleDescriptor: ModuleDescriptor = TODO()
        moduleBuilders.add(IrModuleFragmentBuilder(symbolContext, moduleDescriptor, irBuiltins).apply(block))
    }

    fun build(): List<IrModuleFragment> = moduleBuilders.map(IrModuleFragmentBuilder::build)
}
