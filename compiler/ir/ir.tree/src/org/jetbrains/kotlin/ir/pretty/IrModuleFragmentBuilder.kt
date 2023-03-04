/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager

@PrettyIrDsl
class IrModuleFragmentBuilder @PublishedApi internal constructor(
    private val moduleDescriptor: ModuleDescriptor,
    private val irBuiltins: IrBuiltIns,
    buildingContext: IrBuildingContext,
) : IrElementBuilder<IrModuleFragment>(buildingContext) {

    private val fileBuilders = mutableListOf<IrFileBuilder>()

    @IrNodeBuilderDsl
    fun irFile(name: String, block: IrElementBuilderClosure<IrFileBuilder>) {
        val builder = IrFileBuilder(name, buildingContext)
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

    @PublishedApi
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
