/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

/**
 * An execution policy for a build operation.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * You can obtain an instance of this type from [KotlinToolchains.createInProcessExecutionPolicy] or [KotlinToolchains.daemonExecutionPolicyBuilder]
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
         * A builder for configuring and instantiating the [WithDaemon] execution policy.
         *
         * @since 2.3.20
         */
        public interface Builder {
            /**
             * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
             *
             * @return the previously set value for an option
             * @throws IllegalStateException if the option was not set and has no default value
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
             * Creates an immutable instance of [WithDaemon] based on the configuration of this builder.
             *
             * @since 2.3.20
             */
            public fun build(): WithDaemon

            public operator fun <T> Option<T>.invoke(value: T) {
                set(this, value)
            }
        }

        /**
         * Creates a builder for [WithDaemon] that contains a copy of this configuration.
         *
         * @since 2.3.20
         */
        public fun toBuilder(): Builder

        /**
         * An option for configuring an [ExecutionPolicy.WithDaemon].
         *
         * @see get
         * @see set
         */
        public class Option<V> internal constructor(
            id: String,
            public val availableSinceVersion: KotlinReleaseVersion,
        ) : BaseOption<V>(id)

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         */
        public operator fun <V> get(key: Option<V>): V

        public companion object {
            /**
             * A list of JVM arguments to pass to the Kotlin daemon.
             */
            @JvmField
            public val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS", KotlinReleaseVersion(2, 3, 0))

            /**
             * The time in milliseconds that the daemon process continues to live after all clients have disconnected.
             */
            @JvmField
            public val SHUTDOWN_DELAY_MILLIS: Option<Long?> = Option("SHUTDOWN_DELAY_MILLIS", KotlinReleaseVersion(2, 3, 0))

            /**
             * Specify a custom path for daemon runtime files.
             *
             * This is mainly useful for tests,
             * so that the invoker can make sure that a specific daemon is spun up for a test and no stale daemons are used.
             *
             * @since 2.3.20
             */
            @JvmField
            @DelicateBuildToolsApi
            public val DAEMON_RUN_DIR_PATH: Option<Path> = Option("DAEMON_RUN_DIR_PATH", KotlinReleaseVersion(2, 3, 20))

            /**
             * The path to a directory where the daemon logs files should be stored.
             *
             * Kotlin daemon logs are usually prefixed with `kotlin-daemon` and have the extension `.log`.
             *
             * @since 2.4.0
             */
            @JvmField
            public val LOGS_PATH: Option<Path> = Option("LOGS_PATH", KotlinReleaseVersion(2, 4, 0))

            /**
             * The limit for the maximum size of log files, expressed in bytes.
             *
             * This option can be used to control the storage size allocated for log files.
             * If the size of the log files exceeds this limit, appropriate actions such as
             * truncation or log rotation may be applied.
             *
             * The value for this option must be a positive [Long] representing the maximum size of a log file.
             *
             * If unset (`null`), no size limit is applied. By default, a non-null limit is used.
             *
             * @since 2.4.0
             */
            @JvmField
            public val LOGS_FILE_SIZE_LIMIT: Option<Long?> = Option("LOGS_FILE_SIZE_LIMIT", KotlinReleaseVersion(2, 4, 0))

            /**
             * Specifies the maximum number of log files that can be retained when [[LOGS_FILE_SIZE_LIMIT]] is set.
             *
             * This option is primarily used to limit the
             * number of historical log files maintained on the filesystem to avoid excessive storage consumption.
             *
             * The value for this option must be a positive [Int] representing the maximum number of log files.
             *
             * If unset (`null`), no size limit is applied. By default, a non-null limit is used.
             *
             * @since 2.4.0
             */
            @JvmField
            public val LOGS_FILE_COUNT_LIMIT: Option<Int?> = Option("LOGS_FILE_COUNT_LIMIT", KotlinReleaseVersion(2, 4, 0))
        }
    }
}
