/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager


internal class JsIrLinkerLoader(
    private val compilerConfiguration: CompilerConfiguration,
    private val library: KotlinLibrary,
    private val dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    private val irFactory: IrFactory
) {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun createLinker(loadedModules: Map<ModuleDescriptor, KotlinLibrary>): JsIrLinker {
        val logger = compilerConfiguration[IrMessageLogger.IR_MESSAGE_LOGGER] ?: IrMessageLogger.None
        val signaturer = IdSignatureDescriptor(JsManglerDesc)
        val symbolTable = SymbolTable(signaturer, irFactory)
        val moduleDescriptor = loadedModules.keys.last()
        val typeTranslator = TypeTranslatorImpl(symbolTable, compilerConfiguration.languageVersionSettings, moduleDescriptor)
        val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)
        return JsIrLinker(null, logger, irBuiltIns, symbolTable, null)
    }

    private fun loadModules(): Map<ModuleDescriptor, KotlinLibrary> {
        val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()
        var runtimeModule: ModuleDescriptorImpl? = null

        // TODO: deduplicate this code using part from klib.kt
        fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
            val isBuiltIns = current.unresolvedDependencies.isEmpty()

            val lookupTracker = LookupTracker.DO_NOTHING
            val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                current,
                compilerConfiguration.languageVersionSettings,
                LockBasedStorageManager.NO_LOCKS,
                runtimeModule?.builtIns,
                packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
                lookupTracker = lookupTracker
            )
            if (isBuiltIns) runtimeModule = md

            val dependencies = dependencyGraph[current]!!.map { getModuleDescriptor(it) }
            md.setDependencies(listOf(md) + dependencies)
            md
        }

        return dependencyGraph.keys.associateBy { klib -> getModuleDescriptor(klib) }
    }

    fun processJsIrLinker(dirtyFiles: Collection<String>?): Triple<JsIrLinker, IrModuleFragment, Collection<IrModuleFragment>> {
        val loadedModules = loadModules()
        val jsIrLinker = createLinker(loadedModules)
        val irModules = ArrayList<Pair<IrModuleFragment, KotlinLibrary>>(loadedModules.size)

        // TODO: modules deserialized here have to be reused for cache building further
        for ((descriptor, loadedLibrary) in loadedModules) {
            if (library == loadedLibrary) {
                if (dirtyFiles != null) {
                    irModules.add(jsIrLinker.deserializeDirtyFiles(descriptor, loadedLibrary, dirtyFiles) to loadedLibrary)
                } else {
                    irModules.add(jsIrLinker.deserializeFullModule(descriptor, loadedLibrary) to loadedLibrary)
                }
            } else {
                irModules.add(jsIrLinker.deserializeHeadersWithInlineBodies(descriptor, loadedLibrary) to loadedLibrary)
            }
        }

        jsIrLinker.init(null, emptyList())
        ExternalDependenciesGenerator(jsIrLinker.symbolTable, listOf(jsIrLinker)).generateUnboundSymbolsAsDependencies()
        jsIrLinker.postProcess()

        val currentIrModule = irModules.find { it.second == library }?.first!!
        return Triple(jsIrLinker, currentIrModule, irModules.map { it.first })
    }
}
