/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm.operations

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Compiles Kotlin code targeting JVM platform and using specified options.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JvmPlatformToolchain.jvmCompilationOperationBuilder].
 *
 * An example of the basic usage is:
 *  ```
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.jvm.jvmCompilationOperationBuilder(listOf(Path("/path/foo.kt")), Path("/path/to/outputDirectory"))
 *   operation.compilerArguments[CommonCompilerArguments.LANGUAGE_VERSION] = KotlinVersion.V2_0
 *   toolchain.createBuildSession().use { it.executeOperation(operation.build(), toolchain.daemonExecutionPolicyBuilder().build()) }
 *  ```
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface JvmCompilationOperation : CancellableBuildOperation<CompilationResult> {

    /**
     * All sources of the compilation unit. This includes Java source files.
     *
     * @since 2.3.20
     */
    public val sources: List<Path>

    /**
     * Where to put the output of the compilation
     *
     * @since 2.3.20
     */
    public val destinationDirectory: Path

    /**
     * A builder for configuring and instantiating the [JvmCompilationOperation].
     *
     * @since 2.3.20
     */
    public interface Builder : BuildOperation.Builder {
        /**
         * All sources of the compilation unit. This includes Java source files.
         *
         * @since 2.3.20
         */
        public val sources: List<Path>

        /**
         * Where to put the output of the compilation
         *
         * @since 2.3.20
         */
        public val destinationDirectory: Path

        /**
         * Kotlin compiler configurable options for JVM platform.
         *
         * @since 2.3.20
         */
        public val compilerArguments: JvmCompilerArguments.Builder

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         *
         * @since 2.3.20
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         * @since 2.3.20
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [JvmCompilationOperation] based on the configuration of this builder.
         *
         * @since 2.3.20
         */
        public fun build(): JvmCompilationOperation

        /**
         * Creates the configuration object for snapshot-based incremental compilation (IC) in JVM projects.
         * May be used to configure incremental compilation as follows:
         * ```
         * val icConfig = compilation.snapshotBasedIcConfigurationBuilder(
         *     workingDirectory = Paths.get("build/kotlin"),
         *     sourcesChanges = SourcesChanges.ToBeCalculated,
         *     dependenciesSnapshotFiles = snapshots,
         * )
         *
         * icConfig[JvmSnapshotBasedIncrementalCompilationConfiguration.BACKUP_CLASSES] = true
         *
         * compilation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = icConfig.build()
         * ```
         *
         * @see org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
         * @since 2.4.0
         */
        public fun snapshotBasedIcConfigurationBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
        ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder

        /**
         * Creates the configuration object for snapshot-based incremental compilation (IC) in JVM projects.
         *
         * @deprecated The shrunkClasspathSnapshot parameter is no longer used. Use the 3-parameter overload instead.
         * Will be promoted to an error in KT-83937.
         * @see org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
         * @since 2.3.20
         */
        @Deprecated(
            message = "The shrunkClasspathSnapshot parameter is no longer required",
            replaceWith = ReplaceWith("snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles)"),
            level = DeprecationLevel.WARNING
        )
        public fun snapshotBasedIcConfigurationBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            dependenciesSnapshotFiles: List<Path>,
            shrunkClasspathSnapshot: Path,
        ): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder
    }

    /**
     * Creates a builder for [JvmCompilationOperation] that contains a copy of this configuration.
     *
     * @since 2.3.20
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [JvmCompilationOperation].
     *
     * @see get
     * @see set
     * @see JvmCompilationOperation.Companion
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    @Deprecated(
        "Build operations will become immutable in an upcoming release. " +
                "Use `JvmPlatformToolchain.jvmCompilationOperationBuilder` to create a mutable builder instead."
    )
    public operator fun <V> set(key: Option<V>, value: V)

    /**
     * Kotlin compiler configurable options for JVM platform.
     */
    public val compilerArguments: JvmCompilerArguments

    /**
     * Creates an options set for snapshot-based incremental compilation (IC) in JVM projects.
     * May be used to configure incremental compilation as follows:
     * ```
     * val icOptions = compilation.snapshotBasedIcConfigurationBuilder()
     *
     * icOptions[JvmIncrementalCompilationOptions.BACKUP_CLASSES] = true
     *
     * compilation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = JvmIncrementalCompilationConfiguration(
     *     workingDirectory = Paths.get("build/kotlin"),
     *     sourcesChanges = SourcesChanges.ToBeCalculated,
     *     dependenciesSnapshotFiles = snapshots,
     *     shrunkClasspathSnapshot = shrunkSnapshot,
     *     options = icOptions,
     * )
     * ```
     * @see org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
     */
    @Suppress("DEPRECATION")
    @Deprecated("JvmSnapshotBasedIncrementalCompilationOptions is deprecated. Use `snapshotBasedIcConfigurationBuilder` instead.")
    public fun createSnapshotBasedIcOptions(): org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions

    public companion object {

        /**
         * Configures usage of incremental compilation.
         *
         * @see createSnapshotBasedIcOptions
         */
        @JvmField
        public val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> =
            Option("INCREMENTAL_COMPILATION")

        /**
         * Adds a tracker that will be informed whenever the compiler makes lookups for references.
         */
        @JvmField
        public val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER")

        /**
         * An array of additional Kotlin script extensions (on top of the default `kt` and `kts`).
         *
         * The extension should not contain the leading dot (an example of a valid value `bar` for the `foo.bar` file)
         */
        @JvmField
        public val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS")

        /**
         * Controls at which logging level to display the command line arguments passed to the compiler.
         *
         * Defaults to [CompilerArgumentsLogLevel.DEBUG].
         */
        @JvmField
        public val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> = Option("COMPILER_ARGUMENTS_LOG_LEVEL")

        /**
         * Enables the Compiler Reference Index generation during the compilation.
         */
        @JvmField
        public val GENERATE_COMPILER_REF_INDEX: Option<Boolean> = Option("GENERATE_COMPILER_REF_INDEX")

        /**
         * Transform compiler diagnostics into formatted strings for output.
         *
         * If no specific renderer is provided, the system defaults to a standard format:
         * file://<path>:<line>:<column> <message>
         *
         * Example Output:
         * file:///path/to/File.kt:10:5 Unresolved reference: foo
         *
         * @see CompilerMessageRenderer
         */
        @JvmField
        public val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> = Option("COMPILER_MESSAGE_RENDERER")
    }

    public enum class CompilerArgumentsLogLevel {
        ERROR,
        WARNING,
        INFO,
        DEBUG;
    }
}

