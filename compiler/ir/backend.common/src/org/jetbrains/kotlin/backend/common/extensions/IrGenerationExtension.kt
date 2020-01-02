/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.BindingContext

class IrPluginContext(
    val moduleDescriptor: ModuleDescriptor,
    val bindingContext: BindingContext,
    val languageVersionSettings: LanguageVersionSettings,
    val symbolTable: SymbolTable,
    val typeTranslator: TypeTranslator,
    override val irBuiltIns: IrBuiltIns,
    val symbols: BuiltinSymbolsBase = BuiltinSymbolsBase(irBuiltIns.builtIns, symbolTable)
) : IrGeneratorContext()

interface IrGenerationExtension {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>("org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java)

    fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    )
}