/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File

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
        )

        configuration.perfManager?.notifyGenerationFinished()

        writeCompilationResult(
            result = res,
            dir = outputDir,
            fileNameBase = outputName,
            useDebuggerCustomFormatters = configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)
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
        val performanceManager = configuration.perfManager
        val generateDts = configuration.getBoolean(JSConfigurationKeys.GENERATE_DTS)
        val generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)
        val useDebuggerCustomFormatters = configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)

        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val irModuleInfo = loadIr(
            depsDescriptors = module,
            irFactory = irFactory,
            loadFunctionInterfacesIntoStdlib = true,
        )

        val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
            irModuleInfo,
            module.mainModule,
            configuration,
            performanceManager,
            exportedDeclarations = setOf(FqName("main")),
            generateTypeScriptFragment = generateDts,
            propertyLazyInitialization = propertyLazyInitialization,
        )

        performanceManager?.notifyBackendStarted()
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

        performanceManager?.notifyBackendFinished()
        performanceManager?.notifyGenerationFinished()

        writeCompilationResult(
            result = res,
            dir = outputDir,
            fileNameBase = outputName,
            useDebuggerCustomFormatters = useDebuggerCustomFormatters
        )

        performanceManager?.notifyTranslationToIRFinished()

        return res
    }
}
