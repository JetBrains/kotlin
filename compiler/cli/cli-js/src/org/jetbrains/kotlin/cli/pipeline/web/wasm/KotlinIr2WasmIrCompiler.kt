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
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.js.config.dce
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.useDebuggerCustomFormatters
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.config.*
import java.net.URLEncoder

private val IrModuleFragment.outputFileName
    get() = kotlinLibrary?.jsOutputName ?: (name.asString()
        .replace("<", "_")
        .replace(">", "_")
        .replace(":", "_")
        .replace(" ", "_")
        .let { URLEncoder.encode(it, "UTF-8") })

abstract class WasmCompilerBase(val configuration: CompilerConfiguration) {
    abstract val irFactory: IrFactoryImplForWasmIC
    abstract fun loadIr(modulesStructure: ModulesStructure): IrModuleInfo
    abstract fun lowerIr(irModuleInfo: IrModuleInfo, mainModule: MainModule, exportedDeclarations: Set<FqName>): LoweredIrWithExtraArtifacts
    abstract fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): WasmIrModuleConfiguration
}

class WholeWorldCompiler(configuration: CompilerConfiguration, override val irFactory: IrFactoryImplForWasmIC) : WasmCompilerBase(configuration) {
    override fun loadIr(modulesStructure: ModulesStructure): IrModuleInfo {
        return loadIr(
            modulesStructure = modulesStructure,
            irFactory = irFactory,
            loadFunctionInterfacesIntoStdlib = true
        )
    }

    override fun lowerIr(irModuleInfo: IrModuleInfo, mainModule: MainModule, exportedDeclarations: Set<FqName>): LoweredIrWithExtraArtifacts {
        configuration.wasmDisableCrossFileOptimisations = false
        return compileToLoweredIr(
            irModuleInfo = irModuleInfo,
            mainModule = mainModule,
            configuration = configuration,
            exportedDeclarations = exportedDeclarations,
        )
    }

    override fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): WasmIrModuleConfiguration {
        val dceDumpNameCache = DceDumpNameCache()
        if (configuration.dce) {
            eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
        }
        dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, loweredIr.loweredIr, dceDumpNameCache)

        return compileWholeProgramModeToWasmIr(
            configuration = configuration,
            idSignatureRetriever = irFactory,
            loweredIr = loweredIr,
            allowIncompleteImplementations = configuration.dce
        )
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

    override fun compileIr(loweredIr: LoweredIrWithExtraArtifacts): WasmIrModuleConfiguration =
        compileSingleModuleToWasmIr(
            configuration = configuration,
            loweredIr = loweredIr,
            signatureRetriever = irFactory,
            stdlibIsMainModule = isWasmStdlib,
            mainModuleFragment = loweredIr.backendContext.irModuleFragment,
        )
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

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun compileSingleModuleToWasmIr(
    configuration: CompilerConfiguration,
    loweredIr: LoweredIrWithExtraArtifacts,
    signatureRetriever: IdSignatureRetriever,
    stdlibIsMainModule: Boolean,
    mainModuleFragment: IrModuleFragment,
): WasmIrModuleConfiguration {

    val backendContext = loweredIr.backendContext
    val wasmModuleMetadataCache = WasmModuleMetadataCache(loweredIr.backendContext)
    val moduleName = mainModuleFragment.name.asString()

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
        irModuleFragment = mainModuleFragment,
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

    val dependencyResolutionMap = parseDependencyResolutionMap(configuration)
    val dependencyModules = loweredIr.loweredIr.filterNot { it == mainModuleFragment }
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

    return WasmIrModuleConfiguration(
        wasmCompiledFileFragments = wasmCompiledFileFragments,
        moduleName = moduleName,
        configuration = configuration,
        typeScriptFragment = loweredIr.typeScriptFragment,
        baseFileName = mainModuleFragment.outputFileName,
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