/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibCheckers
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.*
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.toSmartList

val KotlinLibrary.moduleName: String
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)

val KotlinLibrary.jsOutputName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_JS_OUTPUT_NAME)

val KotlinLibrary.serializedIrFileFingerprints: List<SerializedIrFileFingerprint>?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS)?.parseSerializedIrFileFingerprints()

val KotlinLibrary.serializedKlibFingerprint: SerializedKlibFingerprint?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT)?.let { SerializedKlibFingerprint.fromString(it) }

internal val SerializedIrFile.fileMetadata: ByteArray
    get() = backendSpecificMetadata ?: error("Expect file caches to have backendSpecificMetadata, but '$path' doesn't")

fun generateKLib(
    modulesStructure: ModulesStructure,
    outputKlibPath: String,
    nopack: Boolean,
    jsOutputName: String?,
    icData: List<KotlinFileSerializedData>,
    moduleFragment: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    diagnosticReporter: DiagnosticReporter,
    builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS,
    wasmTarget: WasmTarget? = null,
) {
    val configuration = modulesStructure.compilerConfiguration

    serializeModuleIntoKlib(
        moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!,
        configuration = configuration,
        diagnosticReporter = diagnosticReporter,
        metadataSerializer = KlibMetadataIncrementalSerializer(modulesStructure, moduleFragment),
        klibPath = outputKlibPath,
        dependencies = modulesStructure.klibs.all,
        moduleFragment = moduleFragment,
        irBuiltIns = irBuiltIns,
        cleanFiles = icData,
        nopack = nopack,
        containsErrorCode = modulesStructure.jsFrontEndResult.hasErrors,
        jsOutputName = jsOutputName,
        builtInsPlatform = builtInsPlatform,
        wasmTarget = wasmTarget,
    )
}

/**
 * Note: [deserializeDependencies] returns the list of the deserialized [IrModuleFragment]s that has the same
 * order of libraries as in [LoadedKlibs.all]. See the documentation of [LoadedKlibs] for more details.
 */
fun deserializeDependencies(
    klibs: LoadedKlibs,
    irLinker: JsIrLinker,
    filesToLoad: Set<String>?,
    mapping: (KotlinLibrary) -> ModuleDescriptor
): IrModuleDependencies {
    val all: MutableList<IrModuleFragment> = mutableListOf()
    var stdlib: IrModuleFragment? = null
    var included: IrModuleFragment? = null

    klibs.all.forEach { klib: KotlinLibrary ->
        val descriptor: ModuleDescriptor = mapping(klib)
        val module: IrModuleFragment = when {
            klibs.included == null -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
            filesToLoad != null && klib == klibs.included -> irLinker.deserializeDirtyFiles(descriptor, klib, filesToLoad)
            filesToLoad != null && klib != klibs.included -> irLinker.deserializeHeadersWithInlineBodies(descriptor, klib)
            klib == klibs.included -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.ALL })
            else -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
        }

        all += module
        when {
            klib.isAnyPlatformStdlib -> stdlib = module
            klib == klibs.included -> included = module
        }
    }

    return IrModuleDependencies(
        all = all,
        stdlib = stdlib,
        included = included,
        fragmentNames = all.getUniqueNameForEachFragment(),
    )
}

