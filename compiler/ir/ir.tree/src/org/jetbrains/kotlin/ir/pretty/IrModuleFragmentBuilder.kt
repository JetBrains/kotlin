/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl

@PrettyIrDsl
class IrModuleFragmentBuilder internal constructor(
    private val symbolContext: SymbolContext,
    private val moduleDescriptor: ModuleDescriptor,
    private val irBuiltins: IrBuiltIns
) : IrElementBuilder<IrModuleFragment>() {

    private val fileBuilders = mutableListOf<IrFileBuilder>()

    @IrNodeBuilderDsl
    fun irFile(name: String, block: IrElementBuilderClosure<IrFileBuilder>) {
        val builder = IrFileBuilder(symbolContext, name)
        builder.block()
        fileBuilders.add(builder)
    }

    @Deprecated(
        "Custom debug info is not supported for IrModuleFragment",
        replaceWith = ReplaceWith(""),
        level = DeprecationLevel.ERROR,
    )
    override fun debugInfo(startOffset: Int, endOffset: Int) {
        throw UnsupportedOperationException("Custom debug info is not supported for IrModuleFragment")
    }

    override fun build(): IrModuleFragment {
        val moduleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltins)
        for (fileBuilder in fileBuilders) {
            val file = fileBuilder.build() as IrFileImpl
            file.module = moduleFragment
            moduleFragment.files.add(file)
        }
        return moduleFragment
    }
}
