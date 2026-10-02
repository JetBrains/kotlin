/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class, ListOfPathsAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.ListOfPathsAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.findPropertyWithSerialName
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.DAEMON_RUN_DIR_PATH
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.JVM_ARGUMENTS
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.LOGS_FILE_COUNT_LIMIT
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.LOGS_FILE_SIZE_LIMIT
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.LOGS_PATH
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.SHUTDOWN_DELAY_MILLIS
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
internal object InProcessExecutionPolicyImpl : ExecutionPolicy.InProcess

@Serializable
@ConsistentCopyVisibility
internal data class DaemonExecutionPolicyImpl private constructor(
    @SerialName("JVM_ARGUMENTS") var jvmArguments: List<String>? = null,
    @SerialName("SHUTDOWN_DELAY_MILLIS") var shutdownDelayMillis: Long? = null,
    @SerialName("DAEMON_RUN_DIR_PATH") var daemonRunDirPath: Path = Path(DaemonOptions().runFilesPath),
    @SerialName("LOGS_PATH") var logsPath: Path = Path(DEFAULT_LOG_FILE_DIRECTORY),
    @SerialName("LOGS_FILE_SIZE_LIMIT") var logsFileSizeLimit: Long? = DEFAULT_LOG_FILE_SIZE_LIMIT,
    @SerialName("LOGS_FILE_COUNT_LIMIT") var logsFileCountLimit: Int? = DEFAULT_LOG_FILE_COUNT_LIMIT,
) : ExecutionPolicy.WithDaemon, ExecutionPolicy.WithDaemon.Builder,
    DeepCopyable<DaemonExecutionPolicyImpl> {

    internal constructor() : this(null)

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V =
        this::class.findPropertyWithSerialName(key.id).getter.call(this) as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        this::class.findPropertyWithSerialName(key.id).setter.call(this, value)
    }

    override fun build(): ExecutionPolicy.WithDaemon = deepCopy()

    override fun toBuilder(): ExecutionPolicy.WithDaemon.Builder = deepCopy()

    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: Option<V>): V = this::class.findPropertyWithSerialName(key.id).getter.call(this) as V

    override fun deepCopy(): DaemonExecutionPolicyImpl {
        return DaemonExecutionPolicyImpl(
            jvmArguments,
            shutdownDelayMillis,
            daemonRunDirPath,
            logsPath,
            logsFileSizeLimit,
            logsFileCountLimit
        )
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        /**
         * A list of JVM arguments to pass to the Kotlin daemon.
         */
        val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS")

        /**
         * The time in milliseconds that the daemon process continues to live after all clients have disconnected.
         */
        val SHUTDOWN_DELAY_MILLIS: Option<Long?> = Option("SHUTDOWN_DELAY_MILLIS")

        /**
         * Specify a custom path for daemon runtime files.
         *
         * This is mainly useful for tests,
         * so that the invoker can make sure that a specific daemon is spun up for a test and no stale daemons are used.
         */
        val DAEMON_RUN_DIR_PATH: Option<Path> = Option("DAEMON_RUN_DIR_PATH")

        /**
         * The path to a directory where the daemon logs files should be stored.
         *
         * Kotlin daemon logs are usually prefixed with `kotlin-daemon` and have the extension `.log`.
         *
         * @since 2.4.0
         */
        val LOGS_PATH: Option<Path> = Option("LOGS_PATH")

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
        val LOGS_FILE_SIZE_LIMIT: Option<Long?> = Option("LOGS_FILE_SIZE_LIMIT")

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
        val LOGS_FILE_COUNT_LIMIT: Option<Int?> = Option("LOGS_FILE_COUNT_LIMIT")
    }
}