fun loadIr(
    modulesStructure: ModulesStructure,
    irFactory: IrFactory,
    filesToLoad: Set<String>? = null,
    loadFunctionInterfacesIntoStdlib: Boolean = false,
): IrModuleInfo {
    val project = modulesStructure.project
    val mainModule = modulesStructure.mainModule
    val configuration = modulesStructure.compilerConfiguration
    val messageLogger = configuration.messageCollector
    val partialLinkageEnabled = configuration.partialLinkageConfig.isEnabled

    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)

    when (mainModule) {
        is MainModule.SourceFiles -> {
            assert(filesToLoad == null)
            val psi2IrContext = preparePsi2Ir(modulesStructure, symbolTable, partialLinkageEnabled)
            val friendModules =
                mapOf(psi2IrContext.moduleDescriptor.name.asString() to modulesStructure.klibs.friends.map { it.uniqueName })

            return getIrModuleInfoForSourceFiles(
                psi2IrContext = psi2IrContext,
                project = project,
                configuration = configuration,
                files = mainModule.files,
                klibs = modulesStructure.klibs,
                friendModules = friendModules,
                symbolTable = symbolTable,
                messageCollector = messageLogger,
                loadFunctionInterfacesIntoStdlib = loadFunctionInterfacesIntoStdlib,
            ) { modulesStructure.getModuleDescriptor(it) }
        }
        is MainModule.Klib -> {
            val mainModuleLib = modulesStructure.klibs.included
                ?: error("No module with ${mainModule.libPath} found")
            val moduleDescriptor = modulesStructure.getModuleDescriptor(mainModuleLib)
            val friendModules = mapOf(mainModuleLib.uniqueName to modulesStructure.klibs.friends.map { it.uniqueName })

            return getIrModuleInfoForKlib(
                moduleDescriptor = moduleDescriptor,
                klibs = modulesStructure.klibs,
                friendModules = friendModules,
                filesToLoad = filesToLoad,
                configuration = configuration,
                symbolTable = symbolTable,
                messageCollector = messageLogger,
                loadFunctionInterfacesIntoStdlib = loadFunctionInterfacesIntoStdlib,
            ) { modulesStructure.getModuleDescriptor(it) }
        }
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun getIrModuleInfoForKlib(
    moduleDescriptor: ModuleDescriptor,
    klibs: LoadedKlibs,
    friendModules: Map<String, List<String>>,
    filesToLoad: Set<String>?,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    messageCollector: MessageCollector,
    loadFunctionInterfacesIntoStdlib: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleInfo {
    val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
    val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)

    val irLinker = JsIrLinker(
        currentModule = null,
        messageCollector = messageCollector,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageSupport = createPartialLinkageSupportForLinker(
            partialLinkageConfig = configuration.partialLinkageConfig,
            builtIns = irBuiltIns,
            messageCollector = messageCollector
        ),
        icData = null,
        friendModules = friendModules
    )

    val moduleDependencies: IrModuleDependencies = deserializeDependencies(
        klibs = klibs,
        irLinker = irLinker,
        filesToLoad = filesToLoad,
        mapping = mapping
    )
    irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
        irBuiltIns,
        symbolTable,
        typeTranslator,
        loadFunctionInterfacesIntoStdlib.ifTrue {
            moduleDependencies.stdlib?.let { stdlibModule -> FunctionTypeInterfacePackages().makePackageAccessor(stdlibModule) }
        },
        true
    )

    irLinker.init(null)
    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(inOrAfterLinkageStep = true)

    return IrModuleInfo(
        module = moduleDependencies.included!!,
        dependencies = moduleDependencies,
        bultins = irBuiltIns,
        symbolTable = symbolTable,
        deserializer = irLinker,
    )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun getIrModuleInfoForSourceFiles(
    psi2IrContext: GeneratorContext,
    project: Project,
    configuration: CompilerConfiguration,
    files: List<KtFile>,
    klibs: LoadedKlibs,
    friendModules: Map<String, List<String>>,
    symbolTable: SymbolTable,
    messageCollector: MessageCollector,
    loadFunctionInterfacesIntoStdlib: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor
): IrModuleInfo {
    val irBuiltIns = psi2IrContext.irBuiltIns
    val irLinker = JsIrLinker(
        currentModule = psi2IrContext.moduleDescriptor,
        messageCollector = messageCollector,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageSupport = createPartialLinkageSupportForLinker(
            partialLinkageConfig = configuration.partialLinkageConfig,
            builtIns = irBuiltIns,
            messageCollector = messageCollector
        ),
        icData = null,
        friendModules = friendModules,
    )

    // Deserialize module fragments preserving the order of libraries in [klibs].
    val moduleDependencies: IrModuleDependencies = deserializeDependencies(
        klibs = klibs,
        irLinker = irLinker,
        filesToLoad = null,
        mapping = mapping
    )
    (irBuiltIns as IrBuiltInsOverDescriptors).functionFactory =
        IrDescriptorBasedFunctionFactory(
            irBuiltIns,
            symbolTable,
            psi2IrContext.typeTranslator,
            loadFunctionInterfacesIntoStdlib.ifTrue {
                moduleDependencies.stdlib?.let { stdlibModule -> FunctionTypeInterfacePackages().makePackageAccessor(stdlibModule) }
            },
            true
        )

    val (moduleFragment, _) = psi2IrContext.generateModuleFragmentWithPlugins(project, files, irLinker, messageCollector)

    if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
        irLinker.modules.forEach { fakeOverrideChecker.check(it) }
    }

    return IrModuleInfo(
        module = moduleFragment,
        dependencies = moduleDependencies,
        bultins = irBuiltIns,
        symbolTable = symbolTable,
        deserializer = irLinker,
    )
}

private fun preparePsi2Ir(
    modulesStructure: ModulesStructure,
    symbolTable: SymbolTable,
    partialLinkageEnabled: Boolean
): GeneratorContext {
    val analysisResult = modulesStructure.jsFrontEndResult
    val psi2Ir = Psi2IrTranslator(
        modulesStructure.compilerConfiguration.languageVersionSettings,
        Psi2IrConfiguration(ignoreErrors = false, partialLinkageEnabled),
        modulesStructure.compilerConfiguration::checkNoUnboundSymbols
    )
    return psi2Ir.createGeneratorContext(
        analysisResult.moduleDescriptor,
        analysisResult.bindingContext,
        symbolTable
    )
}

