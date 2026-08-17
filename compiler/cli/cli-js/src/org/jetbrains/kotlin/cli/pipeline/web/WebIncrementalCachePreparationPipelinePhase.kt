/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.CliDiagnostics.JS_IC_ERROR
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.util.PhaseType
import java.io.File

abstract class WebIncrementalCachePreparationPipelinePhase<TModuleArtifact, TIcContext>(
    name: String,
) : PipelinePhase<ConfigurationPipelineArtifact, WebIncrementalCachePipelineArtifact<TModuleArtifact>>(
    name = name,
    preActions = emptySet(),
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) where TModuleArtifact : ModuleArtifact,
        TIcContext : PlatformDependentICContext<TModuleArtifact, *, *, *> {

    override fun executePhase(input: ConfigurationPipelineArtifact): WebIncrementalCachePipelineArtifact<TModuleArtifact>? {
        val configuration = input.configuration

        val cacheDirectory = configuration.icCacheDirectory ?: error("Expected a cache directory")
        val outputDirPath = configuration.outputDir

        configuration.reportLog("Produce executable: $outputDirPath")
        configuration.reportLog("Cache directory: $cacheDirectory")

        val cacheGuard = IncrementalCacheGuard(cacheDirectory)
        val artifactConfiguration = configuration.artifactConfigurations.singleOrNull()
            ?: error("Expected exactly one artifact configuration")

        return cacheGuard.acquireAndRelease { status ->
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
            val outputDir = configuration.outputDir ?: error("Expected outputDir")

            configuration.reportLog("")
            configuration.reportLog("Building cache:")
            configuration.reportLog("to: $outputDir")
            configuration.reportLog("cache directory: $cacheDirectory")
            configuration.reportLog(configuration.libraries.toString())

            val start = System.currentTimeMillis()

            val icContext = createIcContext(configuration, artifactConfiguration)
            val cacheUpdater = CacheUpdater(
                cacheDir = cacheDirectory,
                compilerConfiguration = configuration,
                artifactConfiguration = artifactConfiguration,
                icContext = icContext,
                checkForClassStructuralChanges = configuration.wasmCompilation,
                loadBodiesOnlyForMainModule = configuration.wasmCompilationMode() == WasmCompilationMode.SINGLE_MODULE,
            )

            val artifacts = cacheUpdater.actualizeCaches()

            configuration.reportLog("IC rebuilt overall time: ${System.currentTimeMillis() - start}ms")
            for ([event, duration] in cacheUpdater.getStopwatchLastLaps()) {
                configuration.reportLog("  $event: ${(duration / 1e6).toInt()}ms")
            }

            var libIndex = 0
            val dirtyFileLastStats = cacheUpdater.getDirtyFileLastStats()
            for ([libFile, srcFiles] in dirtyFileLastStats) {
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
                configuration.reportLog("${++libIndex}) module [${File(libFile.path).name}] was $msg")
                var fileIndex = 0
                for ([srcFile, stat] in showFiles) {
                    val filteredStats = stat.filter { it != DirtyFileState.NON_MODIFIED_IR }
                    val statStr = filteredStats.takeIf { it.isNotEmpty() }?.joinToString { it.str } ?: continue
                    // Use index, because MessageCollector ignores already reported messages
                    configuration.reportLog("  $libIndex.${++fileIndex}) file [${File(srcFile.path).name}]: ($statStr)")
                }
            }

            configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

            WebIncrementalCachePipelineArtifact(artifacts, dirtyFileLastStats, cacheGuard, configuration)
        }
    }

    protected abstract fun createIcContext(
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): TIcContext
}