/**
 * Convenience function for creating a [JvmSnapshotBasedIncrementalCompilationConfiguration] with options configured by [builderAction].
 *
 * @return an immutable `JvmSnapshotBasedIncrementalCompilationConfiguration`.
 * @see JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder
 * @since 2.4.0
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JvmCompilationOperation.Builder.snapshotBasedIcConfiguration(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    builderAction: JvmSnapshotBasedIncrementalCompilationConfiguration.Builder.() -> Unit,
): JvmSnapshotBasedIncrementalCompilationConfiguration {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles).apply(
        builderAction
    ).build()
}

/**
 * Convenience function for creating a [JvmSnapshotBasedIncrementalCompilationConfiguration] with options configured by [builderAction].
 *
 * @deprecated The shrunkClasspathSnapshot parameter is no longer required. Use the 3-parameter overload instead.
 * Will be promoted to an error in KT-83937.
 * @return an immutable `JvmSnapshotBasedIncrementalCompilationConfiguration`.
 * @see JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder
 * @since 2.3.20
 */
@Deprecated(
    message = "The shrunkClasspathSnapshot parameter is no longer required",
    replaceWith = ReplaceWith("snapshotBasedIcConfiguration(workingDirectory, sourcesChanges, dependenciesSnapshotFiles, builderAction)"),
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JvmCompilationOperation.Builder.snapshotBasedIcConfiguration(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    shrunkClasspathSnapshot: Path,
    builderAction: JvmSnapshotBasedIncrementalCompilationConfiguration.Builder.() -> Unit,
): JvmSnapshotBasedIncrementalCompilationConfiguration {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    @Suppress("DEPRECATION")
    return snapshotBasedIcConfigurationBuilder(workingDirectory, sourcesChanges, dependenciesSnapshotFiles, shrunkClasspathSnapshot).apply(
        builderAction
    ).build()
}