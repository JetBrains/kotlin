/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.tests.buildToolsVersion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.*
import org.jetbrains.kotlin.buildtools.api.tests.reportsCompiledSources
import org.junit.jupiter.api.Assertions
import java.io.File
import java.nio.file.Path
import java.util.LinkedHashMap
import kotlin.io.path.*

abstract class BaseScenarioDsl<T : CompilationResultDsl>(
    protected val testInstance: BaseCompilationTest<*>,
    private val buildRunnerProvider: BuildRunnerProvider,
    private val workingDirectory: Path,
    private val project: Project,
) {
    private val modules = LinkedHashMap<String, Module>()
    protected val moduleDependencies = hashMapOf<Module, MutableSet<Module>>()
    protected var resultAssertions = hashMapOf<Module, Pair<CompilationResult, T.() -> Unit>>()

    inner class ModuleDsl(private val module: Module) {
        /**
         * Defines a dependency from this module to another module.
         *
         * Please keep track of circular dependencies yourself as well as making complex ordering.
         *
         * While this is possible, it will lead to errors:
         * ```
         * val module1 = module("jvm-module1")
         * val module2 = module("jvm-module2")
         * module("jvm-module1") {
         *     implementationDependency(module2)
         * }
         * ```
         * If you really need it for some reason, you're welcome to change [BaseScenarioDsl.compileAll], it's not hard :)
         */
        fun implementationDependency(anotherModule: Module) {
            moduleDependencies.computeIfAbsent(module) {
                hashSetOf()
            }.add(anotherModule)
        }
    }

    /**
     * You must define an expected result for each module registered in the scenario in order [compileAll] to work correctly.
     */
    inner class CompilationAssertionsDsl {
        fun expectSuccess(module: Module, assertions: T.() -> Unit = {}) {
            resultAssertions[module] = CompilationResult.COMPILATION_SUCCESS to assertions
        }

        fun expectCompilationFail(module: Module, assertions: T.() -> Unit = {}) {
            resultAssertions[module] = CompilationResult.COMPILATION_ERROR to assertions
        }
    }

    /**
     * Defines a module for this scenario.
     * It's safe to call it several times for the same module name.
     */
    fun module(moduleName: String, init: ModuleDsl.() -> Unit = {}): Module {
        val module = modules.computeIfAbsent(moduleName) {
            prepareModule(moduleName, workingDirectory)
        }
        ModuleDsl(module).init()
        return module
    }

    /**
     * Compiles all registered modules in the order they were defined.
     * @see CompilationAssertionsDsl
     */
    fun compileAll(configureAssertions: CompilationAssertionsDsl.() -> Unit) {
        CompilationAssertionsDsl().configureAssertions()
        val buildRunner = buildRunnerProvider(project)
        // the DSL requires dependency modules to be declared first, and we preserve the order of modules' declaration,
        // so we shouldn't care about compilation order
        for (module in modules.values) {
            compileModule(buildRunner, module)
        }
        onAllModulesCompiled()
    }

    abstract fun compileModule(buildRunner: BuildRunner, module: Module)

    open fun onAllModulesCompiled() {
        resultAssertions.clear()
    }
}

open class CompilationResultDsl(
    protected val module: Module,
    private val logs: Map<LogLevel, Collection<String>>,
) {
    /**
     * @see [outputFiles(java.lang.String...)]
     */
    fun outputFiles(compiledFiles: Set<String>) {
        val filesLeft = compiledFiles.map { module.outputDirectory.resolve(it).relativeTo(module.outputDirectory) }
            .toMutableSet()
            .apply {
                add(module.outputDirectory.resolve("META-INF/${module.moduleName}.kotlin_module").relativeTo(module.outputDirectory))
            }
        val notDeclaredFiles = hashSetOf<Path>()
        for (file in module.outputDirectory.walk()) {
            if (!file.isRegularFile()) continue
            val currentFile = file.relativeTo(module.outputDirectory)
            filesLeft.remove(currentFile).also { wasPreviously ->
                if (!wasPreviously) notDeclaredFiles.add(currentFile)
            }
        }
        Assertions.assertEquals(emptySet<String>(), filesLeft) {
            "The following files were declared as expected, but not actually produced: $filesLeft"
        }
        Assertions.assertEquals(emptySet<String>(), notDeclaredFiles) {
            "The following files weren't declared as expected output files: $notDeclaredFiles"
        }
    }

    /**
     * Defines expected compilation results.
     *
     * The expected form is a path relative to [Module.outputDirectory]
     */
    fun outputFiles(vararg compiledFiles: String) {
        outputFiles(compiledFiles.toSet())
    }

    fun assertLogs(assertions: Map<LogLevel, Collection<String>>.() -> Unit) {
        logs.assertions()
    }
}

