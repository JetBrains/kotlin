/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
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
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.irMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal fun JsIrLinker.loadUnboundSymbols() {
    ExternalDependenciesGenerator(symbolTable, listOf(this)).generateUnboundSymbolsAsDependencies()
    postProcess()
    checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")
}

internal class JsIrLinkerLoader(
    private val compilerConfiguration: CompilerConfiguration,
    private val dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    private val mainModuleFriends: Collection<KotlinLibrary>,
    private val irFactory: IrFactory
) {
    private val mainLibrary = dependencyGraph.keys.lastOrNull() ?: notFoundIcError("main library")

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun createLinker(loadedModules: Map<ModuleDescriptor, KotlinLibrary>): JsIrLinker {
        val signaturer = IdSignatureDescriptor(JsManglerDesc)
        val symbolTable = SymbolTable(signaturer, irFactory)
        val moduleDescriptor = loadedModules.keys.last()
        val typeTranslator = TypeTranslatorImpl(symbolTable, compilerConfiguration.languageVersionSettings, moduleDescriptor)
        val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)
        val partialLinkageEnabled = compilerConfiguration[JSConfigurationKeys.PARTIAL_LINKAGE] ?: false
        return JsIrLinker(
            currentModule = null,
            messageLogger = compilerConfiguration.irMessageLogger,
            builtIns = irBuiltIns,
            symbolTable = symbolTable,
            partialLinkageEnabled = partialLinkageEnabled,
            translationPluginContext = null,
            friendModules = mapOf(mainLibrary.uniqueName to mainModuleFriends.map { it.uniqueName })
        )
    }

    private fun loadModules(): Map<ModuleDescriptor, KotlinLibrary> {
        val descriptors = hashMapOf<KotlinLibrary, ModuleDescriptorImpl>()
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

    data class LoadedJsIr(val linker: JsIrLinker, val loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>)

    fun loadIr(modifiedFiles: KotlinSourceFileMap<KotlinSourceFileExports>, loadAllIr: Boolean = false): LoadedJsIr {
        val loadedModules = loadModules()
        val jsIrLinker = createLinker(loadedModules)

        val irModules = loadedModules.entries.associate { (descriptor, module) ->
            val libraryFile = KotlinLibraryFile(module)
            val modifiedStrategy = when {
                loadAllIr -> DeserializationStrategy.ALL
                module == mainLibrary -> DeserializationStrategy.ALL
                else -> DeserializationStrategy.EXPLICITLY_EXPORTED
            }
            val modified = modifiedFiles[libraryFile] ?: emptyMap()
            libraryFile to jsIrLinker.deserializeIrModuleHeader(descriptor, module, {
                when (KotlinSourceFile(it)) {
                    in modified -> modifiedStrategy
                    else -> DeserializationStrategy.WITH_INLINE_BODIES
                }
            })
        }

        jsIrLinker.init(null, emptyList())

        if (!loadAllIr) {
            for ((loadingLibFile, loadingSrcFiles) in modifiedFiles) {
                val loadingIrModule = irModules[loadingLibFile] ?: notFoundIcError("loading fragment", loadingLibFile)
                val moduleDeserializer = jsIrLinker.moduleDeserializer(loadingIrModule.descriptor)
                for (loadingSrcFileSignatures in loadingSrcFiles.values) {
                    for (loadingSignature in loadingSrcFileSignatures.getExportedSignatures()) {
                        if (loadingSignature in moduleDeserializer) {
                            moduleDeserializer.addModuleReachableTopLevel(loadingSignature)
                        }
                    }
                }
            }
        }

        jsIrLinker.loadUnboundSymbols()
        return LoadedJsIr(jsIrLinker, irModules)
    }
}
