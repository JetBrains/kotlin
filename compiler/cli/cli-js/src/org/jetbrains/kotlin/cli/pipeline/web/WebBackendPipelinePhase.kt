/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.js.IcCachesConfigurationData
import org.jetbrains.kotlin.cli.js.platformChecker
import org.jetbrains.kotlin.cli.js.prepareIcCaches
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.ic.IncrementalCacheGuard
import org.jetbrains.kotlin.ir.backend.js.ic.acquireAndRelease
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInProductionPipeline
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File

abstract class WebBackendPipelinePhase<Output : WebBackendPipelineArtifact>(
    name: String
) : PipelinePhase<ConfigurationPipelineArtifact, Output>(
    name = name,
    preActions = emptySet(),
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): Output? {
        val configuration = input.configuration
        val messageCollector = configuration.messageCollector

        val cacheDirectory = configuration.icCacheDirectory
        val outputDirPath = configuration.outputDir
        messageCollector.report(CompilerMessageSeverity.INFO, "Produce executable: $outputDirPath")
        messageCollector.report(CompilerMessageSeverity.INFO, "Cache directory: $cacheDirectory")

        val mainCallArguments = if (configuration.callMainMode == K2JsArgumentConstants.NO_CALL) null else emptyList<String>()

        if (cacheDirectory != null) {
            val icCacheReadOnly = configuration.wasmCompilation && configuration.icCacheReadOnly
            val cacheGuard = IncrementalCacheGuard(cacheDirectory, icCacheReadOnly)

            val icCaches = cacheGuard.acquireAndRelease { status ->
                when (status) {
                    IncrementalCacheGuard.AcquireStatus.CACHE_CLEARED -> {
                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "Cache guard file detected, cache directory '$cacheDirectory' cleared"
                        )
                    }
                    IncrementalCacheGuard.AcquireStatus.INVALID_CACHE -> {
                        messageCollector.report(
                            CompilerMessageSeverity.ERROR,
                            "Cache guard file detected in readonly mode, cache directory '$cacheDirectory' should be cleared"
                        )
                        return null
                    }
                    IncrementalCacheGuard.AcquireStatus.OK -> {}
                }
                prepareIcCaches(
                    cacheDirectory = cacheDirectory,
                    icConfigurationData = when {
                        configuration.wasmCompilation -> IcCachesConfigurationData.Wasm(
                            wasmDebug = configuration.getBoolean(WasmConfigurationKeys.WASM_DEBUG),
                            preserveIcOrder = configuration.preserveIcOrder,
                            generateWat = configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT),
                        )
                        else -> IcCachesConfigurationData.Js(
                            granularity = configuration.granularity!!
                        )
                    },
                    messageCollector = messageCollector,
                    outputDir = configuration.outputDir!!,
                    targetConfiguration = configuration,
                    mainCallArguments = mainCallArguments,
                    icCacheReadOnly = icCacheReadOnly,
                )
            }

            // We use one cache directory for both caches: JS AST and JS code.
            // This guard MUST be unlocked after a successful preparing icCaches (see prepareIcCaches()).
            // Do not use IncrementalCacheGuard::acquire() - it may drop an entire cache here, and
            // it breaks the logic from JsExecutableProducer(), therefore use IncrementalCacheGuard::tryAcquire() instead
            // TODO: One day, when we will lower IR and produce JS AST per module,
            //      think about using different directories for JS AST and JS code.
            val output = cacheGuard.tryAcquireAndRelease {
                compileIncrementally(icCaches, configuration)
            }
            return output
        } else {
            val includes = configuration.includes!!
            val includesPath = File(includes).canonicalPath
            val mainLibPath = configuration.libraries.find { File(it).canonicalPath == includesPath }
                ?: error("No library with name $includes ($includesPath) found")
            val kLib = MainModule.Klib(mainLibPath)
            val environment = KotlinCoreEnvironment.createForProduction(
                input.rootDisposable,
                configuration,
                configFiles
            )

            val klibs = loadWebKlibsInProductionPipeline(configuration, configuration.platformChecker)

            val module = ModulesStructure(
                project = environment.project,
                mainModule = kLib,
                compilerConfiguration = configuration,
                klibs = klibs,
            )

            return compileNonIncrementally(configuration, module, mainCallArguments)
        }
    }

    protected abstract val configFiles: EnvironmentConfigFiles

    abstract fun compileIncrementally(icCaches: IcCachesArtifacts, configuration: CompilerConfiguration): Output?

    abstract fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        mainCallArguments: List<String>?,
    ): Output?
}