class IncrementalCompilationResultDsl(
    module: Module,
    logs: Map<LogLevel, Collection<String>>,
    private val compiledSources: Set<String>,
    private val workingDirectory: Path,
) : CompilationResultDsl(module, logs) {
    /**
     * @see [compiledSources(java.lang.String...)]
     */
    fun compiledSources(expectedSources: Set<String>) {
        if (buildToolsVersion.reportsCompiledSources) {
            Assertions.assertEquals(
                expectedSources.map { module.sourcesDirectory.resolve(it).relativeTo(workingDirectory).toString() }.toSet(),
                compiledSources
            )
        }
    }

    /**
     * Defines expected sources that should be compiled during incremental compilation.
     *
     * The expected form is a path relative to [Module.sourcesDirectory]
     */
    fun compiledSources(vararg expectedSources: String) {
        compiledSources(expectedSources.toSet())
    }
}

abstract class BaseNonIncrementalScenarioDsl(
    testInstance: BaseCompilationTest<*>,
    buildRunnerProvider: BuildRunnerProvider,
    workingDirectory: Path,
    project: Project,
) : BaseScenarioDsl<CompilationResultDsl>(testInstance, buildRunnerProvider, workingDirectory, project) {
    override fun compileModule(buildRunner: BuildRunner, module: Module) {
        val dependencies = moduleDependencies[module] ?: emptySet()
        with(testInstance) {
            module.compileImpl(
                buildRunner,
                dependencies = dependencies,
            ) { result, logs ->
                resultAssertions.getValue(module).let { (expectedResult, providedAssertions) ->
                    Assertions.assertEquals(expectedResult, result)
                    CompilationResultDsl(module, logs).providedAssertions()
                }
            }
        }
    }
}

class DefaultNonIncrementalScenarioDsl(
    testInstance: BaseCompilationTest<DefaultNonIncrementalScenarioDsl>,
    buildRunnerProvider: BuildRunnerProvider,
    workingDirectory: Path,
    project: Project,
) : BaseNonIncrementalScenarioDsl(testInstance, buildRunnerProvider, workingDirectory, project)

abstract class BaseIncrementalScenarioDsl(
    private val incrementalTestInstance: BaseIncrementalCompilationTest<*>,
    buildRunnerProvider: BuildRunnerProvider,
    workingDirectory: Path,
    project: Project,
) : BaseScenarioDsl<IncrementalCompilationResultDsl>(incrementalTestInstance, buildRunnerProvider, workingDirectory, project) {
    private val changedSources = hashMapOf<Module, MutableList<File>>()
    private val removedSources = hashMapOf<Module, MutableList<File>>()
    private var firstBuild = true

    override fun compileModule(buildRunner: BuildRunner, module: Module) {
        val dependencies = moduleDependencies[module] ?: emptySet()
        with(incrementalTestInstance) {
            module.compileIncrementallyImpl(
                buildRunner,
                sourcesChanges = if (firstBuild) {
                    SourcesChanges.Unknown
                } else {
                    SourcesChanges.Known(changedSources[module] ?: emptyList(), removedSources[module] ?: emptyList())
                },
                dependencies = dependencies
            ) { result, logs, compiledSources ->
                resultAssertions.getValue(module).let { (expectedResult, providedAssertions) ->
                    Assertions.assertEquals(expectedResult, result)
                    IncrementalCompilationResultDsl(module, logs, compiledSources, workingDirectory).providedAssertions()
                }
            }
        }
    }

    /**
     * Performs registered existing file modification.
     */
    fun changeFile(module: Module, fileName: String, transform: (String) -> String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(transform(file.readText()))
        changedSources.computeIfAbsent(module) {
            arrayListOf()
        }.add(file.toFile())
    }

    /**
     * Performs registered file deletion.
     */
    fun deleteFile(module: Module, fileName: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.deleteExisting()
        removedSources.computeIfAbsent(module) {
            arrayListOf()
        }.add(file.toFile())
    }

    /**
     * Performs registered new file creation.
     */
    fun createFile(module: Module, fileName: String, content: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(content)
        changedSources.computeIfAbsent(module) {
            arrayListOf()
        }.add(file.toFile())
    }

    override fun onAllModulesCompiled() {
        super.onAllModulesCompiled()
        firstBuild = false
        changedSources.clear()
        removedSources.clear()
    }
}

class DefaultIncrementalScenarioDsl(
    incrementalTestInstance: BaseIncrementalCompilationTest<DefaultIncrementalScenarioDsl>,
    buildRunnerProvider: BuildRunnerProvider,
    workingDirectory: Path,
    project: Project,
) : BaseIncrementalScenarioDsl(incrementalTestInstance, buildRunnerProvider, workingDirectory, project)