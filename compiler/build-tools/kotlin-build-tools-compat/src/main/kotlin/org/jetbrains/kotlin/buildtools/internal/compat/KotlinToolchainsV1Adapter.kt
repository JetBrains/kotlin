/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.compat

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
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
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.nio.file.Path
import java.time.Duration
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

public class KotlinToolchainsV1Adapter(
    @Suppress("DEPRECATION_ERROR") private val compilationService: CompilationService,
) : KotlinToolchains {
    private val jvm: JvmPlatformToolchain by lazy {
        object : JvmPlatformToolchain {
            @Deprecated(
                "Use jvmCompilationOperationBuilder instead",
                replaceWith = ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)")
            )
            override fun createJvmCompilationOperation(
                sources: List<Path>,
                destinationDirectory: Path,
            ): JvmCompilationOperation {
                return JvmCompilationOperationV1Adapter(compilationService, sources, destinationDirectory, JvmCompilerArgumentsImpl())
            }

            override fun jvmCompilationOperationBuilder(
                sources: List<Path>,
                destinationDirectory: Path,
            ): JvmCompilationOperation.Builder {
                return JvmCompilationOperationV1Adapter(compilationService, sources, destinationDirectory, JvmCompilerArgumentsImpl())
            }

            @Deprecated(
                "Use `classpathSnapshottingOperationBuilder` instead",
                replaceWith = ReplaceWith("classpathSnapshottingOperationBuilder(classpathEntry)")
            )
            override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
                return JvmClasspathSnapshottingOperationV1Adapter(compilationService, classpathEntry)
            }

            override fun classpathSnapshottingOperationBuilder(classpathEntry: Path): JvmClasspathSnapshottingOperation.Builder {
                return JvmClasspathSnapshottingOperationV1Adapter(compilationService, classpathEntry)
            }

            override fun discoverScriptExtensionsOperationBuilder(classpath: List<Path>): DiscoverScriptExtensionsOperation.Builder {
                return DiscoverScriptExtensionsOperationV1Adapter(compilationService, classpath)
            }
        }
    }

    private class DiscoverScriptExtensionsOperationV1Adapter(
        @Suppress("DEPRECATION_ERROR") val compilationService: CompilationService,
        override val classpath: List<Path>,
        override val options: Options = Options(DiscoverScriptExtensionsOperation::class),
    ) : BuildOperationImpl<Collection<String>>(), DiscoverScriptExtensionsOperation, DiscoverScriptExtensionsOperation.Builder,
        DeepCopyable<DiscoverScriptExtensionsOperationV1Adapter> {


        override fun executeImpl(
            projectId: ProjectId,
            executionPolicy: ExecutionPolicyV1Adapter,
            logger: KotlinLogger?,
        ): Collection<String> {
            check(executionPolicy is ExecutionPolicy.InProcess) { "Only in-process execution policy is supported for this operation." }
            return compilationService.getCustomKotlinScriptFilenameExtensions(classpath.map(Path::toFile))
        }

        override fun toBuilder(): DiscoverScriptExtensionsOperation.Builder = deepCopy()

        override fun <V> get(key: DiscoverScriptExtensionsOperation.Option<V>): V = options[key]

        override fun <V> set(
            key: DiscoverScriptExtensionsOperation.Option<V>,
            value: V,
        ) {
            options[key] = value
        }

        override fun build(): DiscoverScriptExtensionsOperation = deepCopy()

        override fun deepCopy(): DiscoverScriptExtensionsOperationV1Adapter {
            return DiscoverScriptExtensionsOperationV1Adapter(compilationService, classpath, options.deepCopy())
        }
    }

    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            JvmPlatformToolchain::class.java -> jvm
            else -> error("Unsupported platform toolchain type: $type. Only JVM compilation is supported in BTA API v1 fallback (compiler version ${getCompilerVersion()}).")
        } as T
    }

    override fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess {
        return ExecutionPolicyV1Adapter.InProcess(compilationService.makeCompilerExecutionStrategyConfiguration().useInProcessStrategy())
    }

    @Deprecated(
        "Use daemonExecutionPolicyBuilder instead",
        replaceWith = ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)")
    )
    override fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon {
        return ExecutionPolicyV1Adapter.WithDaemon(compilationService)
    }

    override fun daemonExecutionPolicyBuilder(): ExecutionPolicy.WithDaemon.Builder =
        ExecutionPolicyV1Adapter.WithDaemon(compilationService)

    override fun getCompilerVersion(): String {
        return compilationService.getCompilerVersion()
    }

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionV1Adapter(this, RandomProjectUUID(), compilationService)
    }
}

