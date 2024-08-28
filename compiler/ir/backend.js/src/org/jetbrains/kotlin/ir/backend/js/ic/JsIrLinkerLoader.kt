/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.checkIsFunctionInterface
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.FunctionTypeInterfacePackages
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal data class LoadedJsIr(
    val loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
    private val linker: JsIrLinker,
    private val functionTypeInterfacePackages: FunctionTypeInterfacePackages,
) {
    private val signatureProvidersImpl = hashMapOf<KotlinLibraryFile, List<FileSignatureProvider>>()

    private val irFileSourceNames = hashMapOf<IrModuleFragment, Map<IrFile, KotlinSourceFile>>()

    private fun collectSignatureProviders(lib: KotlinLibraryFile, irModule: IrModuleFragment): List<FileSignatureProvider> {
        val moduleDeserializer = linker.moduleDeserializer(irModule.descriptor)
        val deserializers = moduleDeserializer.fileDeserializers()
        val providers = ArrayList<FileSignatureProvider>(deserializers.size)
        val sourceFiles = getIrFileNames(irModule)

        for (fileDeserializer in deserializers) {
            val irFile = fileDeserializer.file
            val sourceFile = sourceFiles[irFile] ?: notFoundIcError("source file name", lib, irFile)
            if (functionTypeInterfacePackages.isFunctionTypeInterfacePackageFile(irFile)) {
                providers += FileSignatureProvider.GeneratedFunctionTypeInterface(irFile, sourceFile)
            } else {
                providers += FileSignatureProvider.DeserializedFromKlib(fileDeserializer, sourceFile)
            }
        }

        return providers
    }

    fun getSignatureProvidersForLib(lib: KotlinLibraryFile): List<FileSignatureProvider> {
        return signatureProvidersImpl.getOrPut(lib) {
            val irFragment = loadedFragments[lib] ?: notFoundIcError("loaded fragment", lib)
            collectSignatureProviders(lib, irFragment)
        }
    }

    fun loadUnboundSymbols() {
        signatureProvidersImpl.clear()
        ExternalDependenciesGenerator(linker.symbolTable, listOf(linker)).generateUnboundSymbolsAsDependencies()
        linker.postProcess(inOrAfterLinkageStep = true)
        linker.checkNoUnboundSymbols(linker.symbolTable, "at the end of IR linkage process")
        linker.clear()
    }

    fun collectSymbolsReplacedWithStubs(): Set<IrSymbol> {
        return linker.partialLinkageSupport.collectAllStubbedSymbols()
    }

    fun getIrFileNames(fragment: IrModuleFragment): Map<IrFile, KotlinSourceFile> {
        return irFileSourceNames.getOrPut(fragment) {
            val files = linker.getDeserializedFilesInKlibOrder(fragment)
            val names = files.map { it.fileEntry.name }
            val sourceFiles = KotlinSourceFile.fromSources(names)
            files.indices.associate {
                files[it] to sourceFiles[it]
            }
        }
    }
}

internal class JsIrLinkerLoader(
    private val compilerConfiguration: CompilerConfiguration,
    private val dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    private val mainModuleFriends: Collection<KotlinLibrary>,
    private val irFactory: IrFactory,
    private val stubbedSignatures: Set<IdSignature>,
) {
    private val mainLibrary = dependencyGraph.keys.lastOrNull() ?: notFoundIcError("main library")

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private class LinkerContext(
        val symbolTable: SymbolTable,
        val typeTranslator: TypeTranslatorImpl,
        val irBuiltIns: IrBuiltInsOverDescriptors,
        val linker: JsIrLinker,
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
        val messageCollector = compilerConfiguration.messageCollector
        val linker = JsIrLinker(
            currentModule = null,
            messageCollector = messageCollector,
            builtIns = irBuiltIns,
            symbolTable = symbolTable,
            partialLinkageSupport = createPartialLinkageSupportForLinker(
                partialLinkageConfig = compilerConfiguration.partialLinkageConfig,
                builtIns = irBuiltIns,
                messageCollector = messageCollector
            ),
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

    fun loadIr(
        modifiedFiles: KotlinSourceFileMap<KotlinSourceFileExports>,
        loadAllIr: Boolean = false,
        loadKotlinTest: Boolean = false,
    ): LoadedJsIr {
        val loadedModules = loadModules()
        val linkerContext = createLinker(loadedModules)

        val irModules = loadedModules.entries.associate { (descriptor, module) ->
            val libraryFile = KotlinLibraryFile(module)
            val modifiedStrategy = when {
                loadAllIr -> DeserializationStrategy.ALL
                module == mainLibrary -> DeserializationStrategy.ALL
                loadKotlinTest && descriptor.name.asString() == "<kotlin-test>" -> DeserializationStrategy.ALL //KT-71037
                else -> DeserializationStrategy.EXPLICITLY_EXPORTED
            }
            val modified = modifiedFiles[libraryFile]?.keys?.mapTo(hashSetOf()) { it.path } ?: emptySet()
            libraryFile to linkerContext.linker.deserializeIrModuleHeader(descriptor, module, {
                when (it) {
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
                            // The signature may refer to function type interface properties (e.g. name) or methods.
                            // It is impossible to detect (without hacks) here which binary symbol is required.
                            // However, when loading a property or a method the entire function type interface is loaded.
                            // And vice versa, a loading of function type interface loads properties and methods as well.
                            // Therefore, load the top level signature only - it must be the signature of function type interface.
                            val topLevelSignature = loadingSignature.topLevelSignature()
                            moduleDeserializer.tryDeserializeIrSymbol(topLevelSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL)
                        } else if (loadingSignature in moduleDeserializer) {
                            moduleDeserializer.addModuleReachableTopLevel(loadingSignature)
                        }
                    }
                }

                for (stubbedSignature in stubbedSignatures) {
                    if (stubbedSignature in moduleDeserializer) {
                        moduleDeserializer.addModuleReachableTopLevel(stubbedSignature)
                    }
                }
            }
        }

        val loadedIr = LoadedJsIr(irModules, linkerContext.linker, linkerContext.functionTypeInterfacePackages)
        loadedIr.loadUnboundSymbols()
        return loadedIr
    }
}
