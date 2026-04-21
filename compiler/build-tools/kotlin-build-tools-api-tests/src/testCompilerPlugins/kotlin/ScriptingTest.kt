/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.reflect.KClass

class ScriptingTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application (non-incremental)")
    @TestMetadata("scripting-kts")
    fun smokeTestCompilerPluginsApplicationNonIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("scripting-kts", dependencies = listOf(dependencyOnThisClasspath))
            module.compile(compilationConfigAction = configureCompilerArgs(GreetScriptTemplate::class)) {
                assertOutputs("Test_greet.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application with custom extension (non-incremental)")
    @TestMetadata("scripting-custom-extension")
    fun smokeTestCompilerPluginsApplicationCustomExtensionNonIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("scripting-custom-extension", dependencies = listOf(dependencyOnThisClasspath))
            module.compile(compilationConfigAction = configureCompilerArgs(GreetScriptCustomExtensionTemplate::class, "greet")) {
                assertOutputs("Test.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application (incremental)")
    @TestMetadata("scripting-kts")
    fun smokeTestCompilerPluginsApplicationIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("scripting-kts", dependencies = listOf(dependencyOnThisClasspath))
            module.compileIncrementally(
                SourcesChanges.Unknown,
                compilationConfigAction = configureCompilerArgs(GreetScriptTemplate::class)
            ) {
                assertOutputs("Test_greet.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application with custom extension (incremental)")
    @TestMetadata("scripting-custom-extension")
    fun smokeTestCompilerPluginsApplicationCustomExtensionIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("scripting-custom-extension", dependencies = listOf(dependencyOnThisClasspath))
            module.compileIncrementally(
                SourcesChanges.Unknown,
                compilationConfigAction = configureCompilerArgs(GreetScriptCustomExtensionTemplate::class, "greet")
            ) {
                assertOutputs("Test.class")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Smoke test of custom script extension discovery")
    fun smokeTestCustomScriptExtensionDiscovery(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        dependencyOnThisClasspath.location.copyToRecursively(workingDirectory, followLinks = false, overwrite = false)
        workingDirectory.resolve("META-INF/kotlin/script/templates").apply {
            createDirectories()
            resolve(GreetScriptTemplate::class.qualifiedName!!).createFile()
        }
        val toolchain = strategyConfig.first
        val operation =
            toolchain.jvm.discoverScriptExtensionsOperationBuilder(listOf(workingDirectory)).build()

        val result = toolchain.createBuildSession().use { session ->
            session.executeOperation(
                operation, strategyConfig.second,
                TestKotlinLogger()
            )
        }

        assertEquals(listOf("greet.kts"), result)
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Script extension discovery with custom extension without .kts suffix")
    fun discoverCustomExtensionWithoutKtsSuffix(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        dependencyOnThisClasspath.location.copyToRecursively(workingDirectory, followLinks = false, overwrite = false)
        workingDirectory.resolve("META-INF/kotlin/script/templates").apply {
            createDirectories()
            resolve(GreetScriptCustomExtensionTemplate::class.qualifiedName!!).createFile()
        }

        val result = executeDiscovery(strategyConfig, listOf(workingDirectory))

        assertEquals(listOf("greet"), result)
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Script extension discovery with compound custom extension")
    fun discoverCompoundCustomExtension(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        dependencyOnThisClasspath.location.copyToRecursively(workingDirectory, followLinks = false, overwrite = false)
        workingDirectory.resolve("META-INF/kotlin/script/templates").apply {
            createDirectories()
            resolve(GreetScriptMyExtensionTemplate::class.qualifiedName!!).createFile()
        }

        val result = executeDiscovery(strategyConfig, listOf(workingDirectory))

        assertEquals(listOf("greet.my"), result)
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Script extension discovery finds multiple templates")
    fun discoverMultipleTemplates(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        dependencyOnThisClasspath.location.copyToRecursively(workingDirectory, followLinks = false, overwrite = false)
        workingDirectory.resolve("META-INF/kotlin/script/templates").apply {
            createDirectories()
            resolve(GreetScriptTemplate::class.qualifiedName!!).createFile()
            resolve(GreetScriptCustomExtensionTemplate::class.qualifiedName!!).createFile()
        }

        val result = executeDiscovery(strategyConfig, listOf(workingDirectory))

        assertEquals(setOf("greet.kts", "greet"), result.toSet())
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Script extension discovery with empty classpath")
    fun discoverWithEmptyClasspath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        workingDirectory.resolve("empty").createDirectories()

        val result = executeDiscovery(strategyConfig, listOf(workingDirectory.resolve("empty")))

        assertTrue(result.isEmpty(), "Expected empty list when no script definitions found")
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Script extension discovery with non-existing path")
    fun discoverWithNonExistingPath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        val nonExistingPath = workingDirectory.resolve("does-not-exist")

        val result = executeDiscovery(strategyConfig, listOf(nonExistingPath))

        assertTrue(result.isEmpty(), "Expected empty list for non-existing path")
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Script extension discovery with marker but without class")
    fun discoverWithMarkerButNoClass(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeInProcess(strategyConfig)
        workingDirectory.resolve("META-INF/kotlin/script/templates").apply {
            createDirectories()
            resolve("com.example.NonExistentScript").createFile()
        }

        val result = executeDiscovery(strategyConfig, listOf(workingDirectory))

        assertTrue(result.isEmpty(), "Expected empty list when class is missing")
    }

    // TODO: Remove this test and add daemon execution support when KT-84096 is implemented
    @BtaVersionsOnlyCompilationTest
    @DisplayName("Script extension discovery fails with daemon execution policy")
    fun discoverScriptExtensionsFailsWithDaemon(toolchain: KotlinToolchains) {
        val classpath = listOf(workingDirectory.resolve("greet-script-template"))
        val operation = toolchain.jvm.discoverScriptExtensionsOperationBuilder(classpath).build()
        val daemonPolicy = toolchain.daemonExecutionPolicyBuilder().build()

        val exception = assertThrows<IllegalStateException> {
            toolchain.createBuildSession().use { session ->
                session.executeOperation(operation, daemonPolicy, TestKotlinLogger())
            }
        }
        assertEquals("Only in-process execution policy is supported for this operation.", exception.message)
    }

    private fun assumeInProcess(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeTrue(
            strategyConfig.second is ExecutionPolicy.InProcess,
            "DiscoverScriptExtensionsOperation only supports in-process execution"
        )
    }

    private fun executeDiscovery(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        classpath: List<java.nio.file.Path>,
    ): Collection<String> {
        val toolchain = strategyConfig.first
        val operation = toolchain.jvm.discoverScriptExtensionsOperationBuilder(classpath).build()
        return toolchain.createBuildSession().use { session ->
            session.executeOperation(operation, strategyConfig.second, TestKotlinLogger())
        }
    }

    private val dependencyOnThisClasspath: Dependency
        get() = FileDependency(
            Paths.get(
                GreetScriptTemplate::class.java
                    .protectionDomain
                    .codeSource
                    .location
                    .toURI()
            )
        )

    private fun configureCompilerArgs(
        scriptingTemplate: KClass<*>,
        customScriptExtension: String? = null,
    ): (JvmCompilationOperation.Builder) -> Unit = {
        it.compilerArguments[COMPILER_PLUGINS] = listOf(scriptingPlugin(scriptingTemplate))
        if (customScriptExtension != null) {
            it[KOTLINSCRIPT_EXTENSIONS] = arrayOf(customScriptExtension)
        }
    }
}
