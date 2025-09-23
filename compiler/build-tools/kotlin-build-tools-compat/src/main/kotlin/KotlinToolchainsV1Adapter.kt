/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.internal.compat

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.internal.compat.JvmCompilationOperationV1Adapter.JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.internal.compat.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.incremental.isJavaFile
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.toJavaDuration

public class KotlinToolchainsV1Adapter(
    private val compilationService: CompilationService,
) : KotlinToolchains {
    private val jvm: JvmPlatformToolchain by lazy {
        object : JvmPlatformToolchain {
            override fun createJvmCompilationOperation(
                sources: List<Path>,
                destinationDirectory: Path,
            ): JvmCompilationOperation {
                return JvmCompilationOperationV1Adapter(compilationService, sources, destinationDirectory, JvmCompilerArgumentsImpl())
            }

            override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
                return JvmClasspathSnapshottingOperationV1Adapter(compilationService, classpathEntry)
            }
        }
    }

    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            JvmPlatformToolchain::class.java -> jvm
            else -> error("Unsupported platform toolchain type: $type. Only JVM compilation is supported in BTA API v1 fallback (compiler version ${getCompilerVersion()}.")
        } as T
    }

    override fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess {
        return ExecutionPolicyV1Adapter.InProcess(compilationService.makeCompilerExecutionStrategyConfiguration().useInProcessStrategy())
    }

    override fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon {
        return ExecutionPolicyV1Adapter.WithDaemon(compilationService)
    }

    override fun getCompilerVersion(): String {
        return compilationService.getCompilerVersion()
    }

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionV1Adapter(this, RandomProjectUUID(), compilationService)
    }
}

private class JvmClasspathSnapshottingOperationV1Adapter(val compilationService: CompilationService, val classpathEntry: Path) :
    BuildOperationImpl<ClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation {
    private val options: Options = Options(JvmClasspathSnapshottingOperation::class)

    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = options[key]


    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicyV1Adapter,
        logger: KotlinLogger?,
    ): ClasspathEntrySnapshot = compilationService.calculateClasspathSnapshot(
        classpathEntry.toFile(), this[GRANULARITY], this[PARSE_INLINED_LOCAL_CLASSES]
    )

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        @JvmField
        val GRANULARITY: Option<ClassSnapshotGranularity> = Option("GRANULARITY", ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)

        @JvmField
        val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = Option("PARSE_INLINED_LOCAL_CLASSES", true)
    }
}

private class JvmCompilationOperationV1Adapter(
    val compilationService: CompilationService,
    val kotlinSources: List<Path>,
    val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl,
) : BuildOperationImpl<CompilationResult>(), JvmCompilationOperation {
    private val options: Options = Options(JvmCompilationOperation::class)

    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = options[key]

    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION", null)

        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)

        val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS", null)
    }

    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicyV1Adapter,
        logger: KotlinLogger?,
    ): CompilationResult {
        val config = compilationService.makeJvmCompilationConfiguration()
        logger?.let { config.useLogger(it) }
        this[INCREMENTAL_COMPILATION]?.let { icConfig ->
            if (icConfig !is JvmSnapshotBasedIncrementalCompilationConfiguration) return@let
            val options = icConfig.options as JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter
            val snapshotBasedConfigV1 = config.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
                .apply {
                    options[ROOT_PROJECT_DIR]?.let { setRootProjectDir(it.toFile()) }
                    options[MODULE_BUILD_DIR]?.let { setBuildDir(it.toFile()) }
                }
                .usePreciseJavaTracking(options[PRECISE_JAVA_TRACKING])
                .usePreciseCompilationResultsBackup(options[BACKUP_CLASSES])
                .keepIncrementalCompilationCachesInMemory(options[KEEP_IC_CACHES_IN_MEMORY])
                .useOutputDirs(
                    options[OUTPUT_DIRS]?.map(Path::toFile) ?: listOf(
                        destinationDirectory.toFile(),
                        icConfig.workingDirectory.toFile()
                    )
                )
                .forceNonIncrementalMode(options[FORCE_RECOMPILATION])
                .useFirRunner(options[USE_FIR_RUNNER])
            config.useIncrementalCompilation(
                icConfig.workingDirectory.toFile(),
                icConfig.sourcesChanges,
                ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
                    icConfig.dependenciesSnapshotFiles.map(Path::toFile),
                    icConfig.shrunkClasspathSnapshot.toFile()
                ),
                snapshotBasedConfigV1
            )
        }

        return compilationService.compileJvm(
            projectId,
            executionPolicy.strategyConfiguration,
            config,
            kotlinSources.map { it.toFile() },
            compilerArguments.toArgumentStrings().fixForFirCheck() + listOf(
                "-d",
                destinationDirectory.absolutePathString()
            ) + kotlinSources.filter { it.toFile().isJavaFile() }.map { it.absolutePathString() }
        )
    }

    private class JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter : JvmSnapshotBasedIncrementalCompilationOptions {
        private val options: Options = Options(JvmSnapshotBasedIncrementalCompilationOptions::class)

        operator fun <V> get(key: Option<V>): V = options[key]

        private operator fun <V> set(key: Option<V>, value: V) {
            options[key] = value
        }

        override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = options[key]

        override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
            options[key] = value
        }

        class Option<V> : BaseOptionWithDefault<V> {
            constructor(id: String) : super(id)
            constructor(id: String, default: V) : super(id, default = default)
        }

        companion object {
            val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", null)

            val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", null)

            val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING", false)

            val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", false)

            val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", false)

            val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", false)

            val RECOMPILATION_CLEANUP_DIRS: Option<Path> = Option("REBUILD_CLEANUP_DIRS")

            val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", null)

            val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
                Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)

            val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER", false)
        }
    }

    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter()
    }
}

