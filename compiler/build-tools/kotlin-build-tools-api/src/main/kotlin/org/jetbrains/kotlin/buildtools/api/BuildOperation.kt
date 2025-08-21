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
 * Instances of concrete implementations for operations can be obtained from [KotlinToolchain] and related classes, e.g.
 * [JvmPlatformToolchain.createClasspathSnapshottingOperation] or [JvmPlatformToolchain.createJvmCompilationOperation]
 *
 * @see KotlinToolchain.BuildSession.executeOperation
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface BuildOperation<R> {
    /**
     * Base class for [JvmCompilationOperation] options.
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
    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        /**
         * A collector for various metrics emitted by the compilation operation.
         */
        @JvmField
        public val METRICS_COLLECTOR: Option<BuildMetricsCollector?> = Option("METRICS_COLLECTOR")
    }
}