fun GeneratorContext.generateModuleFragmentWithPlugins(
    project: Project,
    files: List<KtFile>,
    irLinker: IrDeserializer,
    messageCollector: MessageCollector,
    stubGenerator: DeclarationStubGenerator? = null
): Pair<IrModuleFragment, IrPluginContext> {
    val psi2Ir = Psi2IrTranslator(languageVersionSettings, configuration, messageCollector::checkNoUnboundSymbols)

    // plugin context should be instantiated before postprocessing steps
    val pluginContext = IrPluginContextImpl(
        moduleDescriptor,
        bindingContext,
        languageVersionSettings,
        symbolTable,
        typeTranslator,
        irBuiltIns,
        linker = irLinker,
        messageCollector = messageCollector,
    )
    for (extension in IrGenerationExtension.getInstances(project)) {
        psi2Ir.addPostprocessingStep { module ->
            val old = stubGenerator?.unboundSymbolGeneration
            try {
                stubGenerator?.unboundSymbolGeneration = true
                extension.generate(module, pluginContext)
            } finally {
                stubGenerator?.unboundSymbolGeneration = old!!
            }
        }
    }

    return psi2Ir.generateModuleFragment(this, files, listOf(stubGenerator ?: irLinker)) to pluginContext
}

private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}
public val JsFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)

fun getModuleDescriptorByLibrary(current: KotlinLibrary, mapping: Map<String, ModuleDescriptorImpl>): ModuleDescriptorImpl {
    val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
        current,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        null,
        packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
        lookupTracker = LookupTracker.DO_NOTHING
    )
//    if (isBuiltIns) runtimeModule = md

    val dependencies = current.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { mapping.getValue(it) }

    md.setDependencies(listOf(md) + dependencies)
    return md
}

private const val FILE_FINGERPRINTS_SEPARATOR = " "

private fun List<SerializedIrFileFingerprint>.joinIrFileFingerprints(): String {
    return joinToString(FILE_FINGERPRINTS_SEPARATOR)
}

private fun String.parseSerializedIrFileFingerprints(): List<SerializedIrFileFingerprint> {
    return split(FILE_FINGERPRINTS_SEPARATOR).mapNotNull(SerializedIrFileFingerprint::fromString)
}

fun serializeModuleIntoKlib(
    moduleName: String,
    configuration: CompilerConfiguration,
    diagnosticReporter: DiagnosticReporter,
    metadataSerializer: KlibSingleFileMetadataSerializer<*>,
    klibPath: String,
    dependencies: List<KotlinLibrary>,
    moduleFragment: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    cleanFiles: List<KotlinFileSerializedData>,
    nopack: Boolean,
    containsErrorCode: Boolean = false,
    jsOutputName: String?,
    builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS,
    wasmTarget: WasmTarget? = null,
) {
    val moduleExportedNames = moduleFragment.collectExportedNames()
    val incrementalResultsConsumer = configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    val empty = ByteArray(0)
    val serializerOutput = serializeModuleIntoKlib(
        moduleName = moduleFragment.name.asString(),
        irModuleFragment = moduleFragment,
        configuration = configuration,
        diagnosticReporter = diagnosticReporter,
        cleanFiles = cleanFiles,
        dependencies = dependencies,
        createModuleSerializer = { irDiagnosticReporter ->
            JsIrModuleSerializer(
                settings = IrSerializationSettings(configuration),
                irDiagnosticReporter,
                irBuiltIns,
            ) { JsIrFileMetadata(moduleExportedNames[it]?.values?.toSmartList() ?: emptyList()) }
        },
        metadataSerializer = metadataSerializer,
        platformKlibCheckers = listOfNotNull(
            { irDiagnosticReporter: IrDiagnosticReporter ->
                val cleanFilesIrData = cleanFiles.map { it.irData ?: error("Metadata-only KLIBs are not supported in Kotlin/JS") }
                JsKlibCheckers.makeChecker(
                    irDiagnosticReporter,
                    configuration,
                    // Should IrInlinerBeforeKlibSerialization be set, then calls should have already been checked during pre-serialization,
                    // and there's no need to raise duplicates of those warnings here.
                    doCheckCalls = !configuration.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization),
                    doModuleLevelChecks = true,
                    cleanFilesIrData,
                    moduleExportedNames,
                )
            }.takeIf { builtInsPlatform == BuiltInsPlatform.JS  }
        ),
        processCompiledFileData = { ioFile, compiledFile ->
            incrementalResultsConsumer?.run {
                processPackagePart(ioFile, compiledFile.metadata, empty, empty)
                with(compiledFile.irData!!) {
                    processIrFile(
                        ioFile,
                        fileData,
                        types,
                        signatures,
                        strings,
                        declarations,
                        inlineDeclarations,
                        bodies,
                        fqName.toByteArray(),
                        fileMetadata,
                        debugInfo,
                        fileEntries,
                    )
                }
            }
        },
        processKlibHeader = {
            incrementalResultsConsumer?.processHeader(it)
        },
    )

    val fullSerializedIr = serializerOutput.serializedIr ?: error("Metadata-only KLIBs are not supported in Kotlin/JS")

    val versions = KotlinLibraryVersioning(
        compilerVersion = KotlinCompilerVersion.VERSION,
        abiVersion = configuration.klibAbiVersionForManifest(),
        metadataVersion = configuration.klibMetadataVersionOrDefault()
    )

    val properties = Properties().also { p ->
        if (jsOutputName != null) {
            p.setProperty(KLIB_PROPERTY_JS_OUTPUT_NAME, jsOutputName)
        }
        val wasmTargets = listOfNotNull(/* in the future there might be multiple WASM targets */ wasmTarget)
        if (wasmTargets.isNotEmpty()) {
            p.setProperty(KLIB_PROPERTY_WASM_TARGETS, wasmTargets.joinToString(" ") { it.alias })
        }
        if (containsErrorCode) {
            p.setProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE, "true")
        }

        val fingerprints = fullSerializedIr.files.sortedBy { it.path }.map { SerializedIrFileFingerprint(it) }
        p.setProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS, fingerprints.joinIrFileFingerprints())
        p.setProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT, SerializedKlibFingerprint(fingerprints).klibFingerprint.toString())

        addLanguageFeaturesToManifest(p, configuration.languageVersionSettings)
    }

    buildKotlinLibrary(
        linkDependencies = serializerOutput.neededLibraries,
        ir = fullSerializedIr,
        metadata = serializerOutput.serializedMetadata ?: error("expected serialized metadata"),
        manifestProperties = properties,
        moduleName = moduleName,
        nopack = nopack,
        output = klibPath,
        versions = versions,
        builtInsPlatform = builtInsPlatform
    )
}

