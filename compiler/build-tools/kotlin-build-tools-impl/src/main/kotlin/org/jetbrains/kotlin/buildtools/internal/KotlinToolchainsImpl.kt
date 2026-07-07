/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.BaseCompilationOperationImpl.Companion.COMPILER_ARGUMENTS_LOG_LEVEL
import org.jetbrains.kotlin.buildtools.internal.BaseCompilationOperationImpl.Companion.LOOKUP_TRACKER
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.cri.CriToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.js.JsPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.KotlinMetadataPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmPlatformToolchainImpl
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.incremental.clearJarCaches
import java.io.File
import java.net.URLClassLoader
import java.rmi.RemoteException
import java.util.concurrent.*

internal class KotlinToolchainsImpl() : KotlinToolchains {
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap()
    val toolchains: ConcurrentHashMap<Class<*>, KotlinToolchains.Toolchain> = ConcurrentHashMap()

    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return toolchains.computeIfAbsent(type) { type ->
            when (type) {
                JvmPlatformToolchain::class.java -> JvmPlatformToolchainImpl(getCompilerVersion())
                JsPlatformToolchain::class.java -> JsPlatformToolchainImpl(getCompilerVersion())
                WasmPlatformToolchain::class.java -> WasmPlatformToolchainImpl(getCompilerVersion())
                KotlinMetadataPlatformToolchain::class.java -> KotlinMetadataPlatformToolchainImpl(getCompilerVersion())
                CriToolchain::class.java -> CriToolchainImpl()
                AbiValidationToolchain::class.java -> AbiValidationToolchainImpl()
                else -> error("Unsupported platform toolchain type: $type.")
            }
        } as T
    }

    override fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess = InProcessExecutionPolicyImpl

    @Deprecated(
        "Use jvmCompilationOperationBuilder instead",
        replaceWith = ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)"),
        level = DeprecationLevel.HIDDEN
    )
    fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon = DaemonExecutionPolicyImpl(buildIdToSessionFlagFile)

    override fun daemonExecutionPolicyBuilder(): ExecutionPolicy.WithDaemon.Builder = DaemonExecutionPolicyImpl(buildIdToSessionFlagFile)

    override fun getCompilerVersion(): String = KotlinCompilerVersion.VERSION

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionImpl(this, RandomProjectUUID(), buildIdToSessionFlagFile)
    }

    private class BuildSessionImpl(
        override val kotlinToolchains: KotlinToolchains,
        override val projectId: ProjectId,
        private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    ) : KotlinToolchains.BuildSession {
        private val executorDelegate = lazy {
            Executors.newCachedThreadPool()
        }
        private val executor by executorDelegate

        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(
            operation: BuildOperation<R>,
            executionPolicy: ExecutionPolicy,
            logger: KotlinLogger?,
        ): R {
            check(operation is BuildOperationImpl<R>) { "Unknown operation type: ${operation::class.qualifiedName}" }
            val operationBody: Callable<R> = { operation.execute(projectId, executionPolicy, logger) }
            return when (executionPolicy) {
                is ExecutionPolicy.InProcess -> {
                    unwrapExecutionException(executor.submit(operationBody))
                }
                is DaemonExecutionPolicyImpl -> {
                    operation.executeInDaemon(executionPolicy, logger)
                }
                else -> {
                    error("Unknown execution policy: $executionPolicy")
                }
            }
        }

        private fun getCurrentClasspath() =
            (KotlinToolchainsImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }


        private fun <R> BuildOperationImpl<R>.executeInDaemon(executionPolicy: DaemonExecutionPolicyImpl, logger: KotlinLogger?): R {
            val kotlinLogger = logger ?: DefaultKotlinLogger
            val loggerAdapter = KotlinLoggerMessageCollectorAdapter(
                kotlinLogger,
                DefaultCompilerMessageRenderer,
                false
            )
            kotlinLogger.debug("Compiling using the daemon strategy")
            val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())
            val sessionIsAliveFlagFile = executionPolicy.buildIdToSessionFlagFile.computeIfAbsent(projectId) {
                createSessionIsAliveFlagFile()
            }
            val daemonLogOptions = executionPolicy.toDaemonLogOptions()
            val (daemonOptions = first, additionalJvmArguments = second) = executionPolicy.toDaemonOptions()
            val jvmOptions = executionPolicy.toDaemonJvmOptions(additionalJvmArguments)
            (
                val daemon = compileService, val sessionId,
            ) =
                KotlinCompilerRunnerUtils.newDaemonConnection(
                    compilerId,
                    clientIsAliveFile,
                    sessionIsAliveFlagFile,
                    loggerAdapter,
                    loggerAdapter.kotlinLogger.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")
                        ?.toBooleanStrictOrNull() ?: true,
                    daemonJVMOptions = jvmOptions,
                    daemonOptions = daemonOptions,
                    daemonLogOptions = daemonLogOptions,
                ) ?: error(ExitCode.INTERNAL_ERROR.asCompilationResult)
//            onCancel {
//                daemon.cancelCompilation(sessionId, compilationId)
//            }

//            val arguments = createAndPrepareCompilerArguments()
//            arguments.addSources()
//            logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))
//
//            val rootProjectDir = getRootProjectDir()
//            val daemonCompileOptions = toDaemonCompilationOptions(loggerAdapter.kotlinLogger.isDebugEnabled, arguments)
//            loggerAdapter.kotlinLogger.info("Options for KOTLIN DAEMON: $daemonCompileOptions")
//
//            val metricsReporter = getMetricsReporter()
//            val exitCode = daemon.compile(
//                sessionId,
//                arguments.toArgumentStrings(allowArgFileInValues = false).toTypedArray(),
//                daemonCompileOptions,
//                BtaCompilerServicesWithResultsFacade(loggerAdapter, get(LOOKUP_TRACKER)),
//                DaemonCompilationResults(
//                    loggerAdapter.kotlinLogger, rootProjectDir?.toFile(), metricsReporter
//                ),
//                compilationId
//            ).get()

            if (loggerAdapter.kotlinLogger.isDebugEnabled) {
                daemon.getDaemonJVMOptions().takeIf { it.isGood }?.let { jvmOpts ->
                    loggerAdapter.kotlinLogger.debug("Kotlin compile daemon JVM options: ${jvmOpts.get().mappers.flatMap { it.toArgs("-") }}")
                }
            }

            val result = daemon.executeOperation(this)

            try {
                daemon.releaseCompileSession(sessionId)
            } catch (e: RemoteException) {
                loggerAdapter.kotlinLogger.warn("Unable to release compile session, maybe daemon is already down: $e")
            }
            return when (result) {
                is CompileService.CallResult.Good -> result.get()
                is CompileService.CallResult.Error -> throw result.cause ?: error("The operation failed without a cause")
                is CompileService.CallResult.Dying -> error("The daemon is dying")
                is CompileService.CallResult.Ok -> result.get()
            }
        }

        /**
         * Attempts to retrieve the result of the computation from the given `Future` instance.
         * If the computation threw an exception, unwraps and rethrows the underlying cause of the exception.
         */
        private fun <R> unwrapExecutionException(result: Future<R>): R {
            return try {
                result.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }

        override fun close() {
            clearJarCaches()
            if (executorDelegate.isInitialized()) {
                executor.shutdown()
            }
            val file = buildIdToSessionFlagFile.remove(projectId) ?: return
            file.delete()
        }
    }
}
