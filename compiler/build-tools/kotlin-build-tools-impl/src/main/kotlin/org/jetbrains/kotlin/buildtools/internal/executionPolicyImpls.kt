/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.*
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyByName
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
    @SerialName("SHUTDOWN_DELAY_MILLIS")
    var shutdownDelayMillis: Long? = null,

    @EncodeDefault
    @SerialName("DAEMON_RUN_DIR_PATH")
    var daemonRunDirPath: Path = Path(DaemonOptions().runFilesPath),

    @EncodeDefault
    @SerialName("LOGS_PATH") var logsPath: Path = Path(DEFAULT_LOG_FILE_DIRECTORY),
    @SerialName("LOGS_FILE_SIZE_LIMIT")
    var logsFileSizeLimit: Long? = DEFAULT_LOG_FILE_SIZE_LIMIT,

    @SerialName("LOGS_FILE_COUNT_LIMIT")
    var logsFileCountLimit: Int? = DEFAULT_LOG_FILE_COUNT_LIMIT,
) : ExecutionPolicy.WithDaemon, ExecutionPolicy.WithDaemon.Builder,
    DeepCopyable<DaemonExecutionPolicyImpl> {

    internal constructor() : this(null)

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V =
        getPropertyByName(propertyAccessMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        setPropertyByName(propertyAccessMap, key.id, value)
    }

    override fun build(): ExecutionPolicy.WithDaemon = deepCopy()

    override fun toBuilder(): ExecutionPolicy.WithDaemon.Builder = deepCopy()

    override fun deepCopy(): DaemonExecutionPolicyImpl {
        return DaemonExecutionPolicyImpl(
            jvmArguments?.toList(),
            shutdownDelayMillis,
            daemonRunDirPath,
            logsPath,
            logsFileSizeLimit,
            logsFileCountLimit
        )
    }

    companion object {
        private val propertyAccessMap = buildMap {
            put("JVM_ARGUMENTS", DaemonExecutionPolicyImpl::jvmArguments)
            put("SHUTDOWN_DELAY_MILLIS", DaemonExecutionPolicyImpl::shutdownDelayMillis)
            put("DAEMON_RUN_DIR_PATH", DaemonExecutionPolicyImpl::daemonRunDirPath)
            put("LOGS_PATH", DaemonExecutionPolicyImpl::logsPath)
            put("LOGS_FILE_SIZE_LIMIT", DaemonExecutionPolicyImpl::logsFileSizeLimit)
            put("LOGS_FILE_COUNT_LIMIT", DaemonExecutionPolicyImpl::logsFileCountLimit)
        }
    }
}

private fun getCurrentClasspath() =
    (DaemonExecutionPolicyImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }

internal fun DaemonExecutionPolicyImpl.createDaemonConnection(
    loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    sessionIsAliveFlagFile: Lazy<File>,
): CompileServiceSession? {
    val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())

    val daemonLogOptions = DaemonLogOptions(
        logsPath = logsPath.absolutePathStringOrThrow(),
        logsFileSizeLimit = logsFileSizeLimit ?: 0,
        logsFileCountLimit = logsFileCountLimit ?: Int.MAX_VALUE,
    )
    Files.createDirectories(logsPath)

    val additionalJvmArguments = mutableListOf<String>()
    val daemonOptions = configureDaemonOptions(
        DaemonOptions().apply {
            shutdownDelayMillis?.let { shutdownDelay ->
                shutdownDelayMilliseconds = shutdownDelay
            }

            runFilesPath = daemonRunDirPath.absolutePathStringOrThrow()
            additionalJvmArguments += "D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}=${runFilesPath}"
        })

    val jvmOptions = configureDaemonJVMOptions(
        inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
    ).also { opts ->
        val effectiveJvmArguments = additionalJvmArguments + (jvmArguments ?: emptyList())
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
