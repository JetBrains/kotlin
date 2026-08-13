/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.CliDiagnostics.JS_IC_ERROR
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.util.PhaseType
import java.io.File

abstract class WebBackendPipelinePhase<Output : WebBackendPipelineArtifact, IntermediateOutput, ICArtifact : ModuleArtifact>(
    name: String
) : PipelinePhase<ConfigurationPipelineArtifact, Output>(
    name = name,
    preActions = emptySet(),
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): Output? {
        val configuration = input.configuration

        val cacheDirectory = configuration.icCacheDirectory
        val outputDirPath = configuration.outputDir

        configuration.reportLog("Produce executable: $outputDirPath")
        configuration.reportLog("Cache directory: $cacheDirectory")

        if (cacheDirectory != null) {
            val cacheGuard = IncrementalCacheGuard(cacheDirectory)
            val backendIr = compileToBackendIrIncrementally(cacheDirectory, cacheGuard, configuration)
            return cacheGuard.tryAcquireAndRelease {
                backendIr?.let { compileIntermediate(it, configuration) }
            }
        } else {
            configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)
            val loadedKlibArtifact = klibLoadingPhase.executePhaseIsolatedWithActions(input) ?: return null
            val backendIr = compileNonIncrementally(loadedKlibArtifact)
            return backendIr?.let { compileIntermediate(it, configuration) }
        }
    }

    private fun compileToBackendIrIncrementally(
        cacheDirectory: String,
        cacheGuard: IncrementalCacheGuard,
        configuration: CompilerConfiguration,
    ): IntermediateOutput? {
        val artifactConfiguration = configuration.artifactConfigurations.singleOrNull()
            ?: error("Expected exactly one artifact configuration")
        val icCaches = cacheGuard.acquireAndRelease { status ->
            when (status) {
                IncrementalCacheGuard.AcquireStatus.CACHE_CLEARED -> {
                    configuration.reportInfo("Cache guard file detected, cache directory '$cacheDirectory' cleared")
                }
                IncrementalCacheGuard.AcquireStatus.INVALID_CACHE -> {
                    configuration.report(
                        JS_IC_ERROR,
                        "Cache guard file detected in readonly mode, cache directory '$cacheDirectory' should be cleared"
                    )
                    return null
                }
                IncrementalCacheGuard.AcquireStatus.OK -> {}
            }
            prepareIcCaches(
                cacheDirectory = cacheDirectory,
                outputDir = configuration.outputDir!!,
                targetConfiguration = configuration,
                artifactConfiguration = artifactConfiguration,
            )
        }
        configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        // We use one cache directory for both caches: JS AST and JS code.
        // This guard MUST be unlocked after a successful preparing icCaches (see prepareIcCaches()).
        // Do not use IncrementalCacheGuard::acquire() - it may drop an entire cache here, and
        // it breaks the logic from JsExecutableProducer(), therefore use IncrementalCacheGuard::tryAcquire() instead
        // TODO: One day, when we will lower IR and produce JS AST per module,
        //      think about using different directories for JS AST and JS code.
        return cacheGuard.tryAcquireAndRelease {
            compileIncrementally(icCaches, configuration)
        }
    }

    private fun prepareIcCaches(
        cacheDirectory: String,
        outputDir: File,
        targetConfiguration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): List<ICArtifact> {

        targetConfiguration.reportLog("")
        targetConfiguration.reportLog("Building cache:")
        targetConfiguration.reportLog("to: $outputDir")
        targetConfiguration.reportLog("cache directory: $cacheDirectory")
        targetConfiguration.reportLog(targetConfiguration.libraries.toString())

        val start = System.currentTimeMillis()

        val cacheUpdater = createCacheUpdater(cacheDirectory, targetConfiguration, artifactConfiguration)

        val artifacts = cacheUpdater.actualizeCaches()

        targetConfiguration.reportLog("IC rebuilt overall time: ${System.currentTimeMillis() - start}ms")
        for ([event, duration] in cacheUpdater.getStopwatchLastLaps()) {
            targetConfiguration.reportLog("  $event: ${(duration / 1e6).toInt()}ms")
        }

        var libIndex = 0
        for ([libFile, srcFiles] in cacheUpdater.getDirtyFileLastStats()) {
            val singleState = srcFiles.values.firstOrNull()?.singleOrNull()?.let { singleState ->
                singleState.takeIf { srcFiles.values.all { it.singleOrNull() == singleState } }
            }

            val [msg, showFiles] = when {
                singleState == DirtyFileState.NON_MODIFIED_IR -> continue
                singleState == DirtyFileState.REMOVED_FILE -> "removed" to emptyMap()
                singleState == DirtyFileState.ADDED_FILE -> "built clean" to emptyMap()
                srcFiles.values.any { it.singleOrNull() == DirtyFileState.NON_MODIFIED_IR } -> "partially rebuilt" to srcFiles
                else -> "fully rebuilt" to srcFiles
            }
            targetConfiguration.reportLog("${++libIndex}) module [${File(libFile.path).name}] was $msg")
            var fileIndex = 0
            for ([srcFile, stat] in showFiles) {
                val filteredStats = stat.filter { it != DirtyFileState.NON_MODIFIED_IR }
                val statStr = filteredStats.takeIf { it.isNotEmpty() }?.joinToString { it.str } ?: continue
                // Use index, because MessageCollector ignores already reported messages
                targetConfiguration.reportLog("  $libIndex.${++fileIndex}) file [${File(srcFile.path).name}]: ($statStr)")
            }
        }

        return artifacts
    }

    protected abstract fun createCacheUpdater(
        cacheDirectory: String,
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): CacheUpdater<ICArtifact>

    protected abstract val klibLoadingPhase: WebIrLoadingPipelinePhase

    abstract fun compileIncrementally(
        icCaches: List<ICArtifact>,
        configuration: CompilerConfiguration,
    ): IntermediateOutput?

    abstract fun compileNonIncrementally(loadedIrArtifact: WebLoadedIrPipelineArtifact): IntermediateOutput?

    abstract fun compileIntermediate(
        intermediateResult: IntermediateOutput,
        configuration: CompilerConfiguration,
    ): Output
}
