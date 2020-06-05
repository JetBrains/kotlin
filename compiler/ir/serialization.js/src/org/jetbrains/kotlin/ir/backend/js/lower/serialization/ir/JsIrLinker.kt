/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilderImpl
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.SerializedIrFile

class JsIrLinker(
    private val currentModule: ModuleDescriptor?, logger: LoggingContext, builtIns: IrBuiltIns, symbolTable: SymbolTable,
    override val functionalInterfaceFactory: IrAbstractFunctionFactory,
    private val icData: List<SerializedIrFile>? = null
) :
    KotlinIrLinker(currentModule, logger, builtIns, symbolTable, emptyList()) {

    override val fakeOverrideBuilderImpl = FakeOverrideBuilderImpl(symbolTable, IdSignatureSerializer(JsManglerIr), builtIns)

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
        moduleDescriptor === moduleDescriptor.builtIns.builtInsModule

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary?, strategy: DeserializationStrategy): IrModuleDeserializer =
        JsModuleDeserializer(moduleDescriptor, klib ?: error("Expecting kotlin library"), strategy)

    private inner class JsModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary, strategy: DeserializationStrategy) :
        KotlinIrLinker.BasicIrModuleDeserializer(moduleDescriptor, klib, strategy)

    override fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer {
        val currentModuleDeserializer = super.createCurrentModuleDeserializer(moduleFragment, dependencies)
        icData?.let {
            return CurrentModuleWithICDeserializer(currentModuleDeserializer, symbolTable, builtIns, it) { lib ->
                JsModuleDeserializer(currentModuleDeserializer.moduleDescriptor, lib, currentModuleDeserializer.strategy)
            }
        }
        return currentModuleDeserializer
    }
    val modules
        get() = deserializersForModules.values
            .map { it.moduleFragment }
            .filter { it.descriptor !== currentModule }
}
