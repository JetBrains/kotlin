/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DependencyScenarioDslCacheKey
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.copyToRecursively
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

private data class GlobalCompiledProjectsCacheKey(
    val moduleKey: DependencyScenarioDslCacheKey,
    val snapshotConfig: SnapshotConfig,
    val icOptionsConfigAction: ((JvmSnapshotBasedIncrementalCompilationOptions) -> Unit)?,
    val icSourceTracking: Boolean,
)

internal object GlobalCompiledProjectsCache {
    private val globalTempDirectory = Files.createTempDirectory("compiled-test-projects-cache").apply {
        toFile().deleteOnExit()
    }
    private val compiledProjectsCache = mutableMapOf<GlobalCompiledProjectsCacheKey, Pair<MutableSet<String>, Path>>()

    fun getProjectFromCache(
        module: Module,
        strategyConfig: ExecutionPolicy,
        snapshotConfig: SnapshotConfig,
        icOptionsConfigAction: ((JvmSnapshotBasedIncrementalCompilationOptions) -> Unit),
        icSourceTracking: Boolean,
    ): BaseScenarioModule? {
        val (initialOutputs, cachedBuildDirPath) = compiledProjectsCache[GlobalCompiledProjectsCacheKey(
            module.scenarioDslCacheKey,
            snapshotConfig,
            icOptionsConfigAction,
            icSourceTracking,
        )] ?: return null
        cachedBuildDirPath.copyToRecursively(module.buildDirectory, followLinks = false, overwrite = true)
        return if (icSourceTracking) {
            AutoTrackedScenarioModuleImpl(module, initialOutputs, strategyConfig, icOptionsConfigAction)
        } else {
            ExternallyTrackedScenarioModuleImpl(module, initialOutputs, strategyConfig, icOptionsConfigAction)
        }
    }

    fun putProjectIntoCache(
        module: Module,
        strategyConfig: ExecutionPolicy,
        snapshotConfig: SnapshotConfig,
        icOptionsConfigAction: ((JvmSnapshotBasedIncrementalCompilationOptions) -> Unit),
        icSourceTracking: Boolean,
    ): BaseScenarioModule {
        module.compileIncrementally(
            if (icSourceTracking) SourcesChanges.ToBeCalculated else SourcesChanges.Unknown,
            strategyConfig,
            icOptionsConfigAction = icOptionsConfigAction
        )
        val initialOutputs = mutableSetOf<String>()
        for (file in module.outputDirectory.walk()) {
            if (!file.isRegularFile()) continue
            initialOutputs.add(file.relativeTo(module.outputDirectory).toString())
        }
        val moduleCacheDirectory = globalTempDirectory.resolve(UUID.randomUUID().toString())
        module.buildDirectory.copyToRecursively(moduleCacheDirectory, followLinks = false, overwrite = false)
        compiledProjectsCache[GlobalCompiledProjectsCacheKey(
            module.scenarioDslCacheKey,
            snapshotConfig,
            icOptionsConfigAction,
            icSourceTracking,
        )] = Pair(initialOutputs, moduleCacheDirectory)
        return if (icSourceTracking) {
            AutoTrackedScenarioModuleImpl(module, initialOutputs, strategyConfig, icOptionsConfigAction)
        } else {
            ExternallyTrackedScenarioModuleImpl(module, initialOutputs, strategyConfig, icOptionsConfigAction)
        }
    }
}
