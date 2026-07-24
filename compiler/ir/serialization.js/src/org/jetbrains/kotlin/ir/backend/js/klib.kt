/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.*
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.addMetadataFlagsToManifest
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.metadataVersion
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.toSmartList
import java.io.File
import java.nio.file.Path
import java.util.Properties

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

/**
 * Note: This function returns the list of the deserialized [IrModuleFragment]s that has exactly the same
 * order as the libraries in [klibs].
 */
private fun deserializeDependencies(
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
): IrModuleInfo {
    val configuration = modulesStructure.compilerConfiguration

    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)

    val mainModuleLib = modulesStructure.klibs.included
        ?: error("No module with ${modulesStructure.mainModulePath} found")
    val moduleDescriptor = modulesStructure.getModuleDescriptor(mainModuleLib)
    val friendModules = mapOf(mainModuleLib.uniqueName to modulesStructure.klibs.friends.map { it.uniqueName })

    return getIrModuleInfoForKlib(
        moduleDescriptor = moduleDescriptor,
        klibs = modulesStructure.klibs,
        friendModules = friendModules,
        configuration = configuration,
        symbolTable = symbolTable,
    ) { modulesStructure.getModuleDescriptor(it) }
}

fun loadIrForSingleModule(
    modulesStructure: ModulesStructure,
    irFactory: IrFactory,
): IrModuleInfo {
    val configuration = modulesStructure.compilerConfiguration

    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)

    val mainModuleLib = modulesStructure.klibs.included
        ?: error("No module with ${modulesStructure.mainModulePath} found")
    val friendModules = mapOf(mainModuleLib.uniqueName to modulesStructure.klibs.friends.map { it.uniqueName })
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        configuration.diagnosticsCollector,
        configuration.languageVersionSettings,
    )

    val irLinker = JsIrLinker(
        configuration = configuration,
        symbolTable = symbolTable,
        partialLinkageConfig = configuration.partialLinkageConfig,
        irDiagnosticReporter = irDiagnosticReporter,
        friendModules = friendModules
    )

    var stdlibFragment: IrModuleFragment? = null
    var mainFragment: IrModuleFragment? = null
    val deserializedFragments = modulesStructure.klibs.all.map { klib ->
        val moduleDescriptor = modulesStructure.getModuleDescriptor(klib)
        val fragment = if (klib == modulesStructure.klibs.included) {
            irLinker.deserializeFullModule(moduleDescriptor, klib)
        } else {
            irLinker.deserializeHeadersWithInlineBodies(moduleDescriptor, klib)
        }

        if (klib == modulesStructure.klibs.included) {
            mainFragment = fragment
        }
        if (klib.isWasmStdlib) {
            stdlibFragment = fragment
        }

        fragment
    }

    check(mainFragment != null)
    check(stdlibFragment != null)

    irLinker.init(null)

    @OptIn(InternalSymbolFinderAPI::class)
    val irBuiltIns = IrBuiltInsForLinker(irLinker, configuration.languageVersionSettings)

    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(irBuiltIns, inOrAfterLinkageStep = true)

    val isStdlibCompilation = mainFragment == stdlibFragment

    val moduleDependencies = IrModuleDependencies(
        all = deserializedFragments,
        stdlib = stdlibFragment.takeIf { !isStdlibCompilation },
        included = mainFragment,
        fragmentNames = deserializedFragments.getUniqueNameForEachFragment(),
    )

    // Hack (KT-71039, restored after KT-78040) - pre-load functional interfaces in case if IrLoader cut its count
    // `WasmAddFunctionSupertypeToSuspendFunctionLowering` of Kotlin/Wasm backend
    // adds `Function<...>` supertypes to `SuspendFunction<...>` interfaces.
    // Since these supertypes appear only at the lowering stage,
    // after klib deserialization, the linker / Incremental
    // Compilation never sees the `FunctionN`/`KFunctionN` builtins as used
    // and therefore never loads or serializes them.
    // Referencing `functionN(it)`/`kFunctionN(it)` here forces their definitions
    // to be materialized up front.
    if (isStdlibCompilation) {
        repeat(25) {
            irBuiltIns.functionN(it)
            irBuiltIns.kFunctionN(it)
        }
    }


    return IrModuleInfo(
        module = mainFragment,
        dependencies = moduleDependencies,
        bultins = irBuiltIns,
        symbolTable = symbolTable,
        deserializer = irLinker,
    )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun getIrModuleInfoForKlib(
    moduleDescriptor: ModuleDescriptor,
    klibs: LoadedKlibs,
    friendModules: Map<String, List<String>>,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    mapping: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleInfo {
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        configuration.diagnosticsCollector,
        configuration.languageVersionSettings,
    )

    val irLinker = JsIrLinker(
        configuration = configuration,
        symbolTable = symbolTable,
        partialLinkageConfig = configuration.partialLinkageConfig,
        irDiagnosticReporter = irDiagnosticReporter,
        friendModules = friendModules
    )

    // Deserialize module fragments preserving the order of libraries in `klibs.all`.
    val moduleDependencies: IrModuleDependencies = deserializeDependencies(
        klibs = klibs,
        irLinker = irLinker,
        filesToLoad = configuration[JSConfigurationKeys.IC_FILES_TO_LOAD],
        mapping = mapping
    )

    irLinker.init(null)

    @OptIn(InternalSymbolFinderAPI::class)
    val irBuiltIns = IrBuiltInsForLinker(irLinker, configuration.languageVersionSettings)

    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(irBuiltIns, inOrAfterLinkageStep = true)

    return IrModuleInfo(
        module = moduleDependencies.included!!,
        dependencies = moduleDependencies,
        bultins = irBuiltIns,
        symbolTable = symbolTable,
        deserializer = irLinker,
    )
}

