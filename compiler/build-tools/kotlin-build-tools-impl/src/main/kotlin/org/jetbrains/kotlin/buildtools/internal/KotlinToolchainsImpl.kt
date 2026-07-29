/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.cri.CriToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.js.JsPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.KotlinMetadataPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmPlatformToolchainImpl
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.incremental.clearJarCaches
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.util.concurrent.*

internal class KotlinToolchainsImpl() : KotlinToolchains {
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
    fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon = DaemonExecutionPolicyImpl()

    override fun daemonExecutionPolicyBuilder(): ExecutionPolicy.WithDaemon.Builder = DaemonExecutionPolicyImpl()

    override fun getCompilerVersion(): String = KotlinCompilerVersion.VERSION

    override fun createBuildSession(): KotlinToolchains.BuildSession {
        return BuildSessionImpl(this, RandomProjectUUID())
    }

    private class BuildSessionImpl(
        override val kotlinToolchains: KotlinToolchains,
        override val projectId: ProjectId,
    ) : KotlinToolchains.BuildSession {
        private val sessionIsAliveFlagFile = lazy { createSessionIsAliveFlagFile() }
        private val executorDelegate = lazy {
            Executors.newCachedThreadPool()
        }
        private val executor by executorDelegate

        private val applicationEnvironmentPin: Disposable = Disposer.newDisposable("kotlin-dsl BTA application environment pin")

        /**
         * Pins the shared application environment to this session so it is reused across build operations and
         * disposed when the session ends (see [close]).
         *
         * Initialized lazily on the first in-process operation that uses the environment (see
         * [BuildOperationImpl.usesApplicationEnvironment]).
         */
        private val applicationEnvironmentInitialization = lazy {
            setupIdeaStandaloneExecution()
            @OptIn(CoreEnvironmentDeprecation::class)
            KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(
                applicationEnvironmentPin, CompilerConfiguration.create()
            )
        }

        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(
            operation: BuildOperation<R>,
            executionPolicy: ExecutionPolicy,
            logger: KotlinLogger?,
        ): R {
            check(operation is BuildOperationImpl<R>) { "Unknown operation type: ${operation::class.qualifiedName}" }
            val operationBody: Callable<R> = { operation.execute(projectId, executionPolicy, logger, sessionIsAliveFlagFile) }
            return if (executionPolicy is ExecutionPolicy.InProcess) {
                // For an operation that uses the shared application environment, pin it just before, so that it is kept
                // alive for reuse by subsequent operations and only disposed when the session ends.
                if (operation.usesApplicationEnvironment) {
                    applicationEnvironmentInitialization.value
                }
                unwrapExecutionException(executor.submit(operationBody))
            } else {
                operationBody.call()
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
            if (applicationEnvironmentInitialization.isInitialized()) {
                Disposer.dispose(applicationEnvironmentPin)
            }
            if (executorDelegate.isInitialized()) {
                executor.shutdown()
            }
            if (sessionIsAliveFlagFile.isInitialized()) {
                sessionIsAliveFlagFile.value.delete()
            }
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
