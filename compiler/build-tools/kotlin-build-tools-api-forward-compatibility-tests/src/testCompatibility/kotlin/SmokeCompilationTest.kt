/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.classpathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.snapshotBasedIcConfiguration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.toPath
import kotlin.io.path.writeText

class SmokeCompilationTest : BaseCompilationTest() {
    @DisplayName("non-incremental compilation smoke forward compatibility test")
    @DefaultForwardCompatibilityCompilationTest
    fun testNonIncrementalCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val sourceA = workingDirectory.resolve("a.kt")
        val sourceB = workingDirectory.resolve("b.kt")

        val sources = listOf(
            sourceA.apply { writeText("fun a() = 42") },
            sourceB.apply { writeText("fun b() = a()") },
        )
        val destination = workingDirectory.resolve("classes")

        val (toolchain, executionPolicy) = strategyConfig

        toolchain.createBuildSession().use {
            val compilation = jvmNonIncrementalCompilationOperation(sources, destination)
            it.executeOperation(compilation, executionPolicy)
        }
    }

    @DisplayName("incremental compilation smoke forward compatibility test")
    @DefaultForwardCompatibilityCompilationTest
    fun testIncrementalCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val sourceA = workingDirectory.resolve("a.kt")
        val sourceB = workingDirectory.resolve("b.kt")

        val sources = listOf(
            sourceA.apply { writeText("fun a() = 42") },
            sourceB.apply { writeText("fun b() = a()") },
        )
        val destination = workingDirectory.resolve("app-classes")

        val (toolchain, executionPolicy) = strategyConfig

        toolchain.createBuildSession().use { session ->
            run {
                session.executeOperation(jvmIncrementalCompilationOperation(sources, destination), executionPolicy)
            }
            run {
                sourceA.writeText("fun a(x: Int = 0) = 42")
                val logger = TestKotlinLogger()
                session.executeOperation(
                    jvmIncrementalCompilationOperation(
                        sources, destination,
                        sourceChanges = SourcesChanges.Known(listOf(sourceA.toFile()), emptyList()),
                    ),
                    executionPolicy, logger,
                )
                logger.logMessagesByLevel.getValue(LogLevel.DEBUG).assertCompiledSources(setOf("../a.kt", "../b.kt"))
            }
            run {
                val logger = TestKotlinLogger()
                session.executeOperation(
                    jvmIncrementalCompilationOperation(
                        sources, destination,
                        sourceChanges = SourcesChanges.Known(emptyList(), emptyList()),
                    ),
                    executionPolicy, logger,
                )
                assertTrue(logger.logMessagesByLevel.getValue(LogLevel.DEBUG).none { "compile iteration" in it })
            }
        }
    }

    @DisplayName("incremental compilation with classpath snapshot forward compatibility test")
    @DefaultForwardCompatibilityCompilationTest
    fun testIncrementalCompilationWithClasspathSnapshot(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val app = workingDirectory.resolve("app").also { it.createDirectories() }
        val sourceA = app.resolve("a.kt")
        val lib = workingDirectory.resolve("lib").also { it.createDirectories() }
        val sourceLib = lib.resolve("lib.kt")
        val libDestination = workingDirectory.resolve("lib-classes").also { it.createDirectories() }
        val libSnapshotFile = workingDirectory.resolve("lib-snapshot.bin")
        val appDestination = workingDirectory.resolve("app-classes")

        val sourcesApp = listOf(sourceA.apply { writeText("fun a() = lib()") })
        val sourcesLib = listOf(sourceLib.apply { writeText("fun lib() = 42") })

        val (toolchain, executionPolicy) = strategyConfig

        toolchain.createBuildSession().use { session ->
            fun snapshotLib() {
                session.executeOperation(toolchain.jvm.classpathSnapshottingOperation(libDestination) {})
                    .saveSnapshot(libSnapshotFile)
            }

            run {
                session.executeOperation(jvmNonIncrementalCompilationOperation(sourcesLib, libDestination), executionPolicy)
                snapshotLib()
                session.executeOperation(
                    jvmIncrementalCompilationOperation(
                        sourcesApp, appDestination,
                        extraClasspath = listOf(libDestination),
                        dependenciesSnapshotFiles = listOf(libSnapshotFile),
                    ),
                    executionPolicy,
                )
            }
            run {
                sourceLib.writeText("fun lib(y: Int = 0) = 42")
                session.executeOperation(jvmNonIncrementalCompilationOperation(sourcesLib, libDestination), executionPolicy)
                snapshotLib()
                val logger = TestKotlinLogger()
                session.executeOperation(
                    jvmIncrementalCompilationOperation(
                        sourcesApp, appDestination,
                        extraClasspath = listOf(libDestination),
                        sourceChanges = SourcesChanges.Known(emptyList(), emptyList()),
                        dependenciesSnapshotFiles = listOf(libSnapshotFile),
                    ),
                    executionPolicy, logger,
                )
                logger.logMessagesByLevel.getValue(LogLevel.DEBUG).assertCompiledSources(setOf("../app/a.kt"))
            }
            run {
                val logger = TestKotlinLogger()
                snapshotLib()
                session.executeOperation(
                    jvmIncrementalCompilationOperation(
                        sourcesApp, appDestination,
                        extraClasspath = listOf(libDestination),
                        sourceChanges = SourcesChanges.Known(emptyList(), emptyList()),
                        dependenciesSnapshotFiles = listOf(libSnapshotFile),
                    ),
                    executionPolicy, logger,
                )
                assertTrue(logger.logMessagesByLevel.getValue(LogLevel.DEBUG).none { "compile iteration" in it })
            }
        }
    }

    private fun jvmIncrementalCompilationOperation(
        sources: List<Path>,
        destination: Path,
        extraClasspath: List<Path> = emptyList(),
        sourceChanges: SourcesChanges = SourcesChanges.Unknown,
        dependenciesSnapshotFiles: List<Path> = emptyList(),
    ): JvmCompilationOperation = jvmNonIncrementalCompilationOperation(sources, destination, extraClasspath) {
        this[JvmCompilationOperation.INCREMENTAL_COMPILATION] = snapshotBasedIcConfiguration(
            workingDirectory.resolve("ic-cache"),
            sourceChanges,
            dependenciesSnapshotFiles,
            workingDirectory.resolve("shrunk-classpath-snapshot.bin"),
        ) {
            this[JvmSnapshotBasedIncrementalCompilationConfiguration.ROOT_PROJECT_DIR] = workingDirectory
        }
    }

    private fun jvmNonIncrementalCompilationOperation(
        sources: List<Path>,
        destination: Path,
        extraClasspath: List<Path> = emptyList(),
        additionalConfiguration: JvmCompilationOperation.Builder.() -> Unit = {},
    ): JvmCompilationOperation = toolchain.jvm.jvmCompilationOperation(sources, destination) {
        val stdlibPath = KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath()
        compilerArguments[JvmCompilerArguments.MODULE_NAME] = "a"
        compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
        compilerArguments[NO_REFLECT] = true
        compilerArguments[NO_STDLIB] = true
        compilerArguments[CLASSPATH] = (listOf(stdlibPath) + extraClasspath)
            .joinToString(File.pathSeparator) { it.absolutePathString() }
        additionalConfiguration()
    }
}
