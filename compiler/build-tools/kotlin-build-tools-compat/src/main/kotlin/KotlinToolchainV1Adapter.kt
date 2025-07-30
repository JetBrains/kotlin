/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.internal.compat

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy.WithDaemon.Companion.JVM_ARGUMENTS
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy.WithDaemon.Companion.SHUTDOWN_DELAY
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.GRANULARITY
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.knative.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.compat.arguments.JvmCompilerArgumentsImpl
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.toJavaDuration

public class KotlinToolchainV1Adapter(
    private val compilationService: CompilationService,
) : KotlinToolchain {
    override val jvm: JvmPlatformToolchain = object : JvmPlatformToolchain {
        override fun createJvmCompilationOperation(
            sources: List<Path>,
            destinationDirectory: Path,
        ): JvmCompilationOperation {
            return JvmCompilationOperationV1Adapter(sources, destinationDirectory, JvmCompilerArgumentsImpl())
        }

        override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
            return JvmClasspathSnapshottingOperationV1Adapter(classpathEntry)
        }
    }

    override val js: JsPlatformToolchain get() = error("JS compilation is not available in BTA API v1 fallback (compiler version ${getCompilerVersion()}")
    override val native: NativePlatformToolchain get() = error("Native compilation is not available in BTA API v1 fallback (compiler version ${getCompilerVersion()}")
    override val wasm: WasmPlatformToolchain get() = error("WASM compilation is not available in BTA API v1 fallback (compiler version ${getCompilerVersion()}")

    override fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess {
        return ExecutionPolicyV1Adapter.InProcess(compilationService.makeCompilerExecutionStrategyConfiguration().useInProcessStrategy())
    }

    override fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon {
        return ExecutionPolicyV1Adapter.WithDaemon(compilationService)
    }

    override fun getCompilerVersion(): String {
        return compilationService.getCompilerVersion()
    }

    override fun createBuild(projectId: ProjectId?): KotlinToolchain.Build {
        return BuildV1Adapter(this, projectId ?: RandomProjectUUID(), compilationService)
    }
}

private class JvmClasspathSnapshottingOperationV1Adapter(val classpathEntry: Path) : JvmClasspathSnapshottingOperation {
    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V {
        return optionsMap[key.id] as V
    }

    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        optionsMap[key.id] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V> get(key: BuildOperation.Option<V>): V {
        return optionsMap[key.id] as V
    }

    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        optionsMap[key.id] = value
    }
}

private class JvmCompilationOperationV1Adapter(
    val kotlinSources: List<Path>,
    val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl,
) : JvmCompilationOperation {
    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun <V> get(key: JvmCompilationOperation.Option<V>): V {
        return optionsMap[key.id] as V
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V> get(key: BuildOperation.Option<V>): V {
        return optionsMap[key.id] as V
    }

    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        optionsMap[key.id] = value
    }

    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        optionsMap[key.id] = value
    }

    private class JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter : JvmSnapshotBasedIncrementalCompilationOptions {
        private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

        init {
            this[PRECISE_JAVA_TRACKING] = true
            this[BACKUP_CLASSES] = false
            this[KEEP_IC_CACHES_IN_MEMORY] = false
            this[FORCE_RECOMPILATION] = false
            this[OUTPUT_DIRS] = null
            this[USE_FIR_RUNNER] = false
            this[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = false
        }


        @Suppress("UNCHECKED_CAST")
        override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V {
            return optionsMap[key.id] as V
        }

        override fun <V> set(
            key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>,
            value: V,
        ) {
            optionsMap[key.id] = value
        }
    }

    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter()
    }
}

public interface ExecutionPolicyV1Adapter {
    public val strategyConfiguration: CompilerExecutionStrategyConfiguration

    public class InProcess(override val strategyConfiguration: CompilerExecutionStrategyConfiguration) : ExecutionPolicyV1Adapter,
        ExecutionPolicy.InProcess

    public class WithDaemon(private val compilationService: CompilationService) : ExecutionPolicyV1Adapter,
        ExecutionPolicy.WithDaemon {

        private val optionsMap: MutableMap<ExecutionPolicy.WithDaemon.Option<*>, Any?> = mutableMapOf()

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

        @Suppress("UNCHECKED_CAST")
        override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V {
            return optionsMap[key] as V
        }

        override fun <V> set(
            key: ExecutionPolicy.WithDaemon.Option<V>,
            value: V,
        ) {
            optionsMap[key] = value
        }
    }
}

private class BuildV1Adapter(
    override val kotlinToolchain: KotlinToolchain,
    override val projectId: ProjectId,
    private val compilationService: CompilationService,
) : KotlinToolchain.Build {
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
            is JvmCompilationOperationV1Adapter -> {
                val configV1 = compilationService.makeJvmCompilationConfiguration()
                logger?.let { configV1.useLogger(it) }
                operation[JvmCompilationOperation.INCREMENTAL_COMPILATION]?.let { icConfig ->
                    if (icConfig !is JvmSnapshotBasedIncrementalCompilationConfiguration) return@let
                    val snapshotBasedConfigV1 = configV1.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
                        .setRootProjectDir(icConfig.options[ROOT_PROJECT_DIR].toFile())
                        .setBuildDir(icConfig.options[MODULE_BUILD_DIR].toFile())
                        .usePreciseJavaTracking(icConfig.options[PRECISE_JAVA_TRACKING])
                        .usePreciseCompilationResultsBackup(icConfig.options[BACKUP_CLASSES])
                        .keepIncrementalCompilationCachesInMemory(icConfig.options[KEEP_IC_CACHES_IN_MEMORY])
                        .useOutputDirs(
                            icConfig.options[OUTPUT_DIRS]?.map(Path::toFile) ?: listOf(
                                operation.destinationDirectory.toFile(),
                                icConfig.workingDirectory.toFile()
                            )
                        )
                        .forceNonIncrementalMode(icConfig.options[FORCE_RECOMPILATION])
                        .useFirRunner(icConfig.options[USE_FIR_RUNNER])
                    configV1.useIncrementalCompilation(
                        icConfig.workingDirectory.toFile(),
                        icConfig.sourcesChanges,
                        ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
                            icConfig.dependenciesSnapshotFiles.map(Path::toFile),
                            icConfig.shrunkClasspathSnapshot.toFile()
                        ),
                        snapshotBasedConfigV1
                    )
                }

                compilationService.compileJvm(
                    projectId,
                    executionPolicy.strategyConfiguration,
                    configV1,
                    operation.kotlinSources.map { it.toFile() },
                    operation.compilerArguments.toArgumentStrings() + listOf(
                        "-d",
                        operation.destinationDirectory.absolutePathString()
                    )
                ) as R
            }
            is JvmClasspathSnapshottingOperationV1Adapter -> {
                compilationService.calculateClasspathSnapshot(
                    operation.classpathEntry.toFile(), operation[GRANULARITY], operation[PARSE_INLINED_LOCAL_CLASSES]
                ) as R
            }
            else -> {
                error("Unsupported operation type with BTA API v1 fallback (compiler version ${kotlinToolchain.getCompilerVersion()}: ${operation::class.simpleName}")
            }
        }
    }

    override fun close() {
        compilationService.finishProjectCompilation(projectId)
    }
}

public fun CompilationService.asKotlinToolchain(): KotlinToolchain = KotlinToolchainV1Adapter(this)