const val KLIB_PROPERTY_JS_OUTPUT_NAME = "jsOutputName"
const val KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS = "serializedIrFileFingerprints"
const val KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT = "serializedKlibFingerprint"

fun <SourceFile> shouldGoToNextIcRound(
    compilerConfiguration: CompilerConfiguration,
    createMetadataSerializer: () -> KlibSingleFileMetadataSerializer<SourceFile>,
): Boolean {
    val nextRoundChecker = compilerConfiguration.get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return false
    createMetadataSerializer().run {
        forEachFile { _, sourceFile, ktSourceFile, _ ->
            val protoBuf = serializeSingleFileMetadata(sourceFile)
            // to minimize the number of IC rounds, we should inspect all proto for changes first,
            // then go to the next round if needed, with all new dirty files
            nextRoundChecker.checkProtoChanges(ktSourceFile.toIoFileOrNull()!!, protoBuf.toByteArray())
        }
    }
    return nextRoundChecker.shouldGoToNextRound()
}

private fun List<IrModuleFragment>.getUniqueNameForEachFragment(): Map<IrModuleFragment, String> {
    return this.mapNotNull { moduleFragment ->
        moduleFragment.kotlinLibrary?.jsOutputName?.let { moduleFragment to it }
    }.toMap()
}

fun IncrementalDataProvider.getSerializedData(newSources: List<KtSourceFile>): List<KotlinFileSerializedData> {
    val nonCompiledSources = newSources.associateBy { it.toIoFileOrNull()!! }
    val compiledIrFiles = serializedIrFiles
    val compiledMetaFiles = compiledPackageParts

    assert(compiledIrFiles.size == compiledMetaFiles.size)

    val storage = mutableListOf<KotlinFileSerializedData>()

    for (f in compiledIrFiles.keys) {
        if (f in nonCompiledSources) continue

        val irData = compiledIrFiles[f] ?: error("No Ir Data found for file $f")
        val metaFile = compiledMetaFiles[f] ?: error("No Meta Data found for file $f")
        val irFile = with(irData) {
            SerializedIrFile(
                fileData,
                String(fqn),
                f.path.replace('\\', '/'),
                types,
                signatures,
                strings,
                bodies,
                declarations,
                inlineDeclarations,
                debugInfo,
                fileMetadata,
                fileEntries,
            )
        }
        storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
    }
    return storage
}

@JvmName("getSerializedDataByPsiFiles")
fun IncrementalDataProvider.getSerializedData(newSources: List<KtFile>): List<KotlinFileSerializedData> =
    getSerializedData(newSources.map(::KtPsiSourceFile))
