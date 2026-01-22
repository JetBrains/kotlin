/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedDeclarations
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.MainModule
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
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.config.dceDumpDeclarationIrSizesToFile
import org.jetbrains.kotlin.wasm.config.wasmDisableCrossFileOptimisations
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly
import java.net.URLEncoder

private val IrModuleFragment.outputFileName
    get() = kotlinLibrary?.jsOutputName ?: (name.asString()
        .replace("<", "_")
        .replace(">", "_")
        .replace(":", "_")
        .replace(" ", "_")
        .let { URLEncoder.encode(it, "UTF-8") })

object WasmBackendPipelinePhase : WebBackendPipelinePhase<WasmBackendPipelineArtifact, WasmIrModuleConfiguration>("WasmBackendPipelinePhase") {
    override val configFiles: EnvironmentConfigFiles
        get() = EnvironmentConfigFiles.WASM_CONFIG_FILES

    override fun compileIntermediate(
        intermediateResult: WasmIrModuleConfiguration,
        configuration: CompilerConfiguration,
    ): WasmBackendPipelineArtifact = configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
        val linkedModule = linkWasmIr(intermediateResult)
        val compileResult = compileWasmIrToBinary(intermediateResult, linkedModule)
        val outputDir = configuration.outputDir!!
        writeCompilationResult(
            result = compileResult,
            dir = outputDir,
            fileNameBase = intermediateResult.baseFileName,
        )
        WasmBackendPipelineArtifact(compileResult, outputDir, configuration)
    }

    override fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
    ): WasmIrModuleConfiguration? {
        if (configuration.getBoolean(WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY)) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Incremental compilation not supported for single module mode"
            )
            return null
        }

        val wasmArtifacts = icCaches.artifacts
            .filterIsInstance<WasmModuleArtifact>()
            .flatMap { it.fileArtifacts }
            .mapNotNull { it.loadIrFragments()?.mainFragment }
            .let { fragments -> if (configuration.preserveIcOrder) fragments.sortedBy { it.fragmentTag } else fragments }

        return WasmIrModuleConfiguration(
            wasmCompiledFileFragments = wasmArtifacts,
            moduleName = configuration.moduleName!!,
            configuration = configuration,
            typeScriptFragment = null,
            baseFileName = configuration.outputName!!,
            multimoduleOptions = null,
        )
    }

    override fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        mainCallArguments: List<String>?,
    ): WasmIrModuleConfiguration {
        return if (!configuration.wasmIncludedModuleOnly) {
            compileWholeProgramModeToWasmIr(configuration, module)
        } else {
            compileSingleModuleToWasmIr(configuration, module)
        }
    }

    fun compileWholeProgramModeToWasmIrWithAndWithoutDCE(
        configuration: CompilerConfiguration,
        irModuleInfo: IrModuleInfo,
        mainModule: MainModule,
        idSignatureRetriever: IdSignatureRetriever,
        exportedDeclarations: Set<FqName>,
    ): Pair<WasmIrModuleConfiguration, WasmIrModuleConfiguration> {

        val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            configuration.wasmDisableCrossFileOptimisations = false
            compileToLoweredIr(
                irModuleInfo = irModuleInfo,
                mainModule = mainModule,
                configuration = configuration,
                exportedDeclarations = exportedDeclarations,
            )
        }

        return configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
            val withoutDCE =
                compileWholeProgramModeToWasmIr(configuration, idSignatureRetriever, loweredIr, allowIncompleteImplementations = false)

            val dceDumpNameCache = DceDumpNameCache()
            eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
            dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, loweredIr.loweredIr, dceDumpNameCache)

            val withDCE =
                compileWholeProgramModeToWasmIr(configuration, idSignatureRetriever, loweredIr, allowIncompleteImplementations = true)

            withoutDCE to withDCE
        }

    }

    private fun compileWholeProgramModeToWasmIr(
        configuration: CompilerConfiguration,
        modulesStructure: ModulesStructure,
        exportedDeclarations: Set<FqName> = emptySet(),
    ): WasmIrModuleConfiguration {
        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val irModuleInfo = configuration.perfManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            loadIr(
                modulesStructure = modulesStructure,
                irFactory = irFactory,
                loadFunctionInterfacesIntoStdlib = true,
            )
        }

        val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            configuration.wasmDisableCrossFileOptimisations = false
            compileToLoweredIr(
                irModuleInfo = irModuleInfo,
                mainModule = modulesStructure.mainModule,
                configuration = configuration,
                exportedDeclarations = exportedDeclarations,
            )
        }

        return configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
            val dceDumpNameCache = DceDumpNameCache()
            if (configuration.dce) {
                eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
            }
            dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, loweredIr.loweredIr, dceDumpNameCache)

            compileWholeProgramModeToWasmIr(
                configuration = configuration,
                idSignatureRetriever = irFactory,
                loweredIr = loweredIr,
                allowIncompleteImplementations = configuration.dce
            )
        }
    }

    private fun compileWholeProgramModeToWasmIr(
        configuration: CompilerConfiguration,
        idSignatureRetriever: IdSignatureRetriever,
        loweredIr: LoweredIrWithExtraArtifacts,
        allowIncompleteImplementations: Boolean,
    ): WasmIrModuleConfiguration {
        val (allModules, backendContext, typeScriptFragment) = loweredIr

        val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
        val codeGenerator = WasmModuleFragmentGenerator(
            backendContext,
            wasmModuleMetadataCache,
            idSignatureRetriever,
            allowIncompleteImplementations = allowIncompleteImplementations,
            skipCommentInstructions = !configuration.wasmGenerateWat,
            skipLocations = !configuration.wasmGenerateDwarf && !configuration.sourceMap,
        )
        val wasmCompiledFileFragments = allModules.map { codeGenerator.generateModuleAsSingleFileFragment(it) }

        return WasmIrModuleConfiguration(
            wasmCompiledFileFragments = wasmCompiledFileFragments,
            moduleName = allModules.last().descriptor.name.asString(),
            configuration = configuration,
            typeScriptFragment = typeScriptFragment,
            baseFileName = configuration.outputName!!,
            multimoduleOptions = null,
        )
    }

    private fun parseDependencyResolutionMap(configuration: CompilerConfiguration)
            : Map<String, String> {

        val rawResolutionMap = configuration[WasmConfigurationKeys.WASM_DEPENDENCY_RESOLUTION_MAP] ?: return emptyMap()

        val parsedResolutionMap = rawResolutionMap.split(",")
            .map { it.split(":") }
            .associate { it[0] to it[1] }

        return parsedResolutionMap
    }

    private fun compileSingleModuleToWasmIr(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
    ): WasmIrModuleConfiguration {
        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val irModuleInfo = configuration.perfManager.tryMeasurePhaseTime(PhaseType.TranslationToIr) {
            loadIrForSingleModule(
                modulesStructure = module,
                irFactory = irFactory,
            )
        }

        return compileWasmLoweredFragmentsForSingleModule(
            configuration = configuration,
            irModuleInfo = irModuleInfo,
            mainModule = module.mainModule,
            signatureRetriever = irFactory,
            stdlibIsMainModule = module.klibs.included?.isWasmStdlib == true,
            dependencyResolutionMap = parseDependencyResolutionMap(configuration),
            exportedDeclarations = setOf(FqName("main")),
        )
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun compileWasmLoweredFragmentsForSingleModule(
    configuration: CompilerConfiguration,
    irModuleInfo: IrModuleInfo,
    mainModule: MainModule,
    signatureRetriever: IdSignatureRetriever,
    stdlibIsMainModule: Boolean,
    outputFileNameBase: String? = null,
    dependencyResolutionMap: Map<String, String>,
    exportedDeclarations: Set<FqName>,
): WasmIrModuleConfiguration {

    val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
        configuration.wasmDisableCrossFileOptimisations = true
        compileToLoweredIr(
            irModuleInfo = irModuleInfo,
            mainModule = mainModule,
            configuration = configuration,
            exportedDeclarations = exportedDeclarations,
        )
    }

    return configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
        val backendContext = loweredIr.backendContext
        val wasmModuleMetadataCache = WasmModuleMetadataCache(loweredIr.backendContext)
        val fragmentToCompile = loweredIr.backendContext.irModuleFragment
        val moduleName = fragmentToCompile.name.asString()

        val codeGenerator = WasmModuleFragmentGenerator(
            backendContext,
            wasmModuleMetadataCache,
            signatureRetriever,
            allowIncompleteImplementations = false,
            skipCommentInstructions = !configuration.wasmGenerateWat,
            skipLocations = !configuration.wasmGenerateDwarf && !configuration.sourceMap,
        )

        val wasmCompiledFileFragments = mutableListOf<WasmCompiledFileFragment>()
        val dependencyImports = mutableSetOf<WasmModuleDependencyImport>()

        val referencedDeclarations = ModuleReferencedDeclarations()

        val mainModuleFileFragment = codeGenerator.generateModuleAsSingleFileFragmentWithModuleExport(
            irModuleFragment = fragmentToCompile,
            referencedDeclarations = referencedDeclarations,
        )

        // This signature needed to dynamically load module services
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        if (!stdlibIsMainModule) {
            referencedDeclarations.referencedFunction.add(signatureRetriever.declarationSignature(backendContext.wasmSymbols.registerModuleDescriptor.owner)!!)
            referencedDeclarations.referencedFunction.add(signatureRetriever.declarationSignature(backendContext.wasmSymbols.createString.owner)!!)
            referencedDeclarations.referencedFunction.add(signatureRetriever.declarationSignature(backendContext.wasmSymbols.tryGetAssociatedObject.owner)!!)
            backendContext.wasmSymbols.runRootSuites?.owner?.let { runRootSuites ->
                referencedDeclarations.referencedFunction.add(signatureRetriever.declarationSignature(runRootSuites)!!)
            }
            if (backendContext.isWasmJsTarget) {
                referencedDeclarations.referencedFunction.add(signatureRetriever.declarationSignature(backendContext.wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinStringAdapter.owner)!!)
            }
        }

        val dependencyModules = loweredIr.loweredIr.filterNot { it == fragmentToCompile }
        dependencyModules.mapTo(wasmCompiledFileFragments) { irFragment ->
            val dependencyName = irFragment.name.asString()

            val (wasmFragment, isImported) =
                codeGenerator.generateModuleAsSingleFileFragmentWithModuleImport(irFragment, dependencyName, referencedDeclarations)

            if (isImported) {
                dependencyImports.add(
                    WasmModuleDependencyImport(
                        dependencyName,
                        dependencyResolutionMap[dependencyName]
                            ?: irFragment.outputFileName
                    )
                )
            }

            wasmFragment
        }
        wasmCompiledFileFragments.add(mainModuleFileFragment)

        val stdlibModuleNameForImport =
            loweredIr.loweredIr.first().name.asString().takeIf { !stdlibIsMainModule }

        configuration.useDebuggerCustomFormatters = configuration.useDebuggerCustomFormatters && stdlibModuleNameForImport == null

        val multimoduleOptions = MultimoduleCompileOptions(
            stdlibModuleNameForImport = stdlibModuleNameForImport,
            dependencyModules = dependencyImports,
            initializeUnit = stdlibIsMainModule,
        )

        WasmIrModuleConfiguration(
            wasmCompiledFileFragments = wasmCompiledFileFragments,
            moduleName = moduleName,
            configuration = configuration,
            typeScriptFragment = loweredIr.typeScriptFragment,
            baseFileName = outputFileNameBase ?: fragmentToCompile.outputFileName,
            multimoduleOptions = multimoduleOptions,
        )
    }
}