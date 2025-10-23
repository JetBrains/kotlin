/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class JsIrLinker(
    private val currentModule: ModuleDescriptor?, messageCollector: MessageCollector, builtIns: IrBuiltIns, symbolTable: SymbolTable,
    override val partialLinkageSupport: PartialLinkageSupportForLinker,
    private val icData: ICData? = null,
    friendModules: Map<String, Collection<String>> = emptyMap(),
    private val stubGenerator: DeclarationStubGenerator? = null
) : KotlinIrLinker(
    currentModule = currentModule,
    messageCollector = messageCollector,
    builtIns = builtIns,
    symbolTable = symbolTable,
    exportedDependencies = emptyList(),
    deserializedSymbolPostProcessor = { symbol, signature, fileSymbol ->
        runIf(signature.isLocal) {
            symbol.privateSignature = IdSignature.CompositeSignature(IdSignature.FileSignature(fileSymbol), signature)
        }
        symbol
    }) {

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
        linker = this,
        symbolTable = symbolTable,
        mangler = JsManglerIr,
        typeSystem = IrTypeSystemContextImpl(builtIns),
        friendModules = friendModules,
        partialLinkageSupport = partialLinkageSupport
    )

    override val moduleDependencyTracker: IrModuleDependencyTracker = IrModuleDependencyTrackerImpl()

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean =
        moduleDescriptor === moduleDescriptor.builtIns.builtInsModule

    override fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy
    ): IrModuleDeserializer {
        require(klib != null) { "Expecting kotlin library" }
        val libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
        return when (val lazyIrGenerator = stubGenerator) {
            null -> JsModuleDeserializer(moduleDescriptor, klib.mainIr, strategyResolver, libraryAbiVersion)
            else -> JsLazyIrModuleDeserializer(moduleDescriptor, libraryAbiVersion, builtIns, lazyIrGenerator)
        }
    }

    private val deserializedFilesInKlibOrder = mutableMapOf<IrModuleFragment, List<IrFile>>()

    private inner class JsModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        override val ir: IrLibrary.IrDirectory,
        strategyResolver: (String) -> DeserializationStrategy,
        libraryAbiVersion: KotlinAbiVersion,
    ) : BasicIrModuleDeserializer(this, moduleDescriptor, strategyResolver, libraryAbiVersion) {

        override val klib get() = error("'klib' is not available for ${this::class.java}")

        override fun init(delegate: IrModuleDeserializer) {
            super.init(delegate)
            deserializedFilesInKlibOrder[moduleFragment] = fileDeserializationStates.memoryOptimizedMap { it.file }
        }
    }

    override fun createCurrentModuleDeserializer(moduleFragment: IrModuleFragment, dependencies: Collection<IrModuleDeserializer>): IrModuleDeserializer {
        val currentModuleDeserializer = super.createCurrentModuleDeserializer(moduleFragment, dependencies)

        icData?.let {
            return CurrentModuleWithICDeserializer(currentModuleDeserializer, symbolTable, builtIns, it.icData) { ir ->
                JsModuleDeserializer(currentModuleDeserializer.moduleDescriptor, ir, currentModuleDeserializer.strategyResolver, KotlinAbiVersion.CURRENT)
            }
        }
        return currentModuleDeserializer
    }

    val modules
        get() = deserializersForModules.values
            .map { it.moduleFragment }
            .filter { it.descriptor !== currentModule }


    fun moduleDeserializer(moduleDescriptor: ModuleDescriptor): IrModuleDeserializer {
        return deserializersForModules[moduleDescriptor.name.asString()] ?: error("Deserializer for $moduleDescriptor not found")
    }

    fun getDeserializedFilesInKlibOrder(fragment: IrModuleFragment): List<IrFile> {
        return deserializedFilesInKlibOrder[fragment] ?: emptyList()
    }
}
