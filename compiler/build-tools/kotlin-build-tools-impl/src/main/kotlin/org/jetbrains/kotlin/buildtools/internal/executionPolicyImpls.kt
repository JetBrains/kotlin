/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_COUNT_LIMIT
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_DIRECTORY
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_SIZE_LIMIT
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import java.nio.file.Path
import kotlin.io.path.Path

internal object InProcessExecutionPolicyImpl : ExecutionPolicy.InProcess

internal class DaemonExecutionPolicyImpl private constructor(private val options: Options = Options(ExecutionPolicy.WithDaemon::class)) :
    ExecutionPolicy.WithDaemon, ExecutionPolicy.WithDaemon.Builder, DeepCopyable<DaemonExecutionPolicyImpl> {

    constructor() : this(Options(ExecutionPolicy.WithDaemon::class)) {
        initializeOptions(this::class, options)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V = options[key.id]

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): ExecutionPolicy.WithDaemon = deepCopy()

    override fun toBuilder(): ExecutionPolicy.WithDaemon.Builder = deepCopy()

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    override fun deepCopy(): DaemonExecutionPolicyImpl {
        return DaemonExecutionPolicyImpl(options.deepCopy())
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        /**
         * A list of JVM arguments to pass to the Kotlin daemon.
         */
        val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS", default = null)

        /**
         * The time in milliseconds that the daemon process continues to live after all clients have disconnected.
         */
        val SHUTDOWN_DELAY_MILLIS: Option<Long?> = Option("SHUTDOWN_DELAY_MILLIS", null)

        /**
         * Specify a custom path for daemon runtime files.
         *
         * This is mainly useful for tests,
         * so that the invoker can make sure that a specific daemon is spun up for a test and no stale daemons are used.
         */
        val DAEMON_RUN_DIR_PATH: Option<Path> = Option("DAEMON_RUN_DIR_PATH", Path(DaemonOptions().runFilesPath))

        /**
         * The path to a directory where the daemon logs files should be stored.
         *
         * Kotlin daemon logs are usually prefixed with `kotlin-daemon` and have the extension `.log`.
         *
         * @since 2.4.0
         */
        val LOGS_PATH: Option<Path> = Option("LOGS_PATH", Path(DEFAULT_LOG_FILE_DIRECTORY))

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
        val LOGS_FILE_SIZE_LIMIT: Option<Long?> = Option("LOGS_FILE_SIZE_LIMIT", DEFAULT_LOG_FILE_SIZE_LIMIT)

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
        val LOGS_FILE_COUNT_LIMIT: Option<Int?> = Option("LOGS_FILE_COUNT_LIMIT", DEFAULT_LOG_FILE_COUNT_LIMIT)
    }
}
