/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonToolArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.classloading.LruClassLoadersCache
import org.jetbrains.kotlin.buildtools.internal.cri.CriToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.js.JsPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.KotlinMetadataPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.BtaSerializable
import org.jetbrains.kotlin.buildtools.internal.serializability.Messages
import org.jetbrains.kotlin.buildtools.internal.serializability.btaSerializersModule
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmPlatformToolchainImpl
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.daemon.common.DaemonCallbackChannel
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.*
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

private const val DEFAULT_CLASSLOADERS_CACHE_SIZE = 10
private const val PROPERTY_CLASSLOADERS_CACHE_SIZE = "kotlin.buildtools.classloaders.cache.size"

internal class KotlinToolchainsImpl() : KotlinToolchains {
    val toolchains: ConcurrentHashMap<Class<*>, KotlinToolchains.Toolchain> = ConcurrentHashMap()
    val classloadersCache = LruClassLoadersCache(
        System.getProperty(PROPERTY_CLASSLOADERS_CACHE_SIZE)?.toIntOrNull() ?: DEFAULT_CLASSLOADERS_CACHE_SIZE,
        this::class.java.classLoader
    )

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
    fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon = DaemonExecutionPolicyImpl()

    override fun daemonExecutionPolicyBuilder(): ExecutionPolicy.WithDaemon.Builder = DaemonExecutionPolicyImpl()

    override fun getCompilerVersion(): String = KotlinCompilerVersion.VERSION

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionImpl(this, RandomProjectUUID(), classloadersCache)
    }

    @OptIn(ExperimentalAtomicApi::class)
    private class BuildSessionImpl(
        override val kotlinToolchains: KotlinToolchainsImpl,
        override val projectId: ProjectId,
        val classloadersCache: LruClassLoadersCache,
    ) : KotlinToolchains.BuildSession {
        private val lastOperationId = AtomicInt(0)
        private val sessionIsAliveFlagFile = lazy { createSessionIsAliveFlagFile() }
        private val executorDelegate = lazy {
            Executors.newCachedThreadPool()
        }
        private val executor by executorDelegate
        private val daemonConnectionRegistry = DaemonConnectionRegistry(sessionIsAliveFlagFile)

        /**
         * Pins the shared application environment to this session so it is reused across build operations and
         * disposed when the session ends (see [close]).
         *
         * Initialized lazily on the first in-process operation that uses the environment (see
         * [BuildOperationImpl.usesApplicationEnvironment]).
         */
        private val applicationEnvironmentPin: Lazy<AutoCloseable> = lazy {
            ApplicationEnvironmentPinProvider.create()
        }

        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return executeOperation(operation, logger = null)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun <R> executeOperation(
            operation: BuildOperation<R>,
            executionPolicy: ExecutionPolicy,
            logger: KotlinLogger?,
        ): R {
            check(operation is BuildOperationImpl<R>) { "Unknown operation type: ${operation::class.qualifiedName}" }
            val operationBody: Callable<R> = {
                val classloadersCacheWithLogger =
                    classloadersCache.takeIf { operation[BuildOperationImpl.ENABLE_CLASSLOADER_CACHE] }?.withLogger(logger)
                operation.execute(
                    projectId,
                    executionPolicy,
                    logger,
                    ExecutionContext(classloadersCacheWithLogger, daemonConnectionRegistry)
                )
            }
            return if (executionPolicy is ExecutionPolicy.InProcess) {
                // For an operation that uses the shared application environment, pin it just before, so that it is kept
                // alive for reuse by subsequent operations and only disposed when the session ends.
                if (operation.usesApplicationEnvironment) {
                    val _ = applicationEnvironmentPin.value
                }
                unwrapExecutionException(executor.submit(operationBody))
            } else {
                executionPolicy as DaemonExecutionPolicyImpl
                if (operation is BtaSerializable) {
                    val operationId = lastOperationId.incrementAndFetch()
                    val trackers: List<MessageVisitor> = operation.prepareForSerialization(operationId)
                    val messageRenderer =
                        if (operation is BaseCompilationOperationImpl<*, *>) operation[BaseCompilationOperationImpl.COMPILER_MESSAGE_RENDERER] else DefaultCompilerMessageRenderer
                    val warningsAsError =
                        operation is BaseCompilationOperationImpl<*, *> && operation.compilerArguments[CommonToolArgumentsImpl.WERROR]

                    val loggerAdapter = KotlinLoggerMessageCollectorAdapter(logger ?: DefaultKotlinLogger, messageRenderer, warningsAsError)
                    val daemon = daemonConnectionRegistry.getCompileServiceSession(executionPolicy, loggerAdapter)
                        ?: error("Unable to get daemon connection")
                    val protobuf = ProtoBuf {
                        serializersModule = btaSerializersModule
                    }
                    val serializedOperation = protobuf.encodeToByteArray(operation as BtaSerializable)
                    val callbackChannel = BtaCallbackChannel(protobuf)
                    val result = daemon.compileService.execute(serializedOperation, operationId, callbackChannel).get()
                    @Suppress("UNCHECKED_CAST")
                    protobuf.decodeFromByteArray(operation.getResultSerializer(), result) as R
                } else {
                    operationBody.call()
                }
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
            if (applicationEnvironmentPin.isInitialized()) {
                applicationEnvironmentPin.value.close()
            }
            if (executorDelegate.isInitialized()) {
                executor.shutdown()
            }
            if (sessionIsAliveFlagFile.isInitialized()) {
                sessionIsAliveFlagFile.value.delete()
            }
            daemonConnectionRegistry.close()
        }
    }

    companion object {
        internal fun getBtaApiVersion(): BtaApiVersion = try {
            BtaApiVersion.Exact(KotlinToolingVersion(KotlinToolchains.getVersion()))
        } catch (_: NoSuchMethodError) {
            BtaApiVersion.Before2_4_20
        }
    }
}

internal sealed interface BtaApiVersion {
    object Before2_4_20 : BtaApiVersion
    class Exact(val version: KotlinToolingVersion) : BtaApiVersion
}

internal class ExecutionContext(
    val classloadersCache: LruClassLoadersCache?,
    val daemonConnectionRegistry: DaemonConnectionRegistry,
)


@OptIn(ExperimentalSerializationApi::class)
internal class BtaCallbackChannel(
    val protoBuf: ProtoBuf,
    port: Int = SOCKET_ANY_FREE_PORT,
) : DaemonCallbackChannel,
    UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory) {
    override fun report(message: ByteArray) {
        println(protoBuf.decodeFromByteArray<Messages>(message))
    }
}
