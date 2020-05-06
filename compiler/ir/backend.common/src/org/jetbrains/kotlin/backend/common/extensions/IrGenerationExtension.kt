/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IrExtensionGenerator
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

interface IrGenerationExtension : IrExtensionGenerator {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>("org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java)

    fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    )
}

// Extension point for plugins which run before any lowerings, but after the Ir has been constructed.
@Deprecated("This is a temporary class which will be replaced with another extension mechanism soon.", level = DeprecationLevel.ERROR)
interface PureIrGenerationExtension {
    @Suppress("DEPRECATION_ERROR")
    companion object :
        ProjectExtensionDescriptor<PureIrGenerationExtension>(
            "org.jetbrains.kotlin.pureIrGenerationExtension", PureIrGenerationExtension::class.java
        )

    fun generate(
        moduleFragment: IrModuleFragment,
        context: CommonBackendContext
    )
}
