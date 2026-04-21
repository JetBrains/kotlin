/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

internal abstract class BaseScenarioModule<B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder> private constructor(
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

    val outputFiles: Set<String> get() = outputs.map { it.relativeFilePath }.toSet()

    fun addOutputFiles(outputRelativePaths: Set<String>) {
        outputRelativePaths.forEach { outputRelativePath ->
            val file = module.outputDirectory.resolve(outputRelativePath)
            outputs.add(FileKey(outputRelativePath, file.getLastModifiedTime().toMillis(), file.fileSize()))
        }
    }

    fun removeOutputFiles(outputRelativePaths: Set<String>) {
        outputs.removeAll { it.relativeFilePath in outputRelativePaths }
    }

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

internal class ExternallyTrackedScenarioModuleImpl<B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder>(
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
        removedFiles = outputs.map { module.outputDirectory.resolve(it.relativeFilePath).absolute().toFile() }.filter { !it.exists() }
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

internal class AutoTrackedScenarioModuleImpl<B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder>(
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
    override val kotlinToolchains: KotlinToolchains,
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
    action(JvmScenarioDsl(JvmProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig, kotlinToolchains))
}

fun BaseCompilationTest.jvmScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jvmScenario(executionStrategy.first, executionStrategy.second, action)
}

