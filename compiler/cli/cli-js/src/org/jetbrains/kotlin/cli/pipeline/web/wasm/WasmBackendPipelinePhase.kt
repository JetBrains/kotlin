/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.backend.js.jsOutputName
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.backend.js.loadIrForSingleModule
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File
import java.net.URLEncoder

fun getAllReferencedDeclarations(
    wasmCompiledFileFragment: WasmCompiledFileFragment,
    additionalSignatureToImport: Set<IdSignature>
): Set<IdSignature> {
    val signatures = mutableSetOf<IdSignature>()
    signatures.addAll(wasmCompiledFileFragment.functions.unbound.keys)
    signatures.addAll(wasmCompiledFileFragment.globalFields.unbound.keys)
    signatures.addAll(wasmCompiledFileFragment.globalVTables.unbound.keys)
    signatures.addAll(wasmCompiledFileFragment.globalClassITables.unbound.keys)
    wasmCompiledFileFragment.rttiElements?.let {
        signatures.addAll(it.globalReferences.unbound.keys)
    }
    signatures.addAll(additionalSignatureToImport)
    return signatures
}

private val IrModuleFragment.outputFileName
    get() = kotlinLibrary?.jsOutputName ?: (name.asString()
        .replace("<", "_")
        .replace(">", "_")
        .let { URLEncoder.encode(it, "UTF-8").replace("%", "%25") })

object WasmBackendPipelinePhase : WebBackendPipelinePhase<WasmBackendPipelineArtifact>("WasmBackendPipelinePhase") {
    override val configFiles: EnvironmentConfigFiles
        get() = EnvironmentConfigFiles.WASM_CONFIG_FILES

