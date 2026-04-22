/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.Companion.COMPILER_ARGUMENTS_LOG_LEVEL
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.Companion.COMPILER_MESSAGE_RENDERER
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.Companion.GENERATE_COMPILER_REF_INDEX
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.Companion.LOOKUP_TRACKER
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.jsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.jvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Method
import kotlin.io.path.Path

class AvailableSinceTest : BaseCompilationTest() {
    val versions = listOf(
        "2.2.0",
        "2.2.20",
        "2.3.0",
        "2.3.20",
        "2.4.0",
        "2.4.20",
    ) + KotlinToolchains.loadImplementation(btaClassloader).getCompilerVersion()

    val overrideVersionMethod: Method by lazy {
        btaClassloader.loadClass("org.jetbrains.kotlin.buildtools.internal.OptionsKt")
            .getDeclaredMethod("overrideVersionForOptionsCheck", String::class.java)
    }

    @Test
    fun testBaseAndJvmCompilationOperation() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())
        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.jvm.jvmCompilationOperation(emptyList(), Path("")) {
                    trySet(LOOKUP_TRACKER.availableSinceVersion) {
                        this[LOOKUP_TRACKER] = object : CompilerLookupTracker {
                            override fun recordLookup(
                                filePath: String,
                                scopeFqName: String,
                                scopeKind: CompilerLookupTracker.ScopeKind,
                                name: String,
                            ) {
                            }

                            override fun clear() {}
                        }
                    }
                    trySet(COMPILER_ARGUMENTS_LOG_LEVEL.availableSinceVersion) {
                        this[COMPILER_ARGUMENTS_LOG_LEVEL] = JvmCompilationOperation.CompilerArgumentsLogLevel.DEBUG
                    }
                    trySet(GENERATE_COMPILER_REF_INDEX.availableSinceVersion) {
                        this[GENERATE_COMPILER_REF_INDEX] = false
                    }
                    trySet(COMPILER_MESSAGE_RENDERER.availableSinceVersion) {
                        this[COMPILER_MESSAGE_RENDERER] = object : CompilerMessageRenderer {
                            override fun render(
                                severity: CompilerMessageRenderer.Severity,
                                message: String,
                                location: CompilerMessageRenderer.SourceLocation?,
                            ): String {
                                TODO("Not yet implemented")
                            }
                        }
                    }
                    trySet(INCREMENTAL_COMPILATION.availableSinceVersion) {
                        this[INCREMENTAL_COMPILATION] = object : JvmIncrementalCompilationConfiguration {}
                    }
                    trySet(KOTLINSCRIPT_EXTENSIONS.availableSinceVersion) { this[KOTLINSCRIPT_EXTENSIONS] = arrayOf() }
                    @Suppress("DEPRECATION")
                    trySet(JvmCompilationOperation.LOOKUP_TRACKER.availableSinceVersion) {
                        @Suppress("DEPRECATION")
                        this[JvmCompilationOperation.LOOKUP_TRACKER] = object : CompilerLookupTracker {
                            override fun recordLookup(
                                filePath: String,
                                scopeFqName: String,
                                scopeKind: CompilerLookupTracker.ScopeKind,
                                name: String,
                            ) {
                            }

                            override fun clear() {}
                        }
                    }
                    @Suppress("DEPRECATION")
                    trySet(JvmCompilationOperation.COMPILER_ARGUMENTS_LOG_LEVEL.availableSinceVersion) {
                        @Suppress("DEPRECATION")
                        this[JvmCompilationOperation.COMPILER_ARGUMENTS_LOG_LEVEL] = JvmCompilationOperation.CompilerArgumentsLogLevel.DEBUG
                    }
                    @Suppress("DEPRECATION")
                    trySet(JvmCompilationOperation.GENERATE_COMPILER_REF_INDEX.availableSinceVersion) {
                        @Suppress("DEPRECATION")
                        this[JvmCompilationOperation.GENERATE_COMPILER_REF_INDEX] = false
                    }
                }
            }
        }
    }

    @Test
    fun testBuildOperation() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.jvm.jvmCompilationOperationBuilder(emptyList(), Path("")).apply {
                    trySet(BuildOperation.METRICS_COLLECTOR.availableSinceVersion) {
                        this[BuildOperation.METRICS_COLLECTOR] = null as BuildMetricsCollector?
                    }
                }
            }
        }
    }

    @Test
    fun testBaseIncrementalCompilationConfiguration() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.jvm.jvmCompilationOperationBuilder(emptyList(), Path(""))
                    .snapshotBasedIcConfigurationBuilder(Path(""), SourcesChanges.Unknown, emptyList()).apply {
                        trySet(BaseIncrementalCompilationConfiguration.ROOT_PROJECT_DIR.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.ROOT_PROJECT_DIR] = null
                        }
                        trySet(BaseIncrementalCompilationConfiguration.MODULE_BUILD_DIR.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.MODULE_BUILD_DIR] = null
                        }
                        trySet(BaseIncrementalCompilationConfiguration.BACKUP_CLASSES.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.BACKUP_CLASSES] = false
                        }
                        trySet(BaseIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY] = false
                        }
                        trySet(BaseIncrementalCompilationConfiguration.FORCE_RECOMPILATION.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.FORCE_RECOMPILATION] = false
                        }
                        trySet(BaseIncrementalCompilationConfiguration.OUTPUT_DIRS.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.OUTPUT_DIRS] = null
                        }
                        @OptIn(ExperimentalCompilerArgument::class)
                        trySet(BaseIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM.availableSinceVersion) {
                            @OptIn(ExperimentalCompilerArgument::class)
                            this[BaseIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = false
                        }
                        @OptIn(ExperimentalCompilerArgument::class)
                        trySet(BaseIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION.availableSinceVersion) {
                            @OptIn(ExperimentalCompilerArgument::class)
                            this[BaseIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = true
                        }
                        trySet(BaseIncrementalCompilationConfiguration.TRACK_CONFIGURATION_INPUTS.availableSinceVersion) {
                            this[BaseIncrementalCompilationConfiguration.TRACK_CONFIGURATION_INPUTS] = false
                        }
                    }
            }
        }
    }

    @Test
    fun testJvmSnapshotBasedIncrementalCompilationConfiguration() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.jvm.jvmCompilationOperationBuilder(emptyList(), Path(""))
                    .snapshotBasedIcConfigurationBuilder(Path(""), SourcesChanges.Unknown, emptyList()).apply {
                        trySet(JvmSnapshotBasedIncrementalCompilationConfiguration.PRECISE_JAVA_TRACKING.availableSinceVersion) {
                            this[JvmSnapshotBasedIncrementalCompilationConfiguration.PRECISE_JAVA_TRACKING] = false
                        }
                        trySet(JvmSnapshotBasedIncrementalCompilationConfiguration.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES.availableSinceVersion) {
                            this[JvmSnapshotBasedIncrementalCompilationConfiguration.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = false
                        }
                        @OptIn(ExperimentalCompilerArgument::class)
                        trySet(JvmSnapshotBasedIncrementalCompilationConfiguration.USE_FIR_RUNNER.availableSinceVersion) {
                            @OptIn(ExperimentalCompilerArgument::class)
                            this[JvmSnapshotBasedIncrementalCompilationConfiguration.USE_FIR_RUNNER] = false
                        }
                    }
            }
        }
    }

    @Test
    fun testJvmClasspathSnapshottingOperation() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.jvm.classpathSnapshottingOperationBuilder(Path("")).apply {
                    trySet(JvmClasspathSnapshottingOperation.GRANULARITY.availableSinceVersion) {
                        this[JvmClasspathSnapshottingOperation.GRANULARITY] = ClassSnapshotGranularity.CLASS_LEVEL
                    }
                    trySet(JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES.availableSinceVersion) {
                        this[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES] = false
                    }
                }
            }
        }
    }

    @Test
    fun testDiscoverScriptExtensionsOperation() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.jvm.discoverScriptExtensionsOperationBuilder(emptyList()).apply {
                    trySet(DiscoverScriptExtensionsOperation.COMPILER_MESSAGE_RENDERER.availableSinceVersion) {
                        this[DiscoverScriptExtensionsOperation.COMPILER_MESSAGE_RENDERER] = object : CompilerMessageRenderer {
                            override fun render(
                                severity: CompilerMessageRenderer.Severity,
                                message: String,
                                location: CompilerMessageRenderer.SourceLocation?,
                            ): String {
                                TODO("Not yet implemented")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testJsKlibCompilationOperation() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.js.jsKlibCompilationOperation(emptyList(), Path("")) {
                    trySet(JsKlibCompilationOperation.INCREMENTAL_COMPILATION.availableSinceVersion) {
                        this[JsKlibCompilationOperation.INCREMENTAL_COMPILATION] = null
                    }
                }
            }
        }
    }

    @Test
    fun testJsHistoryBasedIncrementalCompilationConfiguration() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.js.jsKlibCompilationOperationBuilder(emptyList(), Path(""))
                    .historyBasedIcConfigurationBuilder(Path(""), Path(""), SourcesChanges.Unknown, emptyList()).apply {
                        trySet(JsHistoryBasedIncrementalCompilationConfiguration.ROOT_PROJECT_BUILD_DIR.availableSinceVersion) {
                            this[JsHistoryBasedIncrementalCompilationConfiguration.ROOT_PROJECT_BUILD_DIR] = null
                        }
                        trySet(JsHistoryBasedIncrementalCompilationConfiguration.HISTORY_FILE_DIR.availableSinceVersion) {
                            this[JsHistoryBasedIncrementalCompilationConfiguration.HISTORY_FILE_DIR] = null
                        }
                    }
            }
        }
    }

    @Test
    fun testWithDaemon() {
        val toolchains = KotlinToolchains.loadImplementation(btaClassloader)
        assumeTrue(toolchains.hasOptionVersionChecking())

        versions.forEach { version ->
            overrideVersionMethod.invoke(null, version)
            with(KotlinToolingVersion(version)) {
                toolchains.daemonExecutionPolicyBuilder().apply {
                    trySet(ExecutionPolicy.WithDaemon.JVM_ARGUMENTS.availableSinceVersion) {
                        this[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = null
                    }
                    trySet(ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS.availableSinceVersion) {
                        this[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = null
                    }
                    @OptIn(DelicateBuildToolsApi::class)
                    trySet(ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH.availableSinceVersion) {
                        @OptIn(DelicateBuildToolsApi::class)
                        this[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH] = Path("")
                    }
                    trySet(ExecutionPolicy.WithDaemon.LOGS_PATH.availableSinceVersion) {
                        this[ExecutionPolicy.WithDaemon.LOGS_PATH] = Path("")
                    }
                    trySet(ExecutionPolicy.WithDaemon.LOGS_FILE_SIZE_LIMIT.availableSinceVersion) {
                        this[ExecutionPolicy.WithDaemon.LOGS_FILE_SIZE_LIMIT] = null
                    }
                    trySet(ExecutionPolicy.WithDaemon.LOGS_FILE_COUNT_LIMIT.availableSinceVersion) {
                        this[ExecutionPolicy.WithDaemon.LOGS_FILE_COUNT_LIMIT] = null
                    }
                }
            }
        }
    }

    private fun KotlinToolchains.hasOptionVersionChecking(): Boolean =
        KotlinToolingVersion(getCompilerVersion()) >= KotlinToolingVersion(2, 4, 20, "snapshot")

    context(currentVersion: KotlinToolingVersion)
    fun trySet(availableSince: KotlinReleaseVersion, action: () -> Unit) {
        val shouldBeAllowedToSet = currentVersion >= availableSince.toKotlinToolingVersion()
        if (shouldBeAllowedToSet) {
            action()
        } else {
            val exception = assertThrows<IllegalStateException>(action)
            assertTrue(exception.message?.contains("is available only since $availableSince") == true) { "Exception message does not contain ' is available only since $availableSince'" }
        }
    }
}

private fun KotlinReleaseVersion.toKotlinToolingVersion() = KotlinToolingVersion(this.major, this.minor, this.patch, "snapshot")
