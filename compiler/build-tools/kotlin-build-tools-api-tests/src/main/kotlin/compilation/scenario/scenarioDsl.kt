/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal abstract class BaseScenarioModule<B : BaseCompilationOperation.Builder, out IC : BaseIncrementalCompilationConfiguration.Builder> private constructor(
    internal val module: Module<*, B, IC>,
    internal val outputs: MutableSet<String>,
    private val strategyConfig: ExecutionPolicy,
    private val icOptionsConfigAction: ((IC) -> Unit),
) : ScenarioModule<B, IC> {
    // make a copy of the outputs to avoid them being shared between different tests
    constructor(
        module: Module<*, B, IC>,
        outputs: Collection<String>,
        strategyConfig: ExecutionPolicy,
        icOptionsConfigAction: (IC) -> Unit,
    ) : this(module, outputs.toMutableSet(), strategyConfig, icOptionsConfigAction)

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
    }

    override fun deleteFile(fileName: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.deleteExisting()
    }

    override fun createFile(fileName: String, content: String) {
        writeFile(fileName, content)
    }

    override fun createPredefinedFile(fileName: String, version: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        val chosenRevision = module.sourcesDirectory.resolve("$fileName.$version")
        Files.copy(chosenRevision, file)
    }

    protected open fun writeFile(fileName: String, newContent: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(newContent)
    }

    protected abstract fun getSourcesChanges(): SourcesChanges

    override fun compile(
        forceOutput: LogLevel?,
        assertions: context(Module<*, *, *>, ScenarioModule<B, IC>) CompilationOutcome.() -> Unit,
    ) {
        module.compileIncrementally(
            getSourcesChanges(),
            strategyConfig,
            forceOutput,
            icOptionsConfigAction = icOptionsConfigAction,
            assertions = {
                assertions(this)
            })
    }

    override fun executeCompiledCode(
        mainClassFqn: String,
        assertions: ExecutionOutcome.() -> Unit,
    ) {
        module.executeCompiledClass(
            mainClassFqn,
            assertions
        )
    }
}

internal class ExternallyTrackedScenarioModuleImpl<B : BaseCompilationOperation.Builder, out IC : BaseIncrementalCompilationConfiguration.Builder>(
    module: Module<*, B, IC>,
    outputs: MutableSet<String>,
    strategyConfig: ExecutionPolicy,
    icOptionsConfigAction: (IC) -> Unit,
) : BaseScenarioModule<B, IC>(module, outputs, strategyConfig, icOptionsConfigAction) {
    private var sourcesChanges = SourcesChanges.Known(emptyList(), emptyList())

    override fun replaceFileWithVersion(fileName: String, version: String) {
        super.replaceFileWithVersion(fileName, version)
        val file = module.sourcesDirectory.resolve(fileName)
        addToModifiedFiles(file)
    }

    override fun deleteFile(fileName: String) {
        super.deleteFile(fileName)
        val file = module.sourcesDirectory.resolve(fileName)
        addToRemovedFiles(file)
    }

    override fun createPredefinedFile(fileName: String, version: String) {
        super.createPredefinedFile(fileName, version)
        val file = module.sourcesDirectory.resolve(fileName)
        addToModifiedFiles(file)
    }

    override fun writeFile(fileName: String, newContent: String) {
        super.writeFile(fileName, newContent)
        val file = module.sourcesDirectory.resolve(fileName)
        addToModifiedFiles(file)
    }

    override fun getSourcesChanges() = sourcesChanges

    override fun compile(
        forceOutput: LogLevel?,
        assertions: context(Module<*, *, *>, ScenarioModule<B, IC>) CompilationOutcome.() -> Unit,
    ) {
        super.compile(forceOutput) {
            assertions()

            if (actualResult == CompilationResult.COMPILATION_SUCCESS) {
                sourcesChanges = SourcesChanges.Known(emptyList(), emptyList())
            }
        }
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
}

internal class AutoTrackedScenarioModuleImpl<B: BaseCompilationOperation.Builder, out IC : BaseIncrementalCompilationConfiguration.Builder>(
    module: Module<*, B, IC>,
    outputs: MutableSet<String>,
    strategyConfig: ExecutionPolicy,
    icOptionsConfigAction: (IC) -> Unit,
) : BaseScenarioModule<B, IC>(module, outputs, strategyConfig, icOptionsConfigAction) {
    override fun getSourcesChanges() = SourcesChanges.ToBeCalculated
}

private class JvmScenarioDsl(
    private val project: JvmProject,
    private val strategyConfig: ExecutionPolicy,
) : Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder> {
    @Synchronized
    override fun module(
        moduleName: String,
        dependencies: List<ScenarioModule<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JvmCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder> {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, false)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, false)
    }

    @Synchronized
    override fun trackedModule(
        moduleName: String,
        dependencies: List<ScenarioModule<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JvmCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder> {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, true)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, true)
    }
}

fun BaseCompilationTest.jvmScenario(kotlinToolchains: KotlinToolchains, strategyConfig: ExecutionPolicy, action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit) {
    action(JvmScenarioDsl(JvmProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig))
}

fun BaseCompilationTest.jvmScenario(executionStrategy: CompilerExecutionStrategyConfiguration, action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit) {
    jvmScenario(executionStrategy.first, executionStrategy.second, action)
}

private class JsScenarioDsl(
    private val project: JsProject,
    private val strategyConfig: ExecutionPolicy,
) : Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder> {
    @Synchronized
    override fun module(
        moduleName: String,
        dependencies: List<ScenarioModule<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder> {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, false)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, false)
    }

    @Synchronized
    override fun trackedModule(
        moduleName: String,
        dependencies: List<ScenarioModule<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder> {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, true)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig, snapshotConfig, icOptionsConfigAction, true)
    }
}

fun BaseCompilationTest.jsScenario(kotlinToolchains: KotlinToolchains, strategyConfig: ExecutionPolicy, action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit) {
    action(JsScenarioDsl(JsProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig))
}

fun BaseCompilationTest.jsScenario(executionStrategy: CompilerExecutionStrategyConfiguration, action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit) {
    jsScenario(executionStrategy.first, executionStrategy.second, action)
}
