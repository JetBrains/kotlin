/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DependencyScenarioDslCacheKey
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.tests.compilation.model.SnapshotConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

private data class GlobalCompiledProjectsCacheKey<IC : BaseIncrementalCompilationConfiguration.Builder>(
    val moduleKey: DependencyScenarioDslCacheKey,
    val snapshotConfig: SnapshotConfig,
    val icOptionsConfigAction: ((IC) -> Unit)?,
    val icSourceTracking: Boolean,
)

internal object GlobalCompiledProjectsCache {
    private val globalTempDirectory = Files.createTempDirectory("compiled-test-projects-cache").apply {
        toFile().deleteOnExit()
    }
    private val compiledProjectsCache = mutableMapOf<GlobalCompiledProjectsCacheKey<*>, Pair<MutableSet<FileKey>, Path>>()

    fun <B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder> getProjectFromCache(
        module: Module<*, B, IC>,
        strategyConfig: ExecutionPolicy,
        snapshotConfig: SnapshotConfig,
        icOptionsConfigAction: ((IC) -> Unit),
        icSourceTracking: Boolean,
        dependencies: List<ScenarioModule>,
    ): BaseScenarioModule<B, IC>? {
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
            ExternallyTrackedScenarioModuleImpl(module, initialOutputs, strategyConfig, icOptionsConfigAction, dependencies)
        }
    }

    fun <B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder> putProjectIntoCache(
        module: Module<*, B, IC>,
        strategyConfig: ExecutionPolicy,
        snapshotConfig: SnapshotConfig,
        icOptionsConfigAction: (IC) -> Unit,
        icSourceTracking: Boolean,
        dependencies: List<ScenarioModule>,
    ): BaseScenarioModule<B, IC> {
        module.compileIncrementally(
            if (icSourceTracking) SourcesChanges.ToBeCalculated else SourcesChanges.Unknown,
            strategyConfig,
            icOptionsConfigAction = icOptionsConfigAction
        )
        val initialOutputs = mutableSetOf<FileKey>()
        for (file in module.outputDirectory.walk()) {
            if (!file.isRegularFile()) continue
            initialOutputs.add(
                FileKey(
                    file.relativeTo(module.outputDirectory).toString(),
                    file.getLastModifiedTime().toMillis(),
                    file.fileSize()
                )
            )
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
            ExternallyTrackedScenarioModuleImpl(module, initialOutputs, strategyConfig, icOptionsConfigAction, dependencies)
        }
    }
}

data class FileKey(val relativeFilePath: String, val lastModified: Long, val size: Long)