private class JvmClasspathSnapshottingOperationV1Adapter private constructor(
    override val options: Options = Options(JvmClasspathSnapshottingOperation::class),
    @Suppress("DEPRECATION_ERROR") val compilationService: CompilationService,
    override val classpathEntry: Path,
) : BuildOperationImpl<ClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation, JvmClasspathSnapshottingOperation.Builder,
    DeepCopyable<JvmClasspathSnapshottingOperationV1Adapter> {

    constructor(
        @Suppress("DEPRECATION_ERROR") compilationService: CompilationService,
        classpathEntry: Path,
    ) : this(Options(JvmClasspathSnapshottingOperation::class), compilationService, classpathEntry)


    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = options[key]

    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun toBuilder(): JvmClasspathSnapshottingOperation.Builder = deepCopy()

    override fun build(): JvmClasspathSnapshottingOperation = deepCopy()

    override fun deepCopy(): JvmClasspathSnapshottingOperationV1Adapter =
        JvmClasspathSnapshottingOperationV1Adapter(options.deepCopy(), compilationService, classpathEntry)

    operator fun <V> get(key: Option<V>): V = options[key]

    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    override fun executeImpl(
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

private class JvmCompilationOperationV1Adapter private constructor(
    override val options: Options = Options(JvmCompilationOperation::class),
    @Suppress("DEPRECATION_ERROR") val compilationService: CompilationService,
    override val sources: List<Path>,
    override val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl,
) : BuildOperationImpl<CompilationResult>(), JvmCompilationOperation, JvmCompilationOperation.Builder,
    DeepCopyable<JvmCompilationOperationV1Adapter> {
    constructor(
        @Suppress("DEPRECATION_ERROR") compilationService: CompilationService,
        kotlinSources: List<Path>,
        destinationDirectory: Path,
        compilerArguments: JvmCompilerArgumentsImpl,
    ) : this(Options(JvmCompilationOperation::class), compilationService, kotlinSources, destinationDirectory, compilerArguments)

    override fun toBuilder(): JvmCompilationOperation.Builder = deepCopy()

    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = options[key]

    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun build(): JvmCompilationOperation = deepCopy()

    override fun deepCopy(): JvmCompilationOperationV1Adapter {
        return JvmCompilationOperationV1Adapter(
            options.deepCopy(),
            compilationService,
            sources,
            destinationDirectory,
            JvmCompilerArgumentsImpl().also { it.applyArgumentStrings(compilerArguments.toArgumentStrings()) })
    }

    override fun snapshotBasedIcConfigurationBuilder(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
    ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
        return JvmSnapshotBasedIncrementalCompilationConfigurationV1Adapter(
            workingDirectory, sourcesChanges, dependenciesSnapshotFiles,
            JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter(options.deepCopy())
        )
    }

    @Deprecated(
        "The shrunkClasspathSnapshot parameter is no longer required",
        replaceWith = ReplaceWith("snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles)"),
        level = DeprecationLevel.WARNING
    )
    override fun snapshotBasedIcConfigurationBuilder(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
        shrunkClasspathSnapshot: Path,
    ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {

        return JvmSnapshotBasedIncrementalCompilationConfigurationV1Adapter(
            workingDirectory, sourcesChanges, dependenciesSnapshotFiles, shrunkClasspathSnapshot,
            JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter(options.deepCopy())
        )
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

    @Suppress("DEPRECATION_ERROR")
    override fun executeImpl(
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
                @Suppress("DEPRECATION_ERROR") ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
                    icConfig.dependenciesSnapshotFiles.map(Path::toFile),
                    icConfig.shrunkClasspathSnapshot.toFile()
                ),
                snapshotBasedConfigV1
            )
        }

        val javaSources = sources.filter { it.toFile().isJavaFile() }.map { it.absolutePathStringOrThrow() }
        val compilerArguments = compilerArguments.toArgumentStrings().fixForFirCheck() + listOf(
            "-d",
            destinationDirectory.absolutePathStringOrThrow()
        )
        return compilationService.compileJvm(
            projectId,
            executionPolicy.strategyConfiguration,
            config,
            sources.map { it.toFile() },
            if (compilationService.treatsJavaSourcesProperly()) compilerArguments else compilerArguments + javaSources,
        )
    }

    /**
     * It's better to avoid arguments duplication for the versions that contain the fix
     */
    @Suppress("DEPRECATION_ERROR")
    private fun CompilationService.treatsJavaSourcesProperly(): Boolean = try {
        val kotlinCompilerVersion = KotlinToolingVersion(getCompilerVersion())
        kotlinCompilerVersion >= KotlinToolingVersion(2, 2, 21, null)
    } catch (_: Exception) {
        // there might be no getCompilerVersion in older versions
        false
    }

    private class JvmSnapshotBasedIncrementalCompilationConfigurationV1Adapter(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
        shrunkClasspathSnapshot: Path,
        @Deprecated("Use `get` and `set` directly instead.")
        override val options: JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter = JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter(),
    ) : JvmSnapshotBasedIncrementalCompilationConfiguration(
        workingDirectory,
        sourcesChanges,
        dependenciesSnapshotFiles,
        shrunkClasspathSnapshot,
        options
    ), JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
        DeepCopyable<JvmSnapshotBasedIncrementalCompilationConfigurationV1Adapter> {

        constructor(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
            option: JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter,
        ) : this(workingDirectory, sourcesChanges, dependenciesSnapshotFiles, workingDirectory.resolve("shrunk-classpath-snapshot.bin"), option)

        override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>): V {
            return options.options[key]
        }

        override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
            options.options[key] = value
        }

        override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

        operator fun <V> get(key: Option<V>): V {
            return options.options[key]
        }

        operator fun <V> set(key: Option<V>, value: V) {
            options.options[key] = value
        }

        override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationV1Adapter {
            return JvmSnapshotBasedIncrementalCompilationConfigurationV1Adapter(
                workingDirectory, sourcesChanges, dependenciesSnapshotFiles, shrunkClasspathSnapshot, options.deepCopy()
            )
        }

        class Option<V> : BaseOptionWithDefault<V> {
            constructor(id: String) : super(id)
            constructor(id: String, default: V) : super(id, default = default)
        }
    }

    private class JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter(
        val options: Options = Options(
            JvmSnapshotBasedIncrementalCompilationOptions::class
        ),
    ) : JvmSnapshotBasedIncrementalCompilationOptions, DeepCopyable<JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter> {

        operator fun <V> get(key: Option<V>): V = options[key]

        private operator fun <V> set(key: Option<V>, value: V) {
            options[key] = value
        }

        override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = options[key]

        override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
            options[key] = value
        }

        override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter {
            return JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter(options.deepCopy())
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

    @Deprecated("Use `snapshotBasedIcConfigurationBuilder` instead.")
    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsV1Adapter(options.deepCopy())
    }
}

