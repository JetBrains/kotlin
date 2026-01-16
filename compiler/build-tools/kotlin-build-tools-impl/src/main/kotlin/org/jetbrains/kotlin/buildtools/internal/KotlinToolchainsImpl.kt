/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.cri.CriToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.incremental.clearJarCaches
import java.io.File
import java.nio.file.Path
import java.util.concurrent.*

internal class KotlinToolchainsImpl() : KotlinToolchains {
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap()
    val toolchains: ConcurrentHashMap<Class<*>, KotlinToolchains.Toolchain> = ConcurrentHashMap()

    override fun <T : KotlinToolchains.Toolchain> getToolchain(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return toolchains.computeIfAbsent(type) { type ->
            when (type) {
                JvmPlatformToolchain::class.java -> JvmPlatformToolchainImpl(buildIdToSessionFlagFile)
                CriToolchain::class.java -> CriToolchainImpl()
                else -> error("Unsupported platform toolchain type: $type. Only JVM compilation is supported for now.")
            }
        } as T
    }

    override fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess = InProcessExecutionPolicyImpl

    @Deprecated(
        "Use jvmCompilationOperationBuilder instead",
        replaceWith = ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)")
    )
    override fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon = DaemonExecutionPolicyImpl()

    override fun daemonExecutionPolicyBuilder(): ExecutionPolicy.WithDaemon.Builder = DaemonExecutionPolicyImpl()

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
            return if (executionPolicy is ExecutionPolicy.InProcess) {
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
            if (executorDelegate.isInitialized()) {
                executor.shutdown()
            }
            val file = buildIdToSessionFlagFile.remove(projectId) ?: return
            file.delete()
        }
    }
}


internal fun Path.absolutePathStringOrThrow(): String = toFile().absolutePath
