/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.*
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.serializability.cast
import org.jetbrains.kotlin.buildtools.internal.serializability.throwPropertyNotFound
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

@Serializable
internal object InProcessExecutionPolicyImpl : ExecutionPolicy.InProcess

internal sealed interface DaemonBasedExecutionPolicy {
    var shutdownDelayMillis: Long?
    var daemonRunDirPath: Path
    var logsPath: Path
    var logsFileSizeLimit: Long?
    var logsFileCountLimit: Int?

    fun getCommonDaemonOption(id: String): Any? = when (id) {
        "SHUTDOWN_DELAY_MILLIS" -> shutdownDelayMillis
        "DAEMON_RUN_DIR_PATH" -> daemonRunDirPath
        "LOGS_PATH" -> logsPath
        "LOGS_FILE_SIZE_LIMIT" -> logsFileSizeLimit
        "LOGS_FILE_COUNT_LIMIT" -> logsFileCountLimit
        else -> throwPropertyNotFound(id)
    }

    fun setCommonDaemonOption(id: String, value: Any?) {
        when (id) {
            "SHUTDOWN_DELAY_MILLIS" -> shutdownDelayMillis = value.cast()
            "DAEMON_RUN_DIR_PATH" -> daemonRunDirPath = value.cast()
            "LOGS_PATH" -> logsPath = value.cast()
            "LOGS_FILE_SIZE_LIMIT" -> logsFileSizeLimit = value.cast()
            "LOGS_FILE_COUNT_LIMIT" -> logsFileCountLimit = value.cast()
            else -> throwPropertyNotFound(id)
        }
    }
}

@Serializable
@ConsistentCopyVisibility
internal data class DaemonExecutionPolicyImpl private constructor(
    var jvmArguments: List<String>? = null,
    override var shutdownDelayMillis: Long? = null,
    @EncodeDefault
    override var daemonRunDirPath: Path = Path(DaemonOptions().runFilesPath),
    @EncodeDefault
    override var logsPath: Path = Path(DEFAULT_LOG_FILE_DIRECTORY),
    override var logsFileSizeLimit: Long? = DEFAULT_LOG_FILE_SIZE_LIMIT,
    override var logsFileCountLimit: Int? = DEFAULT_LOG_FILE_COUNT_LIMIT,
) : ExecutionPolicy.WithDaemon, ExecutionPolicy.WithDaemon.Builder,
    DeepCopyable<DaemonExecutionPolicyImpl>, DaemonBasedExecutionPolicy {

    internal constructor() : this(null)

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V =
        (if (key.id == "JVM_ARGUMENTS") jvmArguments else getCommonDaemonOption(key.id)) as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        if (key.id == "JVM_ARGUMENTS") {
            jvmArguments = value.cast()
        } else {
            setCommonDaemonOption(key.id, value)
        }
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
}

private fun getCurrentClasspath() =
    (DaemonExecutionPolicyImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }

internal fun DaemonBasedExecutionPolicy.createCompilerId(): CompilerId = when (this) {
    is DaemonExecutionPolicyImpl -> CompilerId.makeCompilerId(getCurrentClasspath())
    is NativeImageDaemonExecutionPolicyImpl -> CompilerId.makeNativeImageCompilerId(
        dist.daemonExecutable.toFile(),
        KotlinCompilerVersion.VERSION
    )
}

internal fun DaemonBasedExecutionPolicy.createDaemonConnection(
    logger: KotlinLogger,
    messageCollector: MessageCollectorWithDiagnosticId,
    sessionIsAliveFlagFile: Lazy<File>,
): CompileServiceSession? {
    val daemonLogOptions = DaemonLogOptions(
        logsPath = logsPath.absolutePathStringOrThrow(),
        logsFileSizeLimit = logsFileSizeLimit ?: 0,
        logsFileCountLimit = logsFileCountLimit ?: Int.MAX_VALUE,
    )
    Files.createDirectories(logsPath)

    val daemonRunFilesPath = daemonRunDirPath.absolutePathStringOrThrow()
    val daemonOptions = configureDaemonOptions(
        DaemonOptions().apply {
            shutdownDelayMillis?.let { shutdownDelay ->
                shutdownDelayMilliseconds = shutdownDelay
            }
            runFilesPath = daemonRunFilesPath
        }
    )

    val daemonJVMOptions = when (this) {
        is DaemonExecutionPolicyImpl -> {
            val customRunFilesPathProperty = "D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}="
            val additionalJvmArguments = listOf(customRunFilesPathProperty + daemonRunFilesPath) + jvmArguments.orEmpty()
            configureDaemonJVMOptions(
                inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
            ).apply {
                jvmParams.addAll(additionalJvmArguments.filterExtractProps(mappers, "", restMapper))
            }
        }
        is NativeImageDaemonExecutionPolicyImpl -> DaemonJVMOptions(reservedCodeCacheSize = "")
    }

    return KotlinCompilerRunnerUtils.newDaemonConnection(
        createCompilerId(),
        clientIsAliveFile,
        sessionIsAliveFlagFile.value,
        messageCollector,
        logger.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")?.toBooleanStrictOrNull() ?: true,
        daemonJVMOptions = daemonJVMOptions,
        daemonOptions = daemonOptions,
        daemonLogOptions = daemonLogOptions,
    )?.also { compileServiceSession ->
        if (this is DaemonExecutionPolicyImpl && logger.isDebugEnabled) {
            compileServiceSession.compileService.getDaemonJVMOptions().takeIf { it.isGood }?.let { jvmOpts ->
                logger.debug("Kotlin compile daemon JVM options: ${jvmOpts.get().mappers.flatMap { it.toArgs("-") }}")
            }
        }
    }
}