private fun Path.absolutePathStringOrThrow(): String = toFile().absolutePath

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
    @Suppress("DEPRECATION_ERROR")
    val strategyConfiguration: CompilerExecutionStrategyConfiguration

    class InProcess(@Suppress("DEPRECATION_ERROR") override val strategyConfiguration: CompilerExecutionStrategyConfiguration) :
        ExecutionPolicyV1Adapter, ExecutionPolicy.InProcess

    class WithDaemon private constructor(
        private val options: Options = Options(ExecutionPolicy.WithDaemon::class),
        @Suppress("DEPRECATION_ERROR") private val compilationService: CompilationService,
    ) : ExecutionPolicyV1Adapter,
        ExecutionPolicy.WithDaemon, ExecutionPolicy.WithDaemon.Builder, DeepCopyable<WithDaemon> {

        @Suppress("DEPRECATION_ERROR")
        constructor(compilationService: CompilationService) : this(Options(ExecutionPolicy.WithDaemon::class), compilationService)

        @Suppress("DEPRECATION_ERROR")
        private fun CompilationService.supportsShutdownDelayInDaemon(): Boolean = try {
            val kotlinCompilerVersion = KotlinToolingVersion(getCompilerVersion())
            kotlinCompilerVersion >= KotlinToolingVersion(2, 3, 0, null)
        } catch (_: Exception) {
            // there might be no getCompilerVersion in older versions
            false
        }

        @Suppress("DEPRECATION_ERROR")
        override val strategyConfiguration: CompilerExecutionStrategyConfiguration
            get() {
                val jvmArguments = get(JVM_ARGUMENTS) ?: emptyList()
                return get(SHUTDOWN_DELAY_MILLIS).let { delay ->
                    if (delay != null && compilationService.supportsShutdownDelayInDaemon()) {
                        compilationService.makeCompilerExecutionStrategyConfiguration().useDaemonStrategy(
                            jvmArguments, Duration.ofMillis(delay)
                        )
                    } else {
                        compilationService.makeCompilerExecutionStrategyConfiguration().useDaemonStrategy(
                            jvmArguments
                        )
                    }
                }
            }

        override fun toBuilder(): ExecutionPolicy.WithDaemon.Builder = deepCopy()

        override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V = options[key.id]

        override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
            options[key] = value
        }

        override fun build(): ExecutionPolicy.WithDaemon = deepCopy()

        operator fun <V> get(key: Option<V>): V = options[key]

        operator fun <V> set(key: Option<V>, value: V) {
            options[key] = value
        }

        override fun deepCopy(): WithDaemon = WithDaemon(options.deepCopy(), compilationService)

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
             * The time in milliseconds that the daemon process continues to live after all clients have disconnected.
             */
            val SHUTDOWN_DELAY_MILLIS: Option<Long?> = Option("SHUTDOWN_DELAY_MILLIS", null)
        }
    }
}

