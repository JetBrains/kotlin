/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinStdlibLocation
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.walk
import kotlin.io.path.writeText

@DisplayName("Cross-module sealed when exhaustiveness")
class SealedWhenExhaustivenessTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding a sealed case in an upstream module invalidates an exhaustive when downstream")
    @TestMetadata("ic-scenarios/sealed-when-exhaustiveness/lib")
    fun testSealedCaseAddedInDependency(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val (toolchains, executionPolicy) = strategyConfig
        JvmProject(toolchains, executionPolicy, workingDirectory.resolve("direct-btapi")).use { project ->
            val model = project.directModule("model", "ic-scenarios/sealed-when-exhaustiveness/lib")
            val consumer = project.directModule(
                moduleName = "consumer",
                template = "ic-scenarios/sealed-when-exhaustiveness/app",
                classpath = listOf(model.classesDir),
            )

            model.compile(toolchains, executionPolicy).assertCompilationSuccess("Initial model build should succeed.")
            consumer.compile(toolchains, executionPolicy).assertCompilationSuccess("Initial consumer build should succeed.")

            model.sourcesDir.resolve("Foo.kt").writeText(fooWithOofSource)
            model.compile(toolchains, executionPolicy).assertCompilationSuccess("Model incremental rebuild should stay healthy.")

            val branchSwitchConsumer = consumer.compile(toolchains, executionPolicy)
            consumer.clean()
            val cleanConsumer = consumer.compile(toolchains, executionPolicy)

            cleanConsumer.assertCompilationFailure(noElseInWhenError, "Clean consumer rebuild should fail.")
            branchSwitchConsumer.assertCompilationFailure(
                noElseInWhenError,
                "Incremental consumer compilation should fail after adding Foo.Oof, matching the clean rebuild."
            )
        }
    }

    private companion object {
        private val noElseInWhenError =
            """.*'when' expression must be exhaustive\. Add the 'Oof' branch or an 'else' branch\..*""".toRegex()

        //language=kt
        private val fooWithOofSource =
            """
            sealed interface Foo {
                object Bar : Foo
                object Oof : Foo
            }
            """.trimIndent()
    }

    private data class DirectBtApiModule(
        val moduleName: String,
        val projectDir: Path,
        val sourcesDir: Path,
        val buildRoot: Path,
        val classpath: List<Path>,
    ) {
        val classesDir: Path = buildRoot.resolve("classes")
        val incrementalCachePath: Path = buildRoot.resolve("inc-state")

        fun clean() {
            buildRoot.deleteRecursively()
        }
    }

    private data class CompilationAttempt(
        val result: CompilationResult,
        val logger: TestKotlinLogger,
    ) {
        fun assertCompilationSuccess(message: String) {
            assertEquals(CompilationResult.COMPILATION_SUCCESS, result, "$message\n\n${describe()}")
        }

        fun assertCompilationFailure(regex: Regex, message: String) {
            assertEquals(CompilationResult.COMPILATION_ERROR, result, "$message\n\n${describe()}")
            val errorOutput = logger.logMessagesByLevel[LogLevel.ERROR].orEmpty().joinToString("\n")
            assertTrue(regex.containsMatchIn(errorOutput), describe())
        }

        fun describe(): String =
            buildString {
                appendLine("Compilation result: $result")
                for (level in LogLevel.entries) {
                    val messages = logger.logMessagesByLevel[level].orEmpty()
                    if (messages.isNotEmpty()) {
                        appendLine()
                        appendLine("$level:")
                        appendLine(messages.joinToString("\n"))
                    }
                }
            }
    }

    // Deliberately bypass JvmProject/JvmModule.compileIncrementally: that harness precomputes dependency
    // snapshots and does not reproduce this stale exhaustive-when case. We need the direct BT API path
    // with SourcesChanges.ToBeCalculated and an empty dependency snapshot list.
    private fun DirectBtApiModule.compile(
        toolchains: KotlinToolchains,
        executionPolicy: ExecutionPolicy,
    ): CompilationAttempt {
        buildRoot.createDirectories()
        incrementalCachePath.createDirectories()

        val sources = sourcesDir.walk()
            .filter { path ->
                val pathString = path.toString()
                pathString.endsWith(".kt") || pathString.endsWith(".kts") || pathString.endsWith(".java")
            }
            .toList()

        val operation = toolchains.jvm.jvmCompilationOperation(sources, classesDir) {
            compilerArguments[NO_REFLECT] = true
            compilerArguments[NO_STDLIB] = true
            compilerArguments[MODULE_NAME] = moduleName
            compilerArguments[CLASSPATH] = listOf(currentKotlinStdlibLocation) + classpath

            @Suppress("DEPRECATION")
            val incrementalConfiguration = snapshotBasedIcConfigurationBuilder(
                incrementalCachePath,
                SourcesChanges.ToBeCalculated,
                emptyList(),
                incrementalCachePath.resolve("shrunk-classpath-snapshot.bin"),
            ).apply {
                this[ROOT_PROJECT_DIR] = projectDir
                this[MODULE_BUILD_DIR] = buildRoot
                this[PRECISE_JAVA_TRACKING] = true
            }.build()

            this[JvmCompilationOperation.INCREMENTAL_COMPILATION] = incrementalConfiguration
        }

        val logger = TestKotlinLogger()
        val buildSession = toolchains.createBuildSession()
        val result = try {
            buildSession.executeOperation(operation, executionPolicy, logger)
        } finally {
            buildSession.close()
        }

        return CompilationAttempt(result, logger)
    }

    private fun JvmProject.directModule(
        moduleName: String,
        template: String,
        classpath: List<Path> = emptyList(),
    ): DirectBtApiModule {
        val sourcesDir = module(template).sourcesDirectory
        return DirectBtApiModule(
            moduleName = moduleName,
            projectDir = projectDirectory,
            sourcesDir = sourcesDir,
            buildRoot = projectDirectory.resolve("${moduleName}-dest"),
            classpath = classpath,
        )
    }
}
