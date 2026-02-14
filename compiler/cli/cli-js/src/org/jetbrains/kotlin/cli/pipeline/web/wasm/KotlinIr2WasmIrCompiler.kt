/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.js.config.dce
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.useDebuggerCustomFormatters
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.wasm.config.*
import java.net.URLEncoder

fun encodeModuleName(moduleName: String): String = moduleName
    .replace("<", "_")
    .replace(">", "_")
    .replace(":", "_")
    .replace(" ", "_")
    .let { URLEncoder.encode(it, "UTF-8") }

private val IrModuleFragment.outputFileName
    get() = kotlinLibrary?.jsOutputName ?: encodeModuleName(name.asString())

abstract class WasmCompilerBase(val configuration: CompilerConfiguration) {
    abstract val irFactory: IrFactoryImplForWasmIC
    abstract fun loadIr(modulesStructure: ModulesStructure): IrModuleInfo
    abstract fun lowerIr(irModuleInfo: IrModuleInfo, mainModule: MainModule, exportedDeclarations: Set<FqName>): LoweredIrWithExtraArtifacts
    abstract fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): List<WasmIrModuleConfiguration>
}

abstract class WholeWorldCompilerBase(configuration: CompilerConfiguration, private val noCrossFileOptimisations: Boolean) : WasmCompilerBase(configuration) {
    override fun loadIr(modulesStructure: ModulesStructure): IrModuleInfo {
        return loadIr(
            modulesStructure = modulesStructure,
            irFactory = irFactory,
            loadFunctionInterfacesIntoStdlib = true
        )
    }

    override fun lowerIr(irModuleInfo: IrModuleInfo, mainModule: MainModule, exportedDeclarations: Set<FqName>): LoweredIrWithExtraArtifacts {
        configuration.wasmDisableCrossFileOptimisations = noCrossFileOptimisations
        return compileToLoweredIr(
            irModuleInfo = irModuleInfo,
            mainModule = mainModule,
            configuration = configuration,
            exportedDeclarations = exportedDeclarations,
        )
    }
}

class WholeWorldCompiler(configuration: CompilerConfiguration, override val irFactory: IrFactoryImplForWasmIC) : WholeWorldCompilerBase(configuration, noCrossFileOptimisations = false) {
    override fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): List<WasmIrModuleConfiguration> {
        val dceDumpNameCache = DceDumpNameCache()
        if (configuration.dce) {
            eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
        }
        dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, loweredIr.loweredIr, dceDumpNameCache)

        val configuration = compileWholeProgramModeToWasmIr(
            configuration = configuration,
            idSignatureRetriever = irFactory,
            loweredIr = loweredIr,
        )

        return listOf(configuration)
    }
}

class WholeWorldMultiModuleCompiler(configuration: CompilerConfiguration, override val irFactory: IrFactoryImplForWasmIC) : WholeWorldCompilerBase(configuration, noCrossFileOptimisations = true) {
    override fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): List<WasmIrModuleConfiguration> {
        val allModules = loweredIr.loweredIr

        val dceDumpNameCache = DceDumpNameCache()
        if (configuration.dce) {
            eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
        }
        dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, allModules, dceDumpNameCache)

        val lastModule = allModules.last()
        return allModules.map { currentModule ->
            compileSingleModuleToWasmIr(
                configuration = configuration,
                loweredIr = loweredIr,
                signatureRetriever = irFactory,
                stdlibIsMainModule = currentModule.kotlinLibrary?.isWasmStdlib == true,
                outputFileNameBase = configuration.outputName?.takeIf { currentModule == lastModule },
                mainModuleFragment = currentModule,
                typeTracking = true,
            )
        }
    }
}

class SingleModuleCompiler(configuration: CompilerConfiguration, override val irFactory: IrFactoryImplForWasmIC, val isWasmStdlib: Boolean) : WasmCompilerBase(configuration) {
    override fun loadIr(modulesStructure: ModulesStructure): IrModuleInfo =
        loadIrForSingleModule(modulesStructure = modulesStructure, irFactory = irFactory)

    override fun lowerIr(
        irModuleInfo: IrModuleInfo,
        mainModule: MainModule,
        exportedDeclarations: Set<FqName>
    ): LoweredIrWithExtraArtifacts {
        configuration.wasmDisableCrossFileOptimisations = true
        return compileToLoweredIr(
            irModuleInfo = irModuleInfo,
            mainModule = mainModule,
            configuration = configuration,
            exportedDeclarations = exportedDeclarations,
        )
    }

    override fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): List<WasmIrModuleConfiguration> {
        val configuration = compileSingleModuleToWasmIr(
            configuration = configuration,
            loweredIr = loweredIr,
            signatureRetriever = irFactory,
            stdlibIsMainModule = isWasmStdlib,
            outputFileNameBase = configuration.outputName,
            mainModuleFragment = loweredIr.backendContext.irModuleFragment,
            typeTracking = false,
        )
        return listOf(configuration)
    }
}

