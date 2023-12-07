/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Project
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

private class ScenarioModuleImpl(
    val module: Module,
    val outputs: MutableSet<String>,
    private val strategyConfig: CompilerExecutionStrategyConfiguration,
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
        module.compileIncrementally(strategyConfig, sourcesChanges, forceOutput, assertions = {
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
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as ScenarioModuleImpl).module }
        val module = project.module(moduleName, transformedDependencies, additionalCompilationArguments)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig)
    }
}

fun BaseCompilationTest.scenario(strategyConfig: CompilerExecutionStrategyConfiguration, action: Scenario.() -> Unit) {
    action(ScenarioDsl(Project(workingDirectory), strategyConfig))
}

private object GlobalCompiledProjectsCache {
    private val globalTempDirectory = Files.createTempDirectory("compiled-test-projects-cache").apply {
        toFile().deleteOnExit()
    }
    private val compiledProjectsCache = mutableMapOf<Module, Pair<MutableSet<String>, Path>>()

    fun getProjectFromCache(module: Module, strategyConfig: CompilerExecutionStrategyConfiguration): ScenarioModuleImpl? {
        val (initialOutputs, cachedBuildDirPath) = compiledProjectsCache[module] ?: return null
        cachedBuildDirPath.copyToRecursively(module.buildDirectory, followLinks = false, overwrite = true)
        return ScenarioModuleImpl(module, initialOutputs, strategyConfig)
    }

    fun putProjectIntoCache(module: Module, strategyConfig: CompilerExecutionStrategyConfiguration): ScenarioModuleImpl {
        module.compileIncrementally(strategyConfig, SourcesChanges.Unknown)
        val initialOutputs = mutableSetOf<String>()
        for (file in module.outputDirectory.walk()) {
            if (!file.isRegularFile()) continue
            initialOutputs.add(file.relativeTo(module.outputDirectory).toString())
        }
        val moduleCacheDirectory = globalTempDirectory.resolve(UUID.randomUUID().toString())
        module.buildDirectory.copyToRecursively(moduleCacheDirectory, followLinks = false, overwrite = false)
        compiledProjectsCache[module] = Pair(initialOutputs, moduleCacheDirectory)
        return ScenarioModuleImpl(module, initialOutputs, strategyConfig)
    }
}