private class BuildSessionV1Adapter(
    override val kotlinToolchains: KotlinToolchains,
    override val projectId: ProjectId,
    @Suppress("DEPRECATION_ERROR") private val compilationService: CompilationService,
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
            is BuildOperationImpl -> {
                operation.execute(projectId, executionPolicy, logger)
            }
            else -> {
                error("Unsupported operation type with BTA API v1 fallback (compiler version ${kotlinToolchains.getCompilerVersion()}: ${operation::class.simpleName}).")
            }
        }
    }

    override fun close() {
        compilationService.finishProjectCompilation(projectId)
    }
}

@Suppress("DEPRECATION_ERROR")
public fun CompilationService.asKotlinToolchains(): KotlinToolchains = KotlinToolchainsV1Adapter(this)

@OptIn(ExperimentalAtomicApi::class)
private abstract class BuildOperationImpl<R> : BuildOperation<R> {
    protected abstract val options: Options
    private val executionStarted = AtomicBoolean(false)

    override fun <V> get(key: BuildOperation.Option<V>): V = options[key.id]

    @Deprecated("Build operations will become immutable in an upcoming release. Obtain an instance of a mutable builder for the operation from the appropriate `Toolchain` instead.")
    override fun <V> set(key: BuildOperation.Option<V>, value: V) {
        options[key] = value
    }

    fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicyV1Adapter, logger: KotlinLogger? = null): R {
        check(executionStarted.compareAndSet(expectedValue = false, newValue = true)) {
            "Build operation $this already started execution."
        }
        return executeImpl(projectId, executionPolicy, logger)
    }

    abstract fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicyV1Adapter, logger: KotlinLogger? = null): R

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