/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmConfigurationUpdater
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import java.io.File

internal class K2WasmCompilerImpl(
    arguments: K2JSCompilerArguments,
    configuration: CompilerConfiguration,
    moduleName: String,
    outputName: String,
    outputDir: File,
    messageCollector: MessageCollector,
    performanceManager: PerformanceManager?,
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

    @K1Deprecation
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
        val intermediate = WasmBackendPipelinePhase.compileIncrementally(icCaches, configuration)
        performanceManager?.notifyPhaseFinished(PhaseType.TranslationToIr)

        if (intermediate == null) return ExitCode.COMPILATION_ERROR
        WasmBackendPipelinePhase.compileIntermediate(intermediate, configuration)
        return OK
    }

    override fun compileNoIC(
        mainCallArguments: List<String>?,
        module: ModulesStructure,
        moduleKind: ModuleKind?,
    ): ExitCode {
        configuration.perfManager?.let {
            @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
            it.notifyCurrentPhaseFinishedIfNeeded() // TODO: KT-75227
        }
        val intermediate = WasmBackendPipelinePhase.compileNonIncrementally(configuration, module, mainCallArguments)
        WasmBackendPipelinePhase.compileIntermediate(intermediate, configuration)

        @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
        performanceManager?.notifyCurrentPhaseFinishedIfNeeded() // TODO: KT-75227 (at least for K2)

        return OK
    }
}
