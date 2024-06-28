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
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

internal class ScenarioModuleImpl(
    internal val module: Module,
    internal val outputs: MutableSet<String>,
    private val strategyConfig: CompilerExecutionStrategyConfiguration,
    private val compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
    private val incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
) : ScenarioModule {
    override fun changeFile(
        fileName: String,
        transform: (String) -> String,
    ) {
        val file = module.sourcesDirectory.resolve(fileName)
        writeFile(fileName, transform(file.readText()))
    }

    override fun replaceFileWithVersion(fileName: String, version: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        val chosenRevision = module.sourcesDirectory.resolve("$fileName.$version")
        Files.delete(file)
        Files.copy(chosenRevision, file)
        addToModifiedFiles(file)
    }

    override fun deleteFile(fileName: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.deleteExisting()
        addToRemovedFiles(file)
    }

    override fun createFile(fileName: String, content: String) {
        writeFile(fileName, content)
    }

    override fun createPredefinedFile(fileName: String, version: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        val chosenRevision = module.sourcesDirectory.resolve("$fileName.$version")
        Files.copy(chosenRevision, file)
        addToModifiedFiles(file)
    }

    private fun writeFile(fileName: String, newContent: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(newContent)
        addToModifiedFiles(file)
    }

    private fun addToModifiedFiles(file: Path) {
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles + file.toFile(),
            removedFiles = sourcesChanges.removedFiles,
        )
    }

    private fun addToRemovedFiles(file: Path) {
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles,
            removedFiles = sourcesChanges.removedFiles + file.toFile(),
        )
    }

    private var sourcesChanges = SourcesChanges.Known(emptyList(), emptyList())

    override fun compile(
        forceOutput: LogLevel?,
        assertions: CompilationOutcome.(Module, ScenarioModule) -> Unit,
    ) {
        module.compileIncrementally(
            sourcesChanges,
            strategyConfig,
            forceOutput,
            compilationConfigAction = { compilationOptionsModifier?.invoke(it) },
            incrementalCompilationConfigAction = { incrementalCompilationOptionsModifier?.invoke(it) },
            assertions = {
                assertions(this, module, this@ScenarioModuleImpl)
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