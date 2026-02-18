/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.toPath
import kotlin.io.path.writeText

class BuildersCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Test builders produce independent operations")
    @DefaultStrategyAgnosticCompilationTest
    fun buildersProduceIndependentInstances(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val sources = listOf("a.kt", "b.kt", "c.kt").map { Paths.get(it) }
        val destination = Paths.get("classes")
        val operationBuilder = toolchain.jvm.jvmCompilationOperationBuilder(sources, destination).apply {
            compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM1_6
            compilerArguments[JvmCompilerArguments.MODULE_NAME] = "abc"
        }

        val operation1 = operationBuilder.build()
        val operation2 = operationBuilder.build()
        assertNotEquals(operation1, operation2)

        operationBuilder.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
        val operation3 = operationBuilder.build()
        assertEquals(JvmTarget.JVM1_6, operation1.compilerArguments[JvmCompilerArguments.JVM_TARGET])
        assertEquals(JvmTarget.JVM_17, operation3.compilerArguments[JvmCompilerArguments.JVM_TARGET])
    }

    @DisplayName("Test all APIs using legacy non-builders")
    @DefaultStrategyAgnosticCompilationTest
    fun testAllApisUsingLegacyNonBuilders(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val sources = listOf(workingDirectory.resolve("a.kt").also { it.writeText("class A") })
        val destination = workingDirectory.resolve("classes")

        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(sources, destination)
        jvmOperation.compilerArguments[JvmCompilerArguments.MODULE_NAME] = "a"
        jvmOperation.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
        jvmOperation.compilerArguments[NO_REFLECT] = true
        jvmOperation.compilerArguments[NO_STDLIB] = true
        jvmOperation.compilerArguments[CLASSPATH] =
            KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath().absolutePathString()
        val icOptions = jvmOperation.createSnapshotBasedIcOptions()
        icOptions[JvmSnapshotBasedIncrementalCompilationOptions.ROOT_PROJECT_DIR] = workingDirectory
        icOptions[JvmSnapshotBasedIncrementalCompilationOptions.FORCE_RECOMPILATION] = true
        val icConfig = JvmSnapshotBasedIncrementalCompilationConfiguration(
            workingDirectory.resolve("ic-cache"),
            SourcesChanges.Unknown,
            emptyList(),
            workingDirectory.resolve("shrunk-classpath-snapshot.bin"),
            icOptions
        )

        jvmOperation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = icConfig
        val executionMode = toolchain.createDaemonExecutionPolicy()
        executionMode[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 1000L

        val snapshotOperation = toolchain.jvm.createClasspathSnapshottingOperation(
            KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath()
        )
        snapshotOperation[JvmClasspathSnapshottingOperation.GRANULARITY] = ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
        snapshotOperation[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES] = false
        snapshotOperation[BuildOperation.METRICS_COLLECTOR] = null

        toolchain.createBuildSession().use {
            it.executeOperation(jvmOperation, executionMode)
            it.executeOperation(snapshotOperation)
        }
    }

    @DisplayName("Modifying WithDaemon builder after build does not affect the built policy")
    @BtaVersionsOnlyCompilationTest
    fun testWithDaemonBuilderImmutability(toolchain: KotlinToolchains) {
        val builder = toolchain.daemonExecutionPolicyBuilder().apply {
            this[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 5000L
        }
        val policy1 = builder.build()

        builder[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 10000L
        val policy2 = builder.build()

        assertEquals(5000L, policy1[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS])
        assertEquals(10000L, policy2[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS])
    }

    @DisplayName("Modifying IC configuration builder after build does not affect the built configuration")
    @DefaultStrategyAgnosticCompilationTest
    fun testICConfigBuilderImmutability(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val sources = listOf(workingDirectory.resolve("a.kt").also { it.writeText("class A") })
        val destination = workingDirectory.resolve("classes")
        val operationBuilder = toolchain.jvm.jvmCompilationOperationBuilder(sources, destination)
        val icBuilder = operationBuilder.snapshotBasedIcConfigurationBuilder(
            workingDirectory = workingDirectory.resolve("ic-work"),
            sourcesChanges = SourcesChanges.Unknown,
            dependenciesSnapshotFiles = emptyList(),
        ).apply {
            this[JvmSnapshotBasedIncrementalCompilationConfiguration.FORCE_RECOMPILATION] = false
            this[JvmSnapshotBasedIncrementalCompilationConfiguration.BACKUP_CLASSES] = true
        }
        val icConfig1 = icBuilder.build()

        icBuilder[JvmSnapshotBasedIncrementalCompilationConfiguration.FORCE_RECOMPILATION] = true
        icBuilder[JvmSnapshotBasedIncrementalCompilationConfiguration.BACKUP_CLASSES] = false
        val icConfig2 = icBuilder.build()

        assertEquals(false, icConfig1[JvmSnapshotBasedIncrementalCompilationConfiguration.FORCE_RECOMPILATION])
        assertEquals(true, icConfig1[JvmSnapshotBasedIncrementalCompilationConfiguration.BACKUP_CLASSES])
        assertEquals(true, icConfig2[JvmSnapshotBasedIncrementalCompilationConfiguration.FORCE_RECOMPILATION])
        assertEquals(false, icConfig2[JvmSnapshotBasedIncrementalCompilationConfiguration.BACKUP_CLASSES])
    }

    @DisplayName("toBuilder round-trip: modifying builder from toBuilder does not affect the original operation")
    @DefaultStrategyAgnosticCompilationTest
    fun testToBuilderOperationImmutability(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val sources = listOf(workingDirectory.resolve("a.kt").also { it.writeText("class A") })
        val destination = workingDirectory.resolve("classes")
        val original = toolchain.jvm.jvmCompilationOperationBuilder(sources, destination).apply {
            compilerArguments[JvmCompilerArguments.MODULE_NAME] = "original"
            compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
        }.build()

        val newBuilder = original.toBuilder()
        newBuilder.compilerArguments[JvmCompilerArguments.MODULE_NAME] = "modified"
        newBuilder.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_21
        val modified = newBuilder.build()

        assertEquals("original", original.compilerArguments[JvmCompilerArguments.MODULE_NAME])
        assertEquals(JvmTarget.JVM_17, original.compilerArguments[JvmCompilerArguments.JVM_TARGET])
        assertEquals("modified", modified.compilerArguments[JvmCompilerArguments.MODULE_NAME])
        assertEquals(JvmTarget.JVM_21, modified.compilerArguments[JvmCompilerArguments.JVM_TARGET])
    }

    @DisplayName("Modifying ClasspathSnapshottingOperation builder after build does not affect the built operation")
    @DefaultStrategyAgnosticCompilationTest
    fun testClasspathSnapshottingBuilderImmutability(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val classpathEntry = KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath()
        val builder = toolchain.jvm.classpathSnapshottingOperationBuilder(classpathEntry).apply {
            this[JvmClasspathSnapshottingOperation.GRANULARITY] = ClassSnapshotGranularity.CLASS_LEVEL
            this[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES] = true
        }
        val operation1 = builder.build()

        builder[JvmClasspathSnapshottingOperation.GRANULARITY] = ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
        builder[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES] = false
        val operation2 = builder.build()

        assertEquals(ClassSnapshotGranularity.CLASS_LEVEL, operation1[JvmClasspathSnapshottingOperation.GRANULARITY])
        assertEquals(true, operation1[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES])
        assertEquals(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, operation2[JvmClasspathSnapshottingOperation.GRANULARITY])
        assertEquals(false, operation2[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES])
    }
}
