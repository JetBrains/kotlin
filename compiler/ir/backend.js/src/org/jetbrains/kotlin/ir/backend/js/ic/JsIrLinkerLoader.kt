/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.checkIsFunctionInterface
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.FunctionTypeInterfacePackages
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.irMessageLogger
import org.jetbrains.kotlin.js.config.WebConfigurationKeys
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal data class LoadedJsIr(
    val loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
    private val linker: JsIrLinker,
    private val functionTypeInterfacePackages: FunctionTypeInterfacePackages
) {
    private val signatureProvidersImpl = hashMapOf<KotlinLibraryFile, List<FileSignatureProvider>>()

    private fun collectSignatureProviders(irModule: IrModuleFragment): List<FileSignatureProvider> {
        val moduleDeserializer = linker.moduleDeserializer(irModule.descriptor)
        val deserializers = moduleDeserializer.fileDeserializers()
        val providers = ArrayList<FileSignatureProvider>(deserializers.size)

        for (fileDeserializer in deserializers) {
            val irFile = fileDeserializer.file
            if (functionTypeInterfacePackages.isFunctionTypeInterfacePackageFile(irFile)) {
                providers += FileSignatureProvider.GeneratedFunctionTypeInterface(irFile)
            } else {
                providers += FileSignatureProvider.DeserializedFromKlib(fileDeserializer)
            }
        }

        return providers
    }

    fun getSignatureProvidersForLib(lib: KotlinLibraryFile): List<FileSignatureProvider> {
        return signatureProvidersImpl.getOrPut(lib) {
            val irFragment = loadedFragments[lib] ?: notFoundIcError("loaded fragment", lib)
            collectSignatureProviders(irFragment)
        }
    }

    fun loadUnboundSymbols() {
        signatureProvidersImpl.clear()
        ExternalDependenciesGenerator(linker.symbolTable, listOf(linker)).generateUnboundSymbolsAsDependencies()
        linker.postProcess()
        linker.checkNoUnboundSymbols(linker.symbolTable, "at the end of IR linkage process")
    }
}

internal class JsIrLinkerLoader(
    private val compilerConfiguration: CompilerConfiguration,
    private val dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    private val mainModuleFriends: Collection<KotlinLibrary>,
    private val irFactory: IrFactory
) {
    private val mainLibrary = dependencyGraph.keys.lastOrNull() ?: notFoundIcError("main library")

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private class LinkerContext(
        val symbolTable: SymbolTable,
        val typeTranslator: TypeTranslatorImpl,
        val irBuiltIns: IrBuiltInsOverDescriptors,
        val linker: JsIrLinker
    ) {
        val functionTypeInterfacePackages = FunctionTypeInterfacePackages()

        fun loadFunctionInterfacesIntoStdlib(stdlibModule: IrModuleFragment) {
            irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
                irBuiltIns,
                symbolTable,
                typeTranslator,
                functionTypeInterfacePackages.makePackageAccessor(stdlibModule),
                true
            )
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun createLinker(loadedModules: Map<ModuleDescriptor, KotlinLibrary>): LinkerContext {
        val signaturer = IdSignatureDescriptor(JsManglerDesc)
        val symbolTable = SymbolTable(signaturer, irFactory)
        val moduleDescriptor = loadedModules.keys.last()
        val typeTranslator = TypeTranslatorImpl(symbolTable, compilerConfiguration.languageVersionSettings, moduleDescriptor)
        val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)
        val partialLinkageEnabled = compilerConfiguration[WebConfigurationKeys.PARTIAL_LINKAGE] ?: false
        val linker = JsIrLinker(
            currentModule = null,
            messageLogger = compilerConfiguration.irMessageLogger,
            builtIns = irBuiltIns,
            symbolTable = symbolTable,
            partialLinkageEnabled = partialLinkageEnabled,
            translationPluginContext = null,
            friendModules = mapOf(mainLibrary.uniqueName to mainModuleFriends.map { it.uniqueName })
        )
        return LinkerContext(symbolTable, typeTranslator, irBuiltIns, linker)
    }

    private fun loadModules(): Map<ModuleDescriptor, KotlinLibrary> {
        val descriptors = hashMapOf<KotlinLibrary, ModuleDescriptorImpl>()
        var runtimeModule: ModuleDescriptorImpl? = null

        // TODO: deduplicate this code using part from klib.kt
        fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl {
            if (current in descriptors) {
                return descriptors.getValue(current)
            }

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

            descriptors[current] = md
            return md
        }

        val moduleDescriptorToKotlinLibrary = dependencyGraph.keys.associateBy { klib -> getModuleDescriptor(klib) }
        return moduleDescriptorToKotlinLibrary
            .onEach { (key, _) -> key.setDependencies(moduleDescriptorToKotlinLibrary.keys.toList()) }
            .map<ModuleDescriptorImpl, KotlinLibrary, Pair<ModuleDescriptor, KotlinLibrary>> { it.key to it.value }
            .toMap()
    }

    fun loadIr(modifiedFiles: KotlinSourceFileMap<KotlinSourceFileExports>, loadAllIr: Boolean = false): LoadedJsIr {
        val loadedModules = loadModules()
        val linkerContext = createLinker(loadedModules)

        val irModules = loadedModules.entries.associate { (descriptor, module) ->
            val libraryFile = KotlinLibraryFile(module)
            val modifiedStrategy = when {
                loadAllIr -> DeserializationStrategy.ALL
                module == mainLibrary -> DeserializationStrategy.ALL
                else -> DeserializationStrategy.EXPLICITLY_EXPORTED
            }
            val modified = modifiedFiles[libraryFile] ?: emptyMap()
            libraryFile to linkerContext.linker.deserializeIrModuleHeader(descriptor, module, {
                when (KotlinSourceFile(it)) {
                    in modified -> modifiedStrategy
                    else -> DeserializationStrategy.WITH_INLINE_BODIES
                }
            })
        }

        val mainLibraryFile = KotlinLibraryFile(mainLibrary)
        val mainFragment = irModules[mainLibraryFile] ?: notFoundIcError("main module fragment", mainLibraryFile)
        val (_, stdlibFragment) = findStdlib(mainFragment, irModules)
        linkerContext.loadFunctionInterfacesIntoStdlib(stdlibFragment)

        linkerContext.linker.init(null, emptyList())

        if (!loadAllIr) {
            for ((loadingLibFile, loadingSrcFiles) in modifiedFiles) {
                val loadingIrModule = irModules[loadingLibFile] ?: notFoundIcError("loading fragment", loadingLibFile)
                val moduleDeserializer = linkerContext.linker.moduleDeserializer(loadingIrModule.descriptor)
                for (loadingSrcFileSignatures in loadingSrcFiles.values) {
                    for (loadingSignature in loadingSrcFileSignatures.getExportedSignatures()) {
                        if (checkIsFunctionInterface(loadingSignature)) {
                            moduleDeserializer.tryDeserializeIrSymbol(loadingSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL)
                        } else if (loadingSignature in moduleDeserializer) {
                            moduleDeserializer.addModuleReachableTopLevel(loadingSignature)
                        }
                    }
                }
            }
        }

        val loadedIr = LoadedJsIr(irModules, linkerContext.linker, linkerContext.functionTypeInterfacePackages)
        loadedIr.loadUnboundSymbols()
        return loadedIr
    }
}
