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
import kotlin.io.path.*

internal abstract class BaseScenarioModule<B : BaseCompilationOperation.Builder, out IC : BaseIncrementalCompilationConfiguration.Builder> private constructor(
    internal val module: Module<*, B, IC>,
    internal val outputs: MutableSet<FileKey>,
    private val strategyConfig: ExecutionPolicy,
    private val icOptionsConfigAction: ((IC) -> Unit),
) : ScenarioModule {
    // make a copy of the outputs to avoid them being shared between different tests
    constructor(
        module: Module<*, B, IC>,
        outputs: Collection<FileKey>,
        strategyConfig: ExecutionPolicy,
        icOptionsConfigAction: (IC) -> Unit,
    ) : this(module, outputs.toMutableSet(), strategyConfig, icOptionsConfigAction)

    val outputFiles: MutableSet<String> get() = outputs.map { it.relativeFilePath }.toMutableSet()

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
        assertions: context(Module<*, *, *>, ScenarioModule) CompilationOutcome.() -> Unit,
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
    outputs: MutableSet<FileKey>,
    strategyConfig: ExecutionPolicy,
    icOptionsConfigAction: (IC) -> Unit,
    val dependencies: List<ScenarioModule>,
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

    fun getOutputChanges() = SourcesChanges.Known(
        modifiedFiles = module.outputDirectory.walk().toList().filter {
            val name = it.relativeTo(module.outputDirectory).toString()
            !outputs.contains(FileKey(name, it.getLastModifiedTime().toMillis(), it.fileSize()))
        }.map { it.absolute().toFile() },
        removedFiles = outputs.map { module.outputDirectory.resolve(it.relativeFilePath).toFile() }.filter { !it.exists() }
    )

    override fun getSourcesChanges() =
        sourcesChanges + dependencies.filterIsInstance<ExternallyTrackedScenarioModuleImpl<*, *>>().map { it.getOutputChanges() }
            .fold(SourcesChanges.Known(emptyList(), emptyList())) { acc, changes -> acc + changes }

    operator fun SourcesChanges.Known.plus(other: SourcesChanges.Known): SourcesChanges.Known = SourcesChanges.Known(
        this.modifiedFiles + other.modifiedFiles,
        this.removedFiles + other.removedFiles,
    )


    override fun compile(
        forceOutput: LogLevel?,
        assertions: context(Module<*, *, *>, ScenarioModule) CompilationOutcome.() -> Unit,
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

internal class AutoTrackedScenarioModuleImpl<B : BaseCompilationOperation.Builder, out IC : BaseIncrementalCompilationConfiguration.Builder>(
    module: Module<*, B, IC>,
    outputs: MutableSet<FileKey>,
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
        dependencies: List<ScenarioModule>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JvmCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule<*, *>).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(
            module,
            strategyConfig,
            snapshotConfig,
            icOptionsConfigAction,
            false,
            dependencies,
        )
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                module,
                strategyConfig,
                snapshotConfig,
                icOptionsConfigAction,
                false,
                dependencies,
            )
    }

    @Synchronized
    override fun trackedModule(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JvmCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule<*, *>).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(
            module,
            strategyConfig,
            snapshotConfig,
            icOptionsConfigAction,
            true,
            dependencies,
        )
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                module,
                strategyConfig,
                snapshotConfig,
                icOptionsConfigAction,
                true,
                dependencies,
            )
    }
}

fun BaseCompilationTest.jvmScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    action(JvmScenarioDsl(JvmProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig))
}

fun BaseCompilationTest.jvmScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jvmScenario(executionStrategy.first, executionStrategy.second, action)
}

private class JsScenarioDsl(
    private val project: JsProject,
    private val strategyConfig: ExecutionPolicy,
) : Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder> {
    @Synchronized
    override fun module(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule<*, *>).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(
            module,
            strategyConfig,
            snapshotConfig,
            icOptionsConfigAction,
            false,
            dependencies,
        )
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                module,
                strategyConfig,
                snapshotConfig,
                icOptionsConfigAction,
                false,
                dependencies,
            )
    }

    @Synchronized
    override fun trackedModule(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule<*, *>).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(
            module,
            strategyConfig,
            snapshotConfig,
            icOptionsConfigAction,
            true,
            dependencies,
        )
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                module,
                strategyConfig,
                snapshotConfig,
                icOptionsConfigAction,
                true,
                dependencies,
            )
    }

    override fun modules(vararg moduleSpecs: ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>): List<ScenarioModule> {
        return modules(tracked = false, *moduleSpecs)
    }

    private fun modules(
        tracked: Boolean,
        vararg moduleSpecs: ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>,
    ): List<ScenarioModule> {
        val specsToModules: MutableList<Pair<ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>, JsModule>> =
            moduleSpecs.fold(mutableListOf()) { acc, spec ->
                val transformedDependencies = spec.dependencies.map { dependency -> acc.first { it.first.moduleName == dependency }.second }
                acc.add(
                    spec to project.module(
                        spec.moduleName,
                        transformedDependencies,
                        spec.snapshotConfig,
                        moduleCompilationConfigAction = spec.compilationConfigAction
                    )
                )
                acc
            }
        val modules = specsToModules.map { it.second }
        modules.forEach { module ->
            module.otherModules.addAll(modules.minus(module))
        }
        return specsToModules.fold(mutableListOf<ScenarioModule>()) { acc, pair ->
            val spec = pair.first
            val module = pair.second
            acc.add(
                GlobalCompiledProjectsCache.getProjectFromCache(
                    module,
                    strategyConfig,
                    spec.snapshotConfig,
                    spec.icOptionsConfigAction,
                    tracked,
                    acc.filter { (it as BaseScenarioModule<*, *>).module.moduleName in spec.dependencies },
                )
                    ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                        module,
                        strategyConfig,
                        spec.snapshotConfig,
                        spec.icOptionsConfigAction,
                        tracked,
                        acc.filter { (it as BaseScenarioModule<*, *>).module.moduleName in spec.dependencies },
                    )
            )
            acc
        }
    }

    override fun trackedModules(vararg moduleSpecs: ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>): List<ScenarioModule> {
        return modules(tracked = true, *moduleSpecs)
    }
}

fun BaseCompilationTest.jsScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    action(JsScenarioDsl(JsProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig))
}

fun BaseCompilationTest.jsScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jsScenario(executionStrategy.first, executionStrategy.second, action)
}
