/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import java.io.File

fun makeJsIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JSCompilerArguments,
    buildHistoryFile: File,
    messageCollector: MessageCollector = MessageCollector.NONE,
    reporter: ICReporter = DoNothingICReporter,
    modulesApiHistory: ModulesApiHistory = EmptyModulesApiHistory,
    providedChangedFiles: ChangedFiles? = null,
) {
    val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
        .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

    val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)
    withJsIC(args) {
        val compiler = IncrementalJsCompilerRunner(
            cachesDir, buildReporter,
            buildHistoryFile = buildHistoryFile,
            modulesApiHistory = modulesApiHistory,
        )
        compiler.compile(allKotlinFiles, args, messageCollector, providedChangedFiles ?: ChangedFiles.DeterminableFiles.ToBeComputed)
    }
}

inline fun <R> withJsIC(args: CommonCompilerArguments, enabled: Boolean = true, fn: () -> R): R {
    if (args.incrementalCompilation == null) {
        args.incrementalCompilation = enabled
    }
    return fn()
}

private fun parseLibrariesArgument(libraries: String?): List<File> =
    (libraries ?: "").split(File.pathSeparator).filter { it.isNotEmpty() }.map { File(it) }

class IncrementalJsCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    buildHistoryFile: File?,
    modulesApiHistory: ModulesApiHistory,
    icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
    pluginsLoader: PluginsLoader? = null,
    lookupTrackerDelegate: LookupTracker = LookupTracker.DO_NOTHING,
) : IncrementalKlibCompilerRunner<CommonJsAndWasmCompilerArguments>(
    workingDir,
    "caches-js",
    reporter,
    buildHistoryFile,
    modulesApiHistory,
    icFeatures,
    pluginsLoader,
    lookupTrackerDelegate,
) {
    override fun createCacheManager(
        icContext: IncrementalCompilationContext, args: CommonJsAndWasmCompilerArguments
    ): IncrementalKlibCachesManager {
        return IncrementalKlibCachesManager(icContext, cacheDirectory, KlibCachePlatform.JS)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCompiler(args: CommonJsAndWasmCompilerArguments): CLICompiler<CommonJsAndWasmCompilerArguments> {
        return when (args) {
            is KotlinWasmCompilerArguments -> KotlinWasmCompiler()
            is K2JSCompilerArguments -> K2JSCompiler()
        } as CLICompiler<CommonJsAndWasmCompilerArguments>
    }

    override fun destinationDir(args: CommonJsAndWasmCompilerArguments): File {
        return File(args.outputDir!!)
    }

    override fun classpathForLibrarySetSnapshot(args: CommonJsAndWasmCompilerArguments): List<File> =
        parseLibrariesArgument(args.libraries)

}
