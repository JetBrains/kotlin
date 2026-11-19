/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import java.io.File

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
    ): IncrementalJsCachesManager {
        return IncrementalJsCachesManager(icContext, KlibMetadataSerializerProtocol, cacheDirectory)
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
