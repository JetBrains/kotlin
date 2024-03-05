/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

private class ScenarioModuleImpl(
    val module: Module,
    val outputs: MutableSet<String>,
    private val strategyConfig: CompilerExecutionStrategyConfiguration,
    private val compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
    private val incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
) : ScenarioModule {
    override fun changeFile(
        fileName: String,
        addedOutputs: Set<String>,
        removedOutputs: Set<String>,
        transform: (String) -> String,
    ) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(transform(file.readText()))
        outputs.addAll(addedOutputs)
        outputs.removeAll(removedOutputs)
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles + file.toFile(),
            removedFiles = sourcesChanges.removedFiles,
        )
    }

    override fun deleteFile(fileName: String, removedOutputs: Set<String>) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.deleteExisting()
        outputs.removeAll(removedOutputs)
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles,
            removedFiles = sourcesChanges.removedFiles + file.toFile(),
        )
    }

    override fun createFile(fileName: String, addedOutputs: Set<String>, content: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(content)
        outputs.addAll(addedOutputs)
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles + file.toFile(),
            removedFiles = sourcesChanges.removedFiles,
        )
    }

    var sourcesChanges = SourcesChanges.Known(emptyList(), emptyList())

    override fun compile(
        forceOutput: LogLevel?,
        assertions: context(Module) CompilationOutcome.() -> Unit,
    ) {
        module.compileIncrementally(
            sourcesChanges,
            strategyConfig,
            forceOutput,
            compilationConfigAction = { compilationOptionsModifier?.invoke(it) },
            incrementalCompilationConfigAction = { incrementalCompilationOptionsModifier?.invoke(it) },
            assertions = {
                assertions(module, this)
                assertOutputs(outputs)
            })
    }
}

private class ScenarioDsl(
    private val project: Project,
    private val strategyConfig: CompilerExecutionStrategyConfiguration,
) : Scenario {
    @Synchronized
    override fun module(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        additionalCompilationArguments: List<String>,
        compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
        incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as ScenarioModuleImpl).module }
        val module =
            project.module(moduleName, transformedDependencies, additionalCompilationArguments)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig, compilationOptionsModifier, incrementalCompilationOptionsModifier)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig, compilationOptionsModifier, incrementalCompilationOptionsModifier)
    }
}

fun BaseCompilationTest.scenario(strategyConfig: CompilerExecutionStrategyConfiguration, action: Scenario.() -> Unit) {
    action(ScenarioDsl(Project(strategyConfig, workingDirectory), strategyConfig))
}

private data class GlobalCompiledProjectsCacheKey(
    val moduleKey: DependencyScenarioDslCacheKey,
    val compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
    val incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
)

private object GlobalCompiledProjectsCache {
    private val globalTempDirectory = Files.createTempDirectory("compiled-test-projects-cache").apply {
        toFile().deleteOnExit()
    }
    private val compiledProjectsCache = mutableMapOf<GlobalCompiledProjectsCacheKey, Pair<MutableSet<String>, Path>>()

    fun getProjectFromCache(
        module: Module,
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
        incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
    ): ScenarioModuleImpl? {
        val (initialOutputs, cachedBuildDirPath) = compiledProjectsCache[GlobalCompiledProjectsCacheKey(
            module.scenarioDslCacheKey,
            compilationOptionsModifier,
            incrementalCompilationOptionsModifier
        )] ?: return null
        cachedBuildDirPath.copyToRecursively(module.buildDirectory, followLinks = false, overwrite = true)
        return ScenarioModuleImpl(module, initialOutputs, strategyConfig, compilationOptionsModifier, incrementalCompilationOptionsModifier)
    }

    fun putProjectIntoCache(
        module: Module,
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
        incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
    ): ScenarioModuleImpl {
        module.compileIncrementally(
            SourcesChanges.Unknown,
            strategyConfig,
            compilationConfigAction = { compilationOptionsModifier?.invoke(it) },
            incrementalCompilationConfigAction = { incrementalCompilationOptionsModifier?.invoke(it) }
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
            compilationOptionsModifier,
            incrementalCompilationOptionsModifier
        )] = Pair(initialOutputs, moduleCacheDirectory)
        return ScenarioModuleImpl(module, initialOutputs, strategyConfig, compilationOptionsModifier, incrementalCompilationOptionsModifier)
    }
}