// fir check in older BTAs expects "-language-version=X" (with "=") syntax and -Xuse-fir-ic (without "=true")
internal fun List<String>.fixForFirCheck(): List<String> {
    var skipNext = false
    return asSequence().windowed(2, partialWindows = true).flatMap { window ->
        if (window[0] == "-language-version") {
            skipNext = true
            listOf("${window[0]}=${window[1]}")
        } else if (!skipNext) {
            listOf(window[0])
        } else {
            skipNext = false
            emptyList()
        }
    }.map {
        if (it == "-Xuse-fir-ic=true") {
            "-Xuse-fir-ic"
        } else {
            it
        }
    }.toList()
}

private interface ExecutionPolicyV1Adapter {
    val strategyConfiguration: CompilerExecutionStrategyConfiguration

    class InProcess(override val strategyConfiguration: CompilerExecutionStrategyConfiguration) : ExecutionPolicyV1Adapter,
        ExecutionPolicy.InProcess

    class WithDaemon(private val compilationService: CompilationService) : ExecutionPolicyV1Adapter,
        ExecutionPolicy.WithDaemon {

        override val strategyConfiguration: CompilerExecutionStrategyConfiguration
            get() {
                val jvmArguments = get(JVM_ARGUMENTS) ?: emptyList()
                return get(SHUTDOWN_DELAY)?.let {
                    compilationService.makeCompilerExecutionStrategyConfiguration().useDaemonStrategy(
                        jvmArguments, it.toJavaDuration()
                    )
                } ?: compilationService.makeCompilerExecutionStrategyConfiguration().useDaemonStrategy(
                    jvmArguments
                )
            }

        private val options: Options = Options(ExecutionPolicy.WithDaemon::class)

        override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V = options[key.id]

        override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
            options[key] = value
        }

        operator fun <V> get(key: Option<V>): V = options[key]

        operator fun <V> set(key: Option<V>, value: V) {
            options[key] = value
        }

        class Option<V> : BaseOptionWithDefault<V> {
            constructor(id: String) : super(id)
            constructor(id: String, default: V) : super(id, default = default)
        }

        companion object {
            /**
             * A list of JVM arguments to pass to the Kotlin daemon.
             */
            val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS", default = null)

            /**
             * The time that the daemon process continues to live after all clients have disconnected.
             */
            val SHUTDOWN_DELAY: Option<Duration?> = Option("SHUTDOWN_DELAY", null)
        }
    }
}

private class BuildSessionV1Adapter(
    override val kotlinToolchains: KotlinToolchains,
    override val projectId: ProjectId,
    private val compilationService: CompilationService,
) : KotlinToolchains.BuildSession {
    override fun <R> executeOperation(operation: BuildOperation<R>): R {
        return executeOperation(operation, logger = null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R> executeOperation(
        operation: BuildOperation<R>,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): R {
        require(executionPolicy is ExecutionPolicyV1Adapter) { "Unsupported execution mode. Execution mode must be obtained from `createInProcessExecutionPolicy` or `createDaemonExecutionPolicy`." }
        return when (operation) {
            is JvmCompilationOperationV1Adapter, is JvmClasspathSnapshottingOperationV1Adapter -> {
                operation.execute(projectId, executionPolicy, logger) as R
            }
            else -> {
                error("Unsupported operation type with BTA API v1 fallback (compiler version ${kotlinToolchains.getCompilerVersion()}: ${operation::class.simpleName}")
            }
        }
    }

    override fun close() {
        compilationService.finishProjectCompilation(projectId)
    }
}

public fun CompilationService.asKotlinToolchains(): KotlinToolchains = KotlinToolchainsV1Adapter(this)

private abstract class BuildOperationImpl<R> : BuildOperation<R> {
    private val options: Options = Options(BuildOperation::class)

    override fun <V> get(key: BuildOperation.Option<V>): V = options[key.id]

    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        options[key] = value
    }

    abstract fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicyV1Adapter, logger: KotlinLogger? = null): R

    operator fun <V> get(key: Option<V>): V = options[key]

    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR", default = null)
    }
}
