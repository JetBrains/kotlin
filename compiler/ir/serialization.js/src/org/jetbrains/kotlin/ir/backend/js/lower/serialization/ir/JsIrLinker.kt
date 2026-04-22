/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.PartialLinkageConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.library.isJsStdlib
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class JsIrLinker(
    configuration: CompilerConfiguration,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    partialLinkageConfig: PartialLinkageConfig,
    irDiagnosticReporter: IrDiagnosticReporter,
    friendModules: Map<String, Collection<String>> = emptyMap(),
) : KotlinIrLinker(
    currentModule = null,
    configuration = configuration,
    builtIns = builtIns,
    symbolTable = symbolTable,
    exportedDependencies = emptyList(),
    deserializedSymbolPostProcessor = { symbol, signature, fileSymbol ->
        runIf(signature.isLocal) {
            symbol.privateSignature = IdSignature.CompositeSignature(IdSignature.FileSignature(fileSymbol), signature)
        }
        symbol
    }) {

    override val partialLinkageSupport: PartialLinkageSupportForLinker = createPartialLinkageSupportForLinker(
        partialLinkageConfig = partialLinkageConfig,
        builtIns = builtIns,
        diagnosticReporter = irDiagnosticReporter,
    )

    override val irMangler: KotlinMangler.IrMangler = JsManglerIr

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
        linker = this,
        symbolTable = symbolTable,
        mangler = irMangler,
        friendModules = friendModules,
        partialLinkageSupport = partialLinkageSupport
    )

    override val moduleDependencyTracker: IrModuleDependencyTracker = IrModuleDependencyTrackerImpl()

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean {
        val klib = (moduleDescriptor.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library ?: return false
        return klib.isJsStdlib || klib.isWasmStdlib
    }

    override fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy
    ): IrModuleDeserializer {
        require(klib != null) { "Expecting kotlin library" }
        val libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
        return JsModuleDeserializer(moduleDescriptor, klib.irOrFail, strategyResolver, libraryAbiVersion)
    }

    private val deserializedFilesInKlibOrder = mutableMapOf<IrModuleFragment, List<IrFile>>()

    private inner class JsModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        override val ir: KlibIrComponent,
        strategyResolver: (String) -> DeserializationStrategy,
        libraryAbiVersion: KotlinAbiVersion,
    ) : BasicIrModuleDeserializer(this, moduleDescriptor, strategyResolver, libraryAbiVersion) {

        override val klib get() = error("'klib' is not available for ${this::class.java}")

        override fun init(delegate: IrModuleDeserializer) {
            super.init(delegate)
            deserializedFilesInKlibOrder[moduleFragment] = fileDeserializationStates.memoryOptimizedMap { it.file }
        }
    }

    val modules
        get() = deserializersForModules.values
            .map { it.moduleFragment }


    fun moduleDeserializer(moduleDescriptor: ModuleDescriptor): IrModuleDeserializer {
        return deserializersForModules[moduleDescriptor.name.asString()] ?: error("Deserializer for $moduleDescriptor not found")
    }

    fun getDeserializedFilesInKlibOrder(fragment: IrModuleFragment): List<IrFile> {
        return deserializedFilesInKlibOrder[fragment] ?: emptyList()
    }
}