    override fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
    ): WasmBackendPipelineArtifact? {
        val outputDir = configuration.outputDir!!
        val result = compileIncrementally(
            icCaches = icCaches,
            configuration = configuration,
            moduleName = configuration.moduleName!!,
            outputDir = outputDir,
            outputName = configuration.outputName!!,
            preserveIcOrder = configuration.preserveIcOrder,
            wasmDebug = configuration.getBoolean(WasmConfigurationKeys.WASM_DEBUG),
            wasmGenerateWat = configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT),
            generateDwarf = configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_DWARF)
        )
        return WasmBackendPipelineArtifact(result, outputDir, configuration)
    }

    internal fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
        moduleName: String,
        outputDir: File,
        outputName: String,
        preserveIcOrder: Boolean,
        wasmDebug: Boolean,
        wasmGenerateWat: Boolean,
        generateDwarf: Boolean
    ): WasmCompilerResult {
        val wasmArtifacts = icCaches.artifacts
            .filterIsInstance<WasmModuleArtifact>()
            .flatMap { it.fileArtifacts }
            .mapNotNull { it.loadIrFragments()?.mainFragment }
            .let { fragments -> if (preserveIcOrder) fragments.sortedBy { it.fragmentTag } else fragments }

        val useDebuggerCustomFormatters = configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)

        val res = compileWasm(
            wasmCompiledFileFragments = wasmArtifacts,
            moduleName = moduleName,
            configuration = configuration,
            typeScriptFragment = null,
            baseFileName = outputName,
            emitNameSection = wasmDebug,
            generateWat = wasmGenerateWat,
            generateDwarf = generateDwarf,
            generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP),
            useDebuggerCustomFormatters = useDebuggerCustomFormatters
        )

        writeCompilationResult(
            result = res,
            dir = outputDir,
            fileNameBase = outputName
        )
        return res
    }

    override fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        mainCallArguments: List<String>?,
    ): WasmBackendPipelineArtifact? {
        val outputDir = configuration.outputDir!!
        val result = compileNonIncrementally(
            configuration,
            module,
            configuration.outputName!!,
            outputDir,
            propertyLazyInitialization = configuration.propertyLazyInitialization,
            dce = configuration.dce,
            configuration[WasmConfigurationKeys.DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE],
            generateDwarf = configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_DWARF),
            wasmDebug = configuration.getBoolean(WasmConfigurationKeys.WASM_DEBUG),
        )
        return WasmBackendPipelineArtifact(result, outputDir, configuration)
    }

    internal fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        outputName: String,
        outputDir: File,
        propertyLazyInitialization: Boolean,
        dce: Boolean,
        dceDumpDeclarationIrSizesToFile: String?,
        wasmDebug: Boolean,
        generateDwarf: Boolean
    ): WasmCompilerResult {
        return if (!configuration.getBoolean(WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY)) {
            compileWholeProgramMode(
                configuration = configuration,
                module = module,
                outputName = outputName,
                outputDir = outputDir,
                propertyLazyInitialization = propertyLazyInitialization,
                dce = dce,
                dceDumpDeclarationIrSizesToFile = dceDumpDeclarationIrSizesToFile,
                wasmDebug = wasmDebug,
                generateDwarf = generateDwarf,
            )
        } else {
            compileSingleModule(
                configuration = configuration,
                module = module,
                outputDir = outputDir,
                wasmDebug = wasmDebug,
            )
        }
    }

    internal fun compileWholeProgramMode(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        outputName: String,
        outputDir: File,
        propertyLazyInitialization: Boolean,
        dce: Boolean,
        dceDumpDeclarationIrSizesToFile: String?,
        wasmDebug: Boolean,
        generateDwarf: Boolean
    ): WasmCompilerResult {
        val performanceManager = configuration.perfManager
        performanceManager?.let {
            @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
            it.notifyCurrentPhaseFinishedIfNeeded() // TODO: KT-75227
            it.notifyPhaseStarted(PhaseType.TranslationToIr)
        }

        val generateDts = configuration.getBoolean(JSConfigurationKeys.GENERATE_DTS)
        val generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)
        val useDebuggerCustomFormatters = configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)

        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val irModuleInfo = loadIr(
            modulesStructure = module,
            irFactory = irFactory,
            loadFunctionInterfacesIntoStdlib = true,
        )

        val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
            irModuleInfo,
            module.mainModule,
            configuration,
            performanceManager,
            generateTypeScriptFragment = generateDts,
            propertyLazyInitialization = propertyLazyInitialization,
        )

        performanceManager.tryMeasurePhaseTime(PhaseType.Backend) {
            val dceDumpNameCache = DceDumpNameCache()
            if (dce) {
                eliminateDeadDeclarations(allModules, backendContext, dceDumpNameCache)
            }

            dumpDeclarationIrSizesIfNeed(dceDumpDeclarationIrSizesToFile, allModules, dceDumpNameCache)

            val generateWat = configuration.get(WasmConfigurationKeys.WASM_GENERATE_WAT, false)

            val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
            val codeGenerator = WasmModuleFragmentGenerator(
                backendContext,
                wasmModuleMetadataCache,
                irFactory,
                allowIncompleteImplementations = dce,
                skipCommentInstructions = !generateWat,
            )
            val wasmCompiledFileFragments = allModules.map { codeGenerator.generateModuleAsSingleFileFragment(it) }

            val res = compileWasm(
                wasmCompiledFileFragments = wasmCompiledFileFragments,
                moduleName = allModules.last().descriptor.name.asString(),
                configuration = configuration,
                typeScriptFragment = typeScriptFragment,
                baseFileName = outputName,
                emitNameSection = wasmDebug,
                generateWat = generateWat,
                generateDwarf = generateDwarf,
                generateSourceMaps = generateSourceMaps,
                useDebuggerCustomFormatters = useDebuggerCustomFormatters
            )

            writeCompilationResult(
                result = res,
                dir = outputDir,
                fileNameBase = outputName
            )

            return res
        }
    }

    private fun parseDependencyResolutionMap(configuration: CompilerConfiguration)
            : Map<String, String> {

        val rawResolutionMap = configuration[WasmConfigurationKeys.WASM_DEPENDENCY_RESOLUTION_MAP] ?: return emptyMap()

        val parsedResolutionMap = rawResolutionMap.split(",")
            .map { it.split(":") }
            .associate { it[0] to it[1] }

        return parsedResolutionMap
    }

    private fun compileSingleModule(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        outputDir: File,
        wasmDebug: Boolean,
    ): WasmCompilerResult {

        val dependencyResolutionMap = parseDependencyResolutionMap(configuration)

        val performanceManager = configuration.perfManager

        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val irModuleInfo = loadIrForSingleModule(
            modulesStructure = module,
            irFactory = irFactory,
        )

        val (allModules, backendContext, _) = compileToLoweredIr(
            irModuleInfo,
            module.mainModule,
            configuration,
            performanceManager,
            exportedDeclarations = setOf(FqName("main")),
            generateTypeScriptFragment = false,
            propertyLazyInitialization = configuration.propertyLazyInitialization,
            disableCrossFileOptimisations = true,
        )

        performanceManager.tryMeasurePhaseTime(PhaseType.Backend) {

            val compilationResult = compileWasmLoweredFragmentsForSingleModule(
                configuration = configuration,
                loweredIrFragments = allModules,
                backendContext = backendContext,
                signatureRetriever = irFactory,
                stdlibIsMainModule = module.klibs.included?.isWasmStdlib == true,
                generateWat = configuration.get(WasmConfigurationKeys.WASM_GENERATE_WAT, false),
                wasmDebug = wasmDebug,
                dependencyResolutionMap = dependencyResolutionMap,
            )

            writeCompilationResult(
                result = compilationResult,
                dir = outputDir,
                fileNameBase = allModules.last().outputFileName,
            )

            return compilationResult
        }
    }
}