private fun compileWholeProgramModeToWasmIr(
    configuration: CompilerConfiguration,
    idSignatureRetriever: IdSignatureRetriever,
    loweredIr: LoweredIrWithExtraArtifacts,
): WasmIrModuleConfiguration {
    val (allModules, backendContext, typeScriptFragment) = loweredIr

    val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
    val codeGenerator = WasmModuleFragmentGenerator(
        backendContext,
        wasmModuleMetadataCache,
        idSignatureRetriever,
        allowIncompleteImplementations = configuration.dce,
        skipCommentInstructions = !configuration.wasmGenerateWat,
        skipLocations = !configuration.wasmGenerateDwarf && !configuration.sourceMap,
    )
    val wasmCompiledFileFragments = allModules.map { irModuleFragment ->
        codeGenerator.generateAsSingleFileFragment(
            irModuleFragment = irModuleFragment,
            trackedTypes = null,
            trackedReferences = null,
            enableMultimoduleExports = false,
        )
    }

    return WasmIrModuleConfiguration(
        wasmCompiledFileFragments = wasmCompiledFileFragments,
        moduleName = allModules.last().descriptor.name.asString(),
        configuration = configuration,
        typeScriptFragment = typeScriptFragment,
        baseFileName = configuration.outputName!!,
        multimoduleOptions = null,
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun compileSingleModuleToWasmIr(
    configuration: CompilerConfiguration,
    loweredIr: LoweredIrWithExtraArtifacts,
    signatureRetriever: IdSignatureRetriever,
    stdlibIsMainModule: Boolean,
    outputFileNameBase: String? = null,
    mainModuleFragment: IrModuleFragment,
    typeTracking: Boolean,
): WasmIrModuleConfiguration {

    val backendContext = loweredIr.backendContext
    val wasmModuleMetadataCache = WasmModuleMetadataCache(loweredIr.backendContext)
    val moduleName = mainModuleFragment.name.asString()

    val codeGenerator = WasmModuleFragmentGenerator(
        backendContext,
        wasmModuleMetadataCache,
        signatureRetriever,
        allowIncompleteImplementations = configuration.dce,
        skipCommentInstructions = !configuration.wasmGenerateWat,
        skipLocations = !configuration.wasmGenerateDwarf && !configuration.sourceMap,
    )

    val dependencyImports = mutableSetOf<WasmModuleDependencyImport>()
    val referencedDeclarations = ModuleReferencedDeclarations()
    val referencedTypes = typeTracking.ifTrue { ModuleReferencedTypes() }
    fun referenceFunction(functionSymbol: IrFunctionSymbol) {
        val signature = signatureRetriever.declarationSignature(functionSymbol.owner)!!
        referencedDeclarations.functions.add(signature)
        referencedTypes?.addFunctionTypeToReferenced(functionSymbol, signatureRetriever)
    }

    val compiledModuleFragments = mutableListOf<WasmCompiledFileFragment>()

    val mainModuleFileFragment = codeGenerator.generateAsSingleFileFragment(
        irModuleFragment = mainModuleFragment,
        trackedReferences = referencedDeclarations,
        trackedTypes = referencedTypes,
        enableMultimoduleExports = true,
    )
    compiledModuleFragments.add(mainModuleFileFragment)

    // This signature needed to dynamically load module services
    if (!stdlibIsMainModule) {
        referenceFunction(backendContext.wasmSymbols.registerModuleDescriptor)
        referenceFunction(backendContext.wasmSymbols.createString)
        referenceFunction(backendContext.wasmSymbols.tryGetAssociatedObject)
        backendContext.wasmSymbols.runRootSuites?.owner?.let { runRootSuites ->
            referenceFunction(runRootSuites.symbol)
        }
        if (backendContext.isWasmJsTarget) {
            referenceFunction(backendContext.wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinStringAdapter)
        }
    }

    val dependencyResolutionMap = parseDependencyResolutionMap(configuration)
    val dependencyModules = loweredIr.loweredIr.filterNot { it == mainModuleFragment }
    dependencyModules.mapTo(compiledModuleFragments) { irFragment ->
        val dependencyFragment =
            codeGenerator.generateDependencyAsSingleFileFragment(irFragment)
                .makeProjection(referencedTypes, referencedDeclarations)

        if (dependencyFragment.definedDeclarations.hasDeclarations) {
            val dependencyName = irFragment.name.asString()
            dependencyImports.add(
                WasmModuleDependencyImport(
                    dependencyName,
                    dependencyResolutionMap[dependencyName]
                        ?: irFragment.outputFileName
                )
            )
        }
        dependencyFragment
    }

    val stdlibModuleNameForImport =
        loweredIr.loweredIr.first().name.asString().takeIf { !stdlibIsMainModule }

    configuration.useDebuggerCustomFormatters = configuration.useDebuggerCustomFormatters && stdlibModuleNameForImport == null

    val multimoduleOptions = MultimoduleCompileOptions(
        stdlibModuleNameForImport = stdlibModuleNameForImport,
        dependencyModules = dependencyImports,
        initializeUnit = stdlibIsMainModule,
    )

    return WasmIrModuleConfiguration(
        wasmCompiledFileFragments = compiledModuleFragments,
        moduleName = moduleName,
        configuration = configuration,
        typeScriptFragment = loweredIr.typeScriptFragment,
        baseFileName = outputFileNameBase ?: mainModuleFragment.outputFileName,
        multimoduleOptions = multimoduleOptions,
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