@OptIn(ExperimentalSerializationApi::class)
public fun main() {
    val executionPolicy = DaemonExecutionPolicyImpl().apply {
        logsPath = Path("/home/something")
    }
//    val ba = ProtoBuf.encodeToByteArray(executionPolicy)
//    val executionPolicy2: DaemonExecutionPolicyImpl = ProtoBuf.decodeFromByteArray(ba)

    val ba = Json.encodeToString(executionPolicy)
    println(ba)
    val executionPolicy2: DaemonExecutionPolicyImpl = Json.decodeFromString(ba)

    println(executionPolicy.logsPath)
    println(executionPolicy2.logsPath)


    val arguments = JvmCompilerArgumentsImpl()
    arguments.d = "abc"
    arguments.`jvm-default` = JvmDefaultMode.NO_COMPATIBILITY
    val argsJson = Json.encodeToString(arguments)
    println(argsJson)
    val arguments2 = Json.decodeFromString<JvmCompilerArgumentsImpl>(argsJson)
    println(arguments2.toArgumentStrings())

    val jvmoperation =
        JvmCompilationOperationImpl(listOf(Path("/home/something")), Path("/dest"), compilerVersion = "2.5.255-SNAPSHOT").apply {
            this[BaseCompilationOperationImpl.LOOKUP_TRACKER] = object : CompilerLookupTracker {
                override fun recordLookup(
                    filePath: String,
                    scopeFqName: String,
                    scopeKind: CompilerLookupTracker.ScopeKind,
                    name: String,
                ) {
                    println("record lookup")
                }

                override fun clear() {
                    println("clear")
                }
            }
        }
    val lookupTrackerForOperation100 = jvmoperation.prepareForSerialization()
    println(Json.encodeToString(jvmoperation))
}

private fun getCurrentClasspath() =
    (DaemonExecutionPolicyImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }

internal fun DaemonExecutionPolicyImpl.createDaemonConnection(
    loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    sessionIsAliveFlagFile: Lazy<File>,
): CompileServiceSession? {
    val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())

    val daemonLogOptions = DaemonLogOptions(
        logsPath = this[LOGS_PATH].absolutePathStringOrThrow(),
        logsFileSizeLimit = this[LOGS_FILE_SIZE_LIMIT] ?: 0,
        logsFileCountLimit = this[LOGS_FILE_COUNT_LIMIT] ?: Int.MAX_VALUE,
    )
    Files.createDirectories(this[LOGS_PATH])

    val additionalJvmArguments = mutableListOf<String>()
    val daemonOptions = configureDaemonOptions(
        DaemonOptions().apply {
            this@createDaemonConnection[SHUTDOWN_DELAY_MILLIS]?.let { shutdownDelay ->
                shutdownDelayMilliseconds = shutdownDelay
            }

            runFilesPath = this@createDaemonConnection[DAEMON_RUN_DIR_PATH].absolutePathStringOrThrow()
            additionalJvmArguments += "D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}=${runFilesPath}"
        })

    val jvmOptions = configureDaemonJVMOptions(
        inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
    ).also { opts ->
        val effectiveJvmArguments = additionalJvmArguments + (this[JVM_ARGUMENTS] ?: emptyList())
        if (effectiveJvmArguments.isNotEmpty()) {
            opts.jvmParams.addAll(
                effectiveJvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
            )
        }
    }

    return KotlinCompilerRunnerUtils.newDaemonConnection(
        compilerId,
        clientIsAliveFile,
        sessionIsAliveFlagFile.value,
        loggerAdapter,
        loggerAdapter.kotlinLogger.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")?.toBooleanStrictOrNull() ?: true,
        daemonJVMOptions = jvmOptions,
        daemonOptions = daemonOptions,
        daemonLogOptions = daemonLogOptions,
    )?.also { compileServiceSession ->
        if (loggerAdapter.kotlinLogger.isDebugEnabled) {
            compileServiceSession.compileService.getDaemonJVMOptions().takeIf { it.isGood }?.let { jvmOpts ->
                loggerAdapter.kotlinLogger.debug("Kotlin compile daemon JVM options: ${jvmOpts.get().mappers.flatMap { it.toArgs("-") }}")
            }
        }
    }
}
