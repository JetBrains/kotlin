/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideControl
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.containsErrorCode
import org.jetbrains.kotlin.resolve.BindingContext

class JsIrLinker(
    private val currentModule: ModuleDescriptor?, logger: LoggingContext, builtIns: IrBuiltIns, symbolTable: SymbolTable,
    override val functionalInterfaceFactory: IrAbstractFunctionFactory,
    override val translationPluginContext: TranslationPluginContext?,
    private val icData: ICData? = null,
    deserializeFakeOverrides: Boolean = FakeOverrideControl.deserializeFakeOverrides
) : KotlinIrLinker(currentModule, logger, builtIns, symbolTable, emptyList(), deserializeFakeOverrides) {

    override val fakeOverrideBuilder = FakeOverrideBuilder(symbolTable, IdSignatureSerializer(JsManglerIr), builtIns)

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
        moduleDescriptor === moduleDescriptor.builtIns.builtInsModule

    private val IrLibrary.libContainsErrorCode: Boolean
        get() = this is KotlinLibrary && this.containsErrorCode

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary?, strategy: DeserializationStrategy): IrModuleDeserializer =
        JsModuleDeserializer(moduleDescriptor, klib ?: error("Expecting kotlin library"), strategy, klib.libContainsErrorCode)

    private inner class JsModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary, strategy: DeserializationStrategy, allowErrorCode: Boolean) :
        KotlinIrLinker.BasicIrModuleDeserializer(moduleDescriptor, klib, strategy, allowErrorCode)

    override fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer {
        val currentModuleDeserializer = super.createCurrentModuleDeserializer(moduleFragment, dependencies)
        icData?.let {
            return CurrentModuleWithICDeserializer(currentModuleDeserializer, symbolTable, builtIns, it.icData) { lib ->
                JsModuleDeserializer(currentModuleDeserializer.moduleDescriptor, lib, currentModuleDeserializer.strategy, it.containsErrorCode)
            }
        }
        return currentModuleDeserializer
    }

    val modules
        get() = deserializersForModules.values
            .map { it.moduleFragment }
            .filter { it.descriptor !== currentModule }

    class JsFePluginContext(
        override val moduleDescriptor: ModuleDescriptor,
        override val bindingContext: BindingContext,
        override val symbolTable: ReferenceSymbolTable,
        override val typeTranslator: TypeTranslator,
        override val irBuiltIns: IrBuiltIns,
    ) : TranslationPluginContext
}
