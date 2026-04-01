/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector

/**
 * A base type representing a build operation with options.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Instances of concrete implementations for operations can be obtained from [KotlinToolchains] and related classes, e.g.
 * [JvmPlatformToolchain.classpathSnapshottingOperationBuilder] or [JvmPlatformToolchain.jvmCompilationOperationBuilder]
 *
 * @see KotlinToolchains.BuildSession.executeOperation
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface BuildOperation<R> {

    /**
     * A builder for configuring a [BuildOperation].
     *
     * @since 2.3.20
     */
    public interface Builder {
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
    }

    /**
     * An option for configuring a [BuildOperation].
     *
     * @see get
     * @see set
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
                "Obtain an instance of a mutable builder for the operation from the appropriate `Toolchain` instead."
    )
    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        /**
         * A collector for various metrics emitted by the compilation operation.
         */
        @JvmField
        public val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR")

        @Deprecated("Internal use only for the migration period. Will be removed soon.", level = DeprecationLevel.ERROR)
        public fun <V> createCustomOption(id: String): Option<V> = Option(id)
    }
}

/**
 * A type of [BuildOperation] that can be cancelled after it starts executing.
 *
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public interface CancellableBuildOperation<R> : BuildOperation<R> {
    /**
     * Attempts to cancel the running operation.
     *
     * Upon cancellation, the [KotlinToolchains.BuildSession.executeOperation] call that is executing this operation
     * will throw an [OperationCancelledException].
     *
     * Please note: There are no guarantees as to when and if the operation will be cancelled.
     * The exact behavior and timing of the cancellation depend on the implementation of the operation.
     * Even after a call to `cancel` returns, it's still possible that the operation completes successfully and delivers its result.
     *
     * @throws IllegalStateException if cancellation is not supported by the implementation (support was added in compiler version 2.3.20)
     * @since 2.3.20
     */
    public fun cancel() {
        error("Cancellation is supported from compiler version 2.3.20.")
    }
}