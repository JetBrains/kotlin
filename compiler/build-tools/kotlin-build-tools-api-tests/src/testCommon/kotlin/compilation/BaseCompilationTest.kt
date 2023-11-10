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
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

abstract class BaseCompilationTest {
    @TempDir
    lateinit var workingDirectory: Path

    val project = Project()

    fun Module.compileImpl(
        runner: BuildRunner,
        dependencies: Set<Module> = emptySet(),
        assertions: (CompilationResult, Map<LogLevel, Collection<String>>) -> Unit = { _, _ -> },
    ) {
        val result = runner.compileModule(this, dependencies)

        if (buildToolsVersion >= KotlinToolingVersion(2, 0, 0, "Beta2")) {
            assertEquals(
                runner.selectedExecutionStrategy == ExecutionStrategy.IN_PROCESS,
                runner.testLogger.loggedMessages.getValue(LogLevel.DEBUG).contains("Compiling using the in-process strategy")
            )
            assertEquals(
                runner.selectedExecutionStrategy == ExecutionStrategy.DAEMON,
                runner.testLogger.loggedMessages.getValue(LogLevel.DEBUG).contains("Compiling using the daemon strategy")
            )
        }

        assertions(result, runner.testLogger.loggedMessages)
    }

    inner class ScenarioDsl(private val buildRunnerProvider: BuildRunnerProvider) {
        private val modules = LinkedHashMap<String, Module>()
        private val moduleDependencies = hashMapOf<Module, MutableSet<Module>>()
        private val changedSources = hashMapOf<Module, MutableList<File>>()
        private val removedSources = hashMapOf<Module, MutableList<File>>()
        private var resultAssertions = hashMapOf<Module, Pair<CompilationResult, CompilationResultDsl.() -> Unit>>()
        private var firstBuild = true

        inner class ModuleDsl(private val module: Module) {
            fun dependsOn(anotherModule: Module) {
                moduleDependencies.computeIfAbsent(module) {
                    hashSetOf()
                }.add(anotherModule)
            }
        }

        inner class CompilationAssertionsDsl {
            fun expectSuccess(module: Module, assertions: CompilationResultDsl.() -> Unit) {
                resultAssertions[module] = CompilationResult.COMPILATION_SUCCESS to assertions
            }

            fun expectCompilationFail(module: Module, assertions: CompilationResultDsl.() -> Unit) {
                resultAssertions[module] = CompilationResult.COMPILATION_ERROR to assertions
            }
        }

        inner class CompilationResultDsl(
            private val module: Module,
            private val logs: Map<LogLevel, Collection<String>>,
            private val compiledSources: Set<String>,
        ) {
            fun compiledSources(expectedSources: Set<String>) {
                check(this@BaseCompilationTest is IncrementalBaseCompilationTest) {
                    "Checking compiled sources within non-incremental tests is not available and doesn't make much sense"
                }
                if (buildToolsVersion.reportsCompiledSources) {
                    assertEquals(
                        expectedSources.map { module.sourcesDirectory.resolve(it).relativeTo(workingDirectory).toString() }.toSet(),
                        compiledSources
                    )
                }
            }

            fun compiledSources(vararg expectedSources: String) {
                compiledSources(expectedSources.toSet())
            }

            fun outputFiles(compiledFiles: Set<String>) {
                val filesLeft = compiledFiles.toMutableSet().apply {
                    add("META-INF/${module.moduleName}.kotlin_module")
                }
                val notDeclaredFiles = hashSetOf<String>()
                for (file in module.outputDirectory.walk()) {
                    if (!file.isRegularFile()) continue
                    val currentFile = file.relativeTo(module.outputDirectory).toString()
                    filesLeft.remove(currentFile).also { wasPreviously ->
                        if (!wasPreviously) notDeclaredFiles.add(currentFile)
                    }
                }
                assertEquals(emptySet<String>(), filesLeft) {
                    "The following files were declared as expected, but not actually produced: $filesLeft"
                }
                assertEquals(emptySet<String>(), notDeclaredFiles) {
                    "The following files weren't declared as expected output files: $notDeclaredFiles"
                }
            }

            fun outputFiles(vararg compiledFiles: String) {
                outputFiles(compiledFiles.toSet())
            }

            fun assertLogs(assertions: Map<LogLevel, Collection<String>>.() -> Unit) {
                logs.assertions()
            }
        }

        fun module(moduleName: String, init: ModuleDsl.() -> Unit = {}): Module {
            val module = modules.computeIfAbsent(moduleName) {
                prepareModule(moduleName, workingDirectory)
            }
            ModuleDsl(module).init()
            return module
        }

        fun compileAll(configureAssertions: CompilationAssertionsDsl.() -> Unit) {
            CompilationAssertionsDsl().configureAssertions()
            val buildRunner = buildRunnerProvider(project)
            // the DSL requires dependency modules to be declared first, and we preserve the order of modules' declaration,
            // so we shouldn't care about compilation order
            for (module in modules.values) {
                val dependencies = moduleDependencies[module] ?: emptySet()
                if (this@BaseCompilationTest is IncrementalBaseCompilationTest) {
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
                            assertEquals(expectedResult, result)
                            CompilationResultDsl(module, logs, compiledSources).providedAssertions()
                        }
                    }
                } else {
                    module.compileImpl(
                        buildRunner,
                        dependencies = dependencies,
                    ) { result, logs ->
                        resultAssertions.getValue(module).let { (expectedResult, providedAssertions) ->
                            assertEquals(expectedResult, result)
                            CompilationResultDsl(module, logs, emptySet()).providedAssertions()
                        }
                    }
                }
            }
            firstBuild = false
            changedSources.clear()
            removedSources.clear()
            resultAssertions.clear()
        }

        fun changeFile(module: Module, fileName: String, transform: (String) -> String) {
            val file = module.sourcesDirectory.resolve(fileName)
            file.writeText(transform(file.readText()))
            changedSources.computeIfAbsent(module) {
                arrayListOf()
            }.add(file.toFile())
        }

        fun deleteFile(module: Module, fileName: String) {
            val file = module.sourcesDirectory.resolve(fileName)
            file.deleteExisting()
            removedSources.computeIfAbsent(module) {
                arrayListOf()
            }.add(file.toFile())
        }

        fun createFile(module: Module, fileName: String, content: String) {
            val file = module.sourcesDirectory.resolve(fileName)
            file.writeText(content)
            changedSources.computeIfAbsent(module) {
                arrayListOf()
            }.add(file.toFile())
        }
    }

    fun maybeSkip(buildRunnerProvider: BuildRunnerProvider) {
        if (this !is IncrementalBaseCompilationTest) return
        Assumptions.assumeFalse(
            buildRunnerProvider.strategy == ExecutionStrategy.IN_PROCESS && buildToolsVersion < KotlinToolingVersion(2, 0, 0, "Beta1"),
            "Skip the test for the versions when in-process IC wasn't supported"
        )
    }

    fun scenario(buildRunnerProvider: BuildRunnerProvider, init: ScenarioDsl.() -> Unit) {
        maybeSkip(buildRunnerProvider)
        ScenarioDsl(buildRunnerProvider).apply(init)
    }
}