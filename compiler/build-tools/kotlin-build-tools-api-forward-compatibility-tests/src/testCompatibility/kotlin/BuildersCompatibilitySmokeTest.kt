/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.absolutePathString
import kotlin.io.path.toPath
import kotlin.io.path.writeText

class BuildersCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Test all APIs using legacy non-builders")
    @Test
    fun testAllApisUsingLegacyNonBuilders() {
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
}
