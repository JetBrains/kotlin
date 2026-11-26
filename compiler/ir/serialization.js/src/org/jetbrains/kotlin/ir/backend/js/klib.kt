/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.parseTreeKind
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.incrementalResultsConsumer
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.metadata.addMetadataFlagsToManifest
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.metadataVersion
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.io.File
import java.nio.file.Path
import java.util.*

val KotlinLibrary.moduleName: String
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)

val KotlinLibrary.serializedIrFileFingerprints: List<SerializedIrFileFingerprint>?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS)?.parseSerializedIrFileFingerprints()

val KotlinLibrary.serializedKlibFingerprint: SerializedKlibFingerprint?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT)?.let { SerializedKlibFingerprint.fromString(it) }

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
    val dependencies = klibs.all.map { klib: KotlinLibrary ->
        val descriptor: ModuleDescriptor = mapping(klib)
        val deserializationStrategy: DeserializationStrategyResolver = when {
            klibs.included == null -> DeserializationStrategy.EXPLICITLY_EXPORTED
            filesToLoad != null && klib == klibs.included -> { filePath: String ->
                if (filePath in filesToLoad) DeserializationStrategy.ALL else DeserializationStrategy.WITH_INLINE_BODIES
            }
            filesToLoad != null && klib != klibs.included -> DeserializationStrategy.WITH_INLINE_BODIES
            klib == klibs.included -> DeserializationStrategy.ALL
            else -> DeserializationStrategy.EXPLICITLY_EXPORTED
        }

        irLinker.createAndRegisterModuleDeserializer(descriptor, klib, deserializationStrategy).moduleFragment
    }

    return IrModuleDependencies(
        allDependencies = dependencies,
        fragmentNames = dependencies.getUniqueNameForEachFragment(),
    )
}

fun loadIr(
    modulesStructure: ModulesStructure,
    irFactory: IrFactory,
): IrModuleInfo {
    val configuration = modulesStructure.compilerConfiguration

    val symbolTable = SymbolTable(signaturer = null, irFactory)

    val mainModuleLib = modulesStructure.klibs.included
        ?: error("No module with ${modulesStructure.mainModulePath} found")
    val friendModules = mapOf(mainModuleLib to modulesStructure.klibs.friends)

    return getIrModuleInfoForKlib(
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

    val symbolTable = SymbolTable(signaturer = null, irFactory)

    val mainModuleLib = modulesStructure.klibs.included
        ?: error("No module with ${modulesStructure.mainModulePath} found")
    val friendModules = mapOf(mainModuleLib to modulesStructure.klibs.friends)
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        configuration.diagnosticsCollector,
        configuration.languageVersionSettings,
        configuration.parseTreeKind,
    )

    val irLinker = JsIrLinker(
        configuration = configuration,
        symbolTable = symbolTable,
        irDiagnosticReporter = irDiagnosticReporter,
        friendModules = friendModules
    )

    var stdlibFragment: IrModuleFragment? = null
    var mainFragment: IrModuleFragment? = null
    val deserializedFragments = modulesStructure.klibs.all.map { klib ->
        val moduleDescriptor = modulesStructure.getModuleDescriptor(klib)
        val deserializationStrategy =
            if (klib == modulesStructure.klibs.included) DeserializationStrategy.ALL else DeserializationStrategy.WITH_INLINE_BODIES

        val fragment = irLinker.createAndRegisterModuleDeserializer(moduleDescriptor, klib, deserializationStrategy).moduleFragment

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

    @OptIn(InternalSymbolFinderAPI::class)
    val irBuiltIns = IrBuiltInsForLinker(irLinker, configuration.languageVersionSettings)

    irLinker.drainDeserializationQueue()
    irLinker.finalizeIrLinkage(irBuiltIns)

    val isStdlibCompilation = mainFragment == stdlibFragment

    val moduleDependencies = IrModuleDependencies(
        allDependencies = deserializedFragments,
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
    klibs: LoadedKlibs,
    friendModules: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    mapping: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleInfo {
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        configuration.diagnosticsCollector,
        configuration.languageVersionSettings,
        configuration.parseTreeKind,
    )

    val irLinker = JsIrLinker(
        configuration = configuration,
        symbolTable = symbolTable,
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

    @OptIn(InternalSymbolFinderAPI::class)
    val irBuiltIns = IrBuiltInsForLinker(irLinker, configuration.languageVersionSettings)

    irLinker.drainDeserializationQueue()
    irLinker.finalizeIrLinkage(irBuiltIns)

    return IrModuleInfo(
        module = moduleDependencies.allDependencies.single { it.kotlinLibrary == klibs.included },
        dependencies = moduleDependencies,
        bultins = irBuiltIns,
        symbolTable = symbolTable,
        deserializer = irLinker,
    )
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
    val incrementalResultsConsumer = configuration.incrementalResultsConsumer
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
                )
            },
            metadataSerializer = metadataSerializer,
            processCompiledFileData = incrementalResultsConsumer?.let { icConsumer ->
                fun SerializedIrFile.processIrFile(ioFile: File, process: ProcessFun) {
                    process(
                        ioFile,
                        fileData,
                        types,
                        signatures,
                        strings,
                        declarations,
                        bodies,
                        fqName.toByteArray(),
                        debugInfo,
                        fileEntries,
                    )
                }

                { ioFile, compiledFile, inlineIds ->
                    icConsumer.processPackagePart(ioFile, compiledFile.metadata)
                    compiledFile.irData!!.processIrFile(ioFile, icConsumer::processIrFile)
                    compiledFile.irInlineData?.processIrFile(ioFile, icConsumer::processIrInlineFile)
                    icConsumer.processIrInlineIds(ioFile, inlineIds)
                    compiledFile.signatureIndex?.let { icConsumer.processSignatureIndex(ioFile, it) }
                }
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
            includeMetadata(serializerOutput.serializedMetadata)
            includeIr(fullSerializedIr)
        }.writeTo(klibPath)
    }
}

const val KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS = "serializedIrFileFingerprints"
const val KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT = "serializedKlibFingerprint"

private fun List<IrModuleFragment>.getUniqueNameForEachFragment(): Map<IrModuleFragment, String> {
    return this.mapNotNull { moduleFragment ->
        moduleFragment.kotlinLibrary?.jsOutputName?.let { moduleFragment to it }
    }.toMap()
}
