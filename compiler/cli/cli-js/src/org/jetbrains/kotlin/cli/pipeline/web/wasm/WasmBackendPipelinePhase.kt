/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getAllReferencedDeclarations
import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.backend.js.loadIrForSingleModule
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File
import kotlin.text.get

val IrModuleFragment.outputFileName
    get() = name.asString().replace("<", "_").replace(">", "_")

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
        val singleModuleName = configuration.get(WasmConfigurationKeys.SINGLE_MODULE_MODE)
        return if (singleModuleName == null) {
            compileNormalMode(
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
            val selectedModule = module.klibs.all.firstOrNull {
                it.moduleName == singleModuleName
            } ?: error("Module $singleModuleName not found")

            val dependedModules = module.klibs.all.takeWhile { it != selectedModule }

            val moduleWithSelectedKLibOnTop = ModulesStructure(
                project = module.project,
                mainModule = MainModule.Klib(selectedModule.libraryFile.path),
                compilerConfiguration = module.compilerConfiguration,
                klibs = LoadedKlibs(dependedModules + selectedModule, included = selectedModule)
            )

            compileSingleModule(
                configuration = configuration,
                module = moduleWithSelectedKLibOnTop,
                outputDir = outputDir,
                wasmDebug = wasmDebug,
            )
        }
    }

    internal fun compileNormalMode(
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

    private fun compileSingleModule(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        outputDir: File,
        wasmDebug: Boolean,
    ): WasmCompilerResult {
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
            isIncremental = true,
        )

        performanceManager.tryMeasurePhaseTime(PhaseType.Backend) {
            val compilationResult = compileWasmLoweredFragmentsForSingleModule(
                configuration = configuration,
                loweredIrFragments = allModules,
                backendContext = backendContext,
                signatureRetriever = irFactory,
                generateWat = configuration.get(WasmConfigurationKeys.WASM_GENERATE_WAT, false),
                wasmDebug = wasmDebug,
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
    generateWat: Boolean,
    wasmDebug: Boolean,
): WasmCompilerResult {
    val mainModuleFragment = loweredIrFragments.last()
    val importModuleName = mainModuleFragment.name.asString()
    val isStdlib = loweredIrFragments.size == 1

    val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
    val codeGenerator = WasmModuleFragmentGenerator(
        backendContext,
        wasmModuleMetadataCache,
        signatureRetriever,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        inlineUnitGetter = isStdlib,
    )

    val wasmCompiledFileFragments = mutableListOf<WasmCompiledFileFragment>()
    val dependenciesModules = mutableListOf<ModuleImport.WasmModuleImport>()

    val mainModuleFileFragment = codeGenerator.generateModuleAsSingleFileFragmentWithIECExport(mainModuleFragment)

    // This signature needed to dynamically load module services
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val additionalSignatureToImport =
        if (isStdlib) emptySet() else setOf(
            signatureRetriever.declarationSignature(backendContext.wasmSymbols.registerModuleDescriptor.owner)!!,
            signatureRetriever.declarationSignature(backendContext.wasmSymbols.createString.owner)!!,
            signatureRetriever.declarationSignature(backendContext.wasmSymbols.tryGetAssociatedObject.owner)!!,
        )

    val importedDeclarations = getAllReferencedDeclarations(mainModuleFileFragment, additionalSignatureToImport)

    val dependencyModules = loweredIrFragments.dropLast(1)
    dependencyModules.mapTo(wasmCompiledFileFragments) {
        val dependencyName = it.name.asString()
        dependenciesModules.add(ModuleImport.WasmModuleImport(dependencyName, it.outputFileName))
        codeGenerator.generateModuleAsSingleFileFragmentWithIECImport(it, dependencyName, importedDeclarations)
    }
    wasmCompiledFileFragments.add(mainModuleFileFragment)

    return compileWasm(
        wasmCompiledFileFragments = wasmCompiledFileFragments,
        moduleName = importModuleName,
        configuration = configuration,
        typeScriptFragment = null,
        baseFileName = mainModuleFragment.outputFileName,
        emitNameSection = wasmDebug,
        generateWat = generateWat,
        generateSourceMaps = false,
        generateDwarf = false,
        useDebuggerCustomFormatters = false,
        stdlibModuleName = loweredIrFragments.first().name.asString().takeIf { !isStdlib },
        dependenciesModules = dependenciesModules,
        initializeUnit = isStdlib,
    )
}