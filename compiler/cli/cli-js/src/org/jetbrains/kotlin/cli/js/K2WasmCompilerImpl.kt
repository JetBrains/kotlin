/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.getWasmPhases
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifact
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.IOException

internal class K2WasmCompilerImpl(
    arguments: K2JSCompilerArguments,
    configuration: CompilerConfiguration,
    moduleName: String,
    outputName: String,
    outputDir: File,
    messageCollector: MessageCollector,
    performanceManager: CommonCompilerPerformanceManager?,
) : K2JsCompilerImplBase(
    arguments = arguments,
    configuration = configuration,
    moduleName = moduleName,
    outputName = outputName,
    outputDir = outputDir,
    messageCollector = messageCollector,
    performanceManager = performanceManager
) {
    override fun checkTargetArguments(): ExitCode? = null

    override fun tryInitializeCompiler(libraries: List<String>, rootDisposable: Disposable): KotlinCoreEnvironment? {
        initializeCommonConfiguration(libraries)

        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.wasmEnableArrayRangeChecks)
        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, arguments.wasmEnableAsserts)
        configuration.put(WasmConfigurationKeys.WASM_GENERATE_WAT, arguments.wasmGenerateWat)
        configuration.put(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, arguments.wasmUseTrapsInsteadOfExceptions)
        configuration.put(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, arguments.wasmUseNewExceptionProposal)
        configuration.put(WasmConfigurationKeys.WASM_USE_JS_TAG, arguments.wasmUseJsTag ?: arguments.wasmUseNewExceptionProposal)
        configuration.putIfNotNull(WasmConfigurationKeys.WASM_TARGET, arguments.wasmTarget?.let(WasmTarget::fromName))

        val moduleName = arguments.irModuleName ?: outputName
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        val environmentForWasm =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.WASM_CONFIG_FILES)
        if (messageCollector.hasErrors()) return null

        val configurationWasm = environmentForWasm.configuration

        configurationWasm.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configurationWasm.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

        return environmentForWasm
    }

    override fun compileWithIC(
        icCaches: IcCachesArtifacts,
        targetConfiguration: CompilerConfiguration,
        moduleKind: ModuleKind?,
    ): ExitCode {
        val wasmArtifacts = icCaches.artifacts
            .filterIsInstance<WasmModuleArtifact>()
            .flatMap { it.fileArtifacts }
            .mapNotNull { it.loadIrFragments()?.mainFragment }
            .let { fragments -> if (arguments.preserveIcOrder) fragments.sortedBy { it.fragmentTag } else fragments }

        val res = compileWasm(
            wasmCompiledFileFragments = wasmArtifacts,
            moduleName = moduleName,
            configuration = configuration,
            typeScriptFragment = null,
            baseFileName = outputName,
            emitNameSection = arguments.wasmDebug,
            generateWat = arguments.wasmGenerateWat,
            generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP),
        )

        performanceManager?.notifyGenerationFinished()

        writeCompilationResult(
            result = res,
            dir = outputDir,
            fileNameBase = outputName,
            useDebuggerCustomFormatters = configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)
        )

        performanceManager?.notifyIRTranslationFinished()

        return OK
    }

    override fun compileNoIC(
        mainCallArguments: List<String>?,
        module: ModulesStructure,
        moduleKind: ModuleKind?,
    ): ExitCode {
        val generateDts = configuration.getBoolean(JSConfigurationKeys.GENERATE_DTS)
        val generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)
        val useDebuggerCustomFormatters = configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)

        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val irModuleInfo = loadIr(
            depsDescriptors = module,
            irFactory = irFactory,
            verifySignatures = false,
            loadFunctionInterfacesIntoStdlib = true,
        )

        configuration.phaseConfig = createPhaseConfig(arguments).also {
            if (arguments.listPhases) it.list(getWasmPhases(configuration, isIncremental = false))
        }

        val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
            irModuleInfo,
            module.mainModule,
            configuration,
            performanceManager,
            exportedDeclarations = setOf(FqName("main")),
            generateTypeScriptFragment = generateDts,
            propertyLazyInitialization = arguments.irPropertyLazyInitialization,
        )

        performanceManager?.notifyIRGenerationStarted()
        val dceDumpNameCache = DceDumpNameCache()
        if (arguments.irDce) {
            eliminateDeadDeclarations(allModules, backendContext, dceDumpNameCache)
        }

        dumpDeclarationIrSizesIfNeed(arguments.irDceDumpDeclarationIrSizesToFile, allModules, dceDumpNameCache)

        val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
        val codeGenerator = WasmModuleFragmentGenerator(
            backendContext,
            wasmModuleMetadataCache,
            irFactory,
            allowIncompleteImplementations = arguments.irDce,
        )
        val wasmCompiledFileFragments = allModules.map { codeGenerator.generateModuleAsSingleFileFragment(it) }

        val res = compileWasm(
            wasmCompiledFileFragments = wasmCompiledFileFragments,
            moduleName = allModules.last().descriptor.name.asString(),
            configuration = configuration,
            typeScriptFragment = typeScriptFragment,
            baseFileName = outputName,
            emitNameSection = arguments.wasmDebug,
            generateWat = configuration.get(WasmConfigurationKeys.WASM_GENERATE_WAT, false),
            generateSourceMaps = generateSourceMaps,
            useDebuggerCustomFormatters = useDebuggerCustomFormatters
        )

        performanceManager?.notifyIRGenerationFinished()
        performanceManager?.notifyGenerationFinished()

        writeCompilationResult(
            result = res,
            dir = outputDir,
            fileNameBase = outputName,
            useDebuggerCustomFormatters = useDebuggerCustomFormatters
        )

        performanceManager?.notifyIRTranslationFinished()

        return OK
    }
}