private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}

@OptIn(K1Deprecation::class)
val JsFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)

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
    diagnosticReporter: IrDiagnosticReporter,
    metadataSerializer: KlibSingleFileMetadataSerializer<*>,
    klibPath: Path,
    moduleFragment: IrModuleFragment,
    irBuiltIns: IrBuiltIns,
    cleanFiles: List<KotlinFileSerializedData>,
    nopack: Boolean,
    jsOutputName: String?,
    builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS,
    wasmTarget: WasmTarget? = null,
    performanceManager: PerformanceManager? = null
) {
    val moduleJsExportNames = moduleFragment.collectJsExportNames()
    val incrementalResultsConsumer = configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    val empty = ByteArray(0)
    val serializerOutput = performanceManager.tryMeasurePhaseTime(PhaseType.IrSerialization) {
        serializeModuleIntoKlib(
            moduleName = moduleFragment.name.asString(),
            irModuleFragment = moduleFragment,
            configuration = configuration,
            diagnosticReporter = diagnosticReporter,
            cleanFiles = cleanFiles,
            dependencies = emptyList(),
            createModuleSerializer = { irDiagnosticReporter ->
                JsIrModuleSerializer(
                    settings = IrSerializationSettings(configuration),
                    irDiagnosticReporter,
                    irBuiltIns,
                ) { JsIrFileMetadata(moduleJsExportNames[it]?.values?.toSmartList() ?: emptyList()) }
            },
            metadataSerializer = metadataSerializer,
            processCompiledFileData = incrementalResultsConsumer?.let { icConsumer ->
                { ioFile, compiledFile ->
                    icConsumer.processPackagePart(ioFile, compiledFile.metadata, empty, empty)
                    with(compiledFile.irData!!) {
                        icConsumer.processIrFile(
                            ioFile,
                            fileData,
                            types,
                            signatures,
                            strings,
                            declarations,
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
    }
    val fullSerializedIr = serializerOutput.serializedIr ?: error("Metadata-only KLIBs are not supported in Kotlin/JS")

    val versions = KotlinLibraryVersioning(
        compilerVersion = KotlinCompilerVersion.VERSION,
        abiVersion = configuration.klibAbiVersionForManifest(),
        metadataVersion = configuration.metadataVersion()
    )

    val properties = Properties().also { p ->
        if (jsOutputName != null) {
            p.setProperty(KLIB_PROPERTY_JS_OUTPUT_NAME, jsOutputName)
        }

        val fingerprints = fullSerializedIr.files.sortedBy { it.path }.map { SerializedIrFileFingerprint(it) }
        p.setProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS, fingerprints.joinIrFileFingerprints())
        p.setProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT, SerializedKlibFingerprint(fingerprints).klibFingerprint.toString())

        addLanguageFeaturesToManifest(p, configuration.languageVersionSettings)
        addMetadataFlagsToManifest(p, configuration.languageVersionSettings)
    }

    performanceManager.tryMeasurePhaseTime(PhaseType.KlibWriting) {
        KlibWriter {
            format(if (nopack) KlibFormat.Directory else KlibFormat.ZipArchive)
            manifest {
                moduleName(moduleName)
                versions(versions)
                platformAndTargets(
                    builtInsPlatform = builtInsPlatform,
                    targetNames = if (builtInsPlatform == BuiltInsPlatform.WASM)
                        listOfNotNull(/* in the future there might be multiple WASM targets */wasmTarget?.alias)
                    else
                        emptyList()
                )
                customProperties { this += properties }
            }
            includeMetadata(serializerOutput.serializedMetadata ?: error("expected serialized metadata"))
            includeIr(fullSerializedIr)
        }.writeTo(klibPath)
    }
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
        forEachFile { _, ioFile, sourceFile, _, _ ->
            val protoBuf = serializeSingleFileMetadata(sourceFile)
            // to minimize the number of IC rounds, we should inspect all proto for changes first,
            // then go to the next round if needed, with all new dirty files
            nextRoundChecker.checkProtoChanges(ioFile, protoBuf.toByteArray())
        }
    }
    return nextRoundChecker.shouldGoToNextRound()
}

private fun List<IrModuleFragment>.getUniqueNameForEachFragment(): Map<IrModuleFragment, String> {
    return this.mapNotNull { moduleFragment ->
        moduleFragment.kotlinLibrary?.jsOutputName?.let { moduleFragment to it }
    }.toMap()
}

fun IncrementalDataProvider.getSerializedData(nonCompiledSources: Set<File>): List<KotlinFileSerializedData> {
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
    getSerializedData(newSources.mapNotNullTo(mutableSetOf()) { KtPsiSourceFile(it).toIoFileOrNull() })
