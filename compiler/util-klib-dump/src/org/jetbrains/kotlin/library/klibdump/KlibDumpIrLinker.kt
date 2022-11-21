/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.klibdump

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.containsErrorCode

class KlibDumpIrLinker(currentModule: ModuleDescriptor, messageLogger: IrMessageLogger, builtIns: IrBuiltIns, symbolTable: SymbolTable) :
    KotlinIrLinker(
        currentModule = currentModule,
        messageLogger = messageLogger,
        builtIns = builtIns,
        symbolTable = symbolTable,
        exportedDependencies = emptyList(),
        partialLinkageEnabled = false,
        symbolProcessor = { s, _ -> s },
    ) {
    override val fakeOverrideBuilder: FakeOverrideBuilder
        get() = FakeOverrideBuilder(
            linker = this,
            symbolTable = symbolTable,
            mangler = KlibDumpIrMangler,
            typeSystem = IrTypeSystemContextImpl(builtIns),
            friendModules = emptyMap(),
            partialLinkageEnabled = partialLinkageSupport.partialLinkageEnabled
        )

    override val translationPluginContext: TranslationPluginContext?
        get() = null

    override fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy
    ): IrModuleDeserializer {
        require(klib != null) { "Expecting kotlin library" }
        val libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
        return Deserializer(moduleDescriptor, klib, strategyResolver, libraryAbiVersion, klib.containsErrorCode)
    }

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = false

    private inner class Deserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: IrLibrary,
        strategyResolver: (String) -> DeserializationStrategy,
        libraryAbiVersion: KotlinAbiVersion,
        allowErrorCode: Boolean
    ) : BasicIrModuleDeserializer(this, moduleDescriptor, klib, strategyResolver, libraryAbiVersion, allowErrorCode)
}
