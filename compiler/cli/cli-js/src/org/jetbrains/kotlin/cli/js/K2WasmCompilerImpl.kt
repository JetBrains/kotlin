/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.wasm.getWasmPhases
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmConfigurationUpdater
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

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

    override fun tryInitializeCompiler(rootDisposable: Disposable): KotlinCoreEnvironment? {
        WasmConfigurationUpdater.fillConfiguration(configuration, arguments)

        val environmentForWasm =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.WASM_CONFIG_FILES)
        if (messageCollector.hasErrors()) return null
        return environmentForWasm
    }

    override fun compileWithIC(
        icCaches: IcCachesArtifacts,
        targetConfiguration: CompilerConfiguration,
        moduleKind: ModuleKind?,
    ): ExitCode {
        WasmBackendPipelinePhase.compileIncrementally(
            icCaches,
            configuration,
            moduleName,
            outputDir,
            outputName,
            arguments.preserveIcOrder,
            arguments.wasmDebug,
            arguments.wasmGenerateWat,
            arguments.generateDwarf
        )

        performanceManager?.notifyIRTranslationFinished()

        return OK
    }

    override fun compileNoIC(
        mainCallArguments: List<String>?,
        module: ModulesStructure,
        moduleKind: ModuleKind?,
    ): ExitCode {
        configuration.phaseConfig = createPhaseConfig(arguments).also {
            if (arguments.listPhases) it.list(getWasmPhases(configuration, isIncremental = false))
        }


        WasmBackendPipelinePhase.compileNonIncrementally(
            configuration,
            module,
            outputName,
            outputDir,
            propertyLazyInitialization = arguments.irPropertyLazyInitialization,
            dce = arguments.irDce,
            dceDumpDeclarationIrSizesToFile = arguments.irDceDumpDeclarationIrSizesToFile,
            wasmDebug = arguments.wasmDebug,
            generateDwarf = arguments.generateDwarf
        )

        performanceManager?.notifyIRTranslationFinished()

        return OK
    }
}