fun compileWasmLoweredFragmentsForSingleModule(
    configuration: CompilerConfiguration,
    loweredIrFragments: List<IrModuleFragment>,
    backendContext: WasmBackendContext,
    signatureRetriever: IdSignatureRetriever,
    stdlibIsMainModule: Boolean,
    generateWat: Boolean,
    wasmDebug: Boolean,
    outputFileNameBase: String? = null,
    dependencyResolutionMap: Map<String, String>,
): WasmCompilerResult {
    val mainModuleFragment = backendContext.irModuleFragment
    val moduleName = mainModuleFragment.name.asString()

    val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
    val codeGenerator = WasmModuleFragmentGenerator(
        backendContext,
        wasmModuleMetadataCache,
        signatureRetriever,
        allowIncompleteImplementations = false,
        skipCommentInstructions = !generateWat,
    )

    val wasmCompiledFileFragments = mutableListOf<WasmCompiledFileFragment>()
    val dependencyImports = mutableSetOf<WasmModuleDependencyImport>()

    val mainModuleFileFragment = codeGenerator.generateModuleAsSingleFileFragmentWithModuleExport(mainModuleFragment)

    // This signature needed to dynamically load module services
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val additionalSignatureToImport =
        if (stdlibIsMainModule) emptySet() else setOf(
            signatureRetriever.declarationSignature(backendContext.wasmSymbols.registerModuleDescriptor.owner)!!,
            signatureRetriever.declarationSignature(backendContext.wasmSymbols.createString.owner)!!,
            signatureRetriever.declarationSignature(backendContext.wasmSymbols.tryGetAssociatedObject.owner)!!,
        )

    val importedDeclarations = getAllReferencedDeclarations(mainModuleFileFragment, additionalSignatureToImport)

    val dependencyModules = loweredIrFragments.filterNot { it == mainModuleFragment }
    dependencyModules.mapTo(wasmCompiledFileFragments) {
        val dependencyName = it.name.asString()
        val initialOutputFileName = it.outputFileName

        dependencyImports.add(
            WasmModuleDependencyImport(
                dependencyName,
                dependencyResolutionMap[dependencyName]
                    ?: initialOutputFileName
            )
        )
        codeGenerator.generateModuleAsSingleFileFragmentWithModuleImport(it, dependencyName, importedDeclarations)
    }
    wasmCompiledFileFragments.add(mainModuleFileFragment)

    val stdlibModuleNameForImport =
        loweredIrFragments.first().name.asString().takeIf { !stdlibIsMainModule }

    val useDebuggerCustomFormatters =
        configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS) &&
                stdlibModuleNameForImport == null

    return compileWasm(
        wasmCompiledFileFragments = wasmCompiledFileFragments,
        moduleName = moduleName,
        configuration = configuration,
        typeScriptFragment = null,
        baseFileName = outputFileNameBase ?: mainModuleFragment.outputFileName,
        emitNameSection = wasmDebug,
        generateWat = generateWat,
        generateSourceMaps = false,
        generateDwarf = false,
        useDebuggerCustomFormatters = useDebuggerCustomFormatters,
        stdlibModuleNameForImport = stdlibModuleNameForImport,
        dependencyModules = dependencyImports,
        initializeUnit = stdlibIsMainModule,
    )
}