@Serializable
internal data class NativeImageDistribution(val home: Path) {
    /**
     * The path of the native image compiler executable in the distribution.
     */
    val compilerExecutable: Path
        get() = home.resolve("bin").resolve(if (isWindows) "kotlinc-native-image.exe" else "kotlinc-native-image").absolute()

    /**
     * The path of the native image daemon executable in the distribution.
     */
    val daemonExecutable: Path
        get() = home.resolve("bin").resolve(if (isWindows) "kotlin-daemon-native-image.exe" else "kotlin-daemon-native-image").absolute()

}


private const val NATIVE_IMAGE_DIST_PATH_ID = "DIST_PATH"

internal sealed interface NativeImageBasedExecutionPolicy {
    var distPath: Path?

    val dist: NativeImageDistribution
        get() = NativeImageDistribution(requireDistPath())

    fun requireDistPath(): Path = checkNotNull(distPath) { "$NATIVE_IMAGE_DIST_PATH_ID must be set for the native image execution policy." }
}

@Serializable
internal data class NativeImageProcessExecutionPolicyImpl(
    override var distPath: Path? = null,
) : ExecutionPolicy.NativeImageProcess, ExecutionPolicy.NativeImageProcess.Builder,
    DeepCopyable<NativeImageProcessExecutionPolicyImpl>, NativeImageBasedExecutionPolicy {

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.NativeImageProcess.Option<V>): V = when (key.id) {
        NATIVE_IMAGE_DIST_PATH_ID -> requireDistPath()
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.NativeImageProcess.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            NATIVE_IMAGE_DIST_PATH_ID -> distPath = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override fun build(): ExecutionPolicy.NativeImageProcess {
        requireDistPath()
        return deepCopy()
    }

    override fun toBuilder(): ExecutionPolicy.NativeImageProcess.Builder = deepCopy()

    override fun deepCopy(): NativeImageProcessExecutionPolicyImpl = copy()
}

@Serializable
internal data class NativeImageDaemonExecutionPolicyImpl(
    override var distPath: Path? = null,
    override var shutdownDelayMillis: Long? = null,
    @EncodeDefault
    override var daemonRunDirPath: Path = Path(DaemonOptions().runFilesPath),
    @EncodeDefault
    override var logsPath: Path = Path(DEFAULT_LOG_FILE_DIRECTORY),
    override var logsFileSizeLimit: Long? = DEFAULT_LOG_FILE_SIZE_LIMIT,
    override var logsFileCountLimit: Int? = DEFAULT_LOG_FILE_COUNT_LIMIT,
) : ExecutionPolicy.NativeImageDaemon, ExecutionPolicy.NativeImageDaemon.Builder,
    DeepCopyable<NativeImageDaemonExecutionPolicyImpl>, DaemonBasedExecutionPolicy, NativeImageBasedExecutionPolicy {

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.NativeImageDaemon.Option<V>): V =
        (if (key.id == NATIVE_IMAGE_DIST_PATH_ID) requireDistPath() else getCommonDaemonOption(key.id)) as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.NativeImageDaemon.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        if (key.id == NATIVE_IMAGE_DIST_PATH_ID) {
            distPath = value.cast()
        } else {
            setCommonDaemonOption(key.id, value)
        }
    }

    override fun build(): ExecutionPolicy.NativeImageDaemon {
        requireDistPath()
        return deepCopy()
    }

    override fun toBuilder(): ExecutionPolicy.NativeImageDaemon.Builder = deepCopy()

    override fun deepCopy(): NativeImageDaemonExecutionPolicyImpl = NativeImageDaemonExecutionPolicyImpl(
        distPath,
        shutdownDelayMillis,
        daemonRunDirPath,
        logsPath,
        logsFileSizeLimit,
        logsFileCountLimit,
    )
}
