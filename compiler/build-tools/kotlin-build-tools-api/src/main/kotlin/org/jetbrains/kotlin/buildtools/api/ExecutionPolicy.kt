/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import kotlin.time.Duration

/**
 * An execution policy for a build operation.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * You can obtain an instance of this type from [KotlinToolchains.createInProcessExecutionPolicy] or [KotlinToolchains.createInProcessExecutionPolicy]
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public sealed interface ExecutionPolicy {

    /**
     * Execution policy that runs the build operation in the existing process.
     */
    public interface InProcess : ExecutionPolicy

    /**
     * Execution policy that runs the build operation using the long-running Kotlin daemon.
     */
    public interface WithDaemon : ExecutionPolicy {
        /**
         * Base class for [ExecutionPolicy.WithDaemon] options.
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
             * A list of JVM arguments to pass to the Kotlin daemon.
             */
            @JvmField
            public val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS")

            /**
             * The time that the daemon process continues to live after all clients have disconnected.
             */
            @JvmField
            public val SHUTDOWN_DELAY: Option<Duration?> = Option("SHUTDOWN_DELAY")
        }
    }
}
