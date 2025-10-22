/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains.Companion.loadImplementation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains.Toolchain
import org.jetbrains.kotlin.buildtools.api.internal.KotlinCompilerVersion
import org.jetbrains.kotlin.buildtools.api.internal.wrappers.PreKotlin220Wrapper
import org.jetbrains.kotlin.buildtools.api.internal.wrappers.PreKotlin230Wrapper
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The main entry point to the Build Tools API.
 *
 * Allows access to the target-specific toolchains for creating build operations.
 * Currently only the [JvmPlatformToolchain] is supported.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [loadImplementation].
 *
 * An example of the basic usage is:
 *  ```
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchains.jvm.createJvmCompilationOperation(listOf(Path("/path/foo.kt")), Path("/path/to/outputDirectory"))
 *   toolchain.createBuildSession().use { it.executeOperation(operation) }
 *  ```
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface KotlinToolchains {

    /**
     * Represents a toolchain that can be obtained from [KotlinToolchains.getToolchain] and used to create various build operations.
     *
     * [JvmPlatformToolchain] used for compiling Kotlin code for the JVM platform is an example of a `Toolchain`,
     * and other platform-specific toolchains will be available in the future.
     *
     * This interface is not intended to be implemented by the API consumers.
     */
    public interface Toolchain

    /**
     * Returns a [Toolchain] of the given [type], possibly creating it when accessed for the first time.
     *
     * The implementation may return the same instance of a `Toolchain` for subsequent calls.
     *
     * @see JvmPlatformToolchain
     */
    public fun <T : Toolchain> getToolchain(type: Class<T>): T

    /**
     * Creates an [ExecutionPolicy] that allows executing operations in-process.
     *
     * @see BuildSession.executeOperation
     */
    public fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess

    /**
     * Creates an [ExecutionPolicy] that allows executing operations using a Kotlin daemon.
     *
     * @see BuildSession.executeOperation
     */
    public fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon

    /**
     * Returns the version of the Kotlin compiler used to run compilation.
     *
     * @return A string representing the version of the Kotlin compiler, for example `2.3.0`.
     */
    public fun getCompilerVersion(): String

    /**
     * Create a new [BuildSession] that can be used to execute multiple [BuildOperations][BuildOperation] while retaining certain caches.
     *
     * Remember to call [BuildSession.close] when all build operations are finished.
     */
    public fun createBuildSession(): BuildSession

    /**
     * Represents a build session that can execute (see [executeOperation])
     * multiple [BuildOperations][BuildOperation] while retaining certain caches.
     *
     * Remember to call [close] after all operations are finished and no more operations in this session are planned.
     */
    public interface BuildSession : AutoCloseable {
        /**
         * Access to the [KotlinToolchains] that created this build.
         */
        public val kotlinToolchains: KotlinToolchains

        /**
         * The [ProjectId] that identifies this build.
         */
        public val projectId: ProjectId

        /**
         * Execute the given [operation] using [ExecutionPolicy.InProcess].
         *
         * @param operation the [BuildOperation] to execute.
         * Operations can be obtained from platform toolchains, e.g. [JvmPlatformToolchain.createJvmCompilationOperation]
         */
        public fun <R> executeOperation(
            operation: BuildOperation<R>,
        ): R

        /**
         * Execute the given [operation] using the given [executionPolicy].
         *
         * @param operation the [BuildOperation] to execute.
         * Operations can be obtained from platform toolchains, e.g. [JvmPlatformToolchain.createJvmCompilationOperation]
         * @param executionPolicy an [ExecutionPolicy] obtained from [createInProcessExecutionPolicy] or [createDaemonExecutionPolicy]
         * @param logger an optional [KotlinLogger]
         */
        public fun <R> executeOperation(
            operation: BuildOperation<R>,
            executionPolicy: ExecutionPolicy = kotlinToolchains.createInProcessExecutionPolicy(),
            logger: KotlinLogger? = null,
        ): R

        /**
         * This must be called at the end of the project build (that is when all build operations scoped to the project are finished).
         */
        public override fun close()
    }

    public companion object {
        /**
         * Create an instance of [KotlinToolchains] using the given [classLoader].
         *
         * Make sure that the classloader has access to a Build Tools API implementation,
         * which usually means that it has the Kotlin compiler and related dependencies in its classpath.
         *
         * @param classLoader a [ClassLoader] that contains exactly one implementation of [KotlinToolchains].
         * If executing operations using [ExecutionPolicy.WithDaemon], a [java.net.URLClassLoader] must be used here.
         */
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): KotlinToolchains = try {
            val baseImplementation = loadImplementation(KotlinToolchains::class, classLoader)
            val kotlinCompilerVersion = KotlinCompilerVersion(baseImplementation.getCompilerVersion())
            when {
                kotlinCompilerVersion <= KotlinCompilerVersion(2, 3, 0, null) -> {
                    PreKotlin230Wrapper(baseImplementation)
                }
                else -> baseImplementation
            }
        } catch (_: NoImplementationFoundException) {
            try {
                classLoader.loadClass("org.jetbrains.kotlin.buildtools.internal.compat.KotlinToolchainsV1Adapter")
                    .constructors.first()
                    .newInstance(@Suppress("DEPRECATION") CompilationService.loadImplementation(classLoader)) as KotlinToolchains
            } catch (e: ClassNotFoundException) {
                throw NoImplementationFoundException(KotlinToolchains::class).initCause(e)
            }
        }
    }
}

/**
 * Returns a [Toolchain] of the given type [T], possibly creating it when accessed for the first time.
 *
 * The implementation may return the same instance of a `Toolchain` for subsequent calls.
 *
 * @see JvmPlatformToolchain
 */
@ExperimentalBuildToolsApi
public inline fun <reified T : Toolchain> KotlinToolchains.getToolchain(): T {
    return getToolchain(T::class.java)
}


// coroutines helpers

public object KotlinToolchainsDispatcherProvider {
    public var dispatcher: CoroutineDispatcher = Dispatchers.IO
        set(value) {
            field = value
            executor = dispatcher.asExecutor()
        }

    internal var executor: Executor = dispatcher.asExecutor()
}

@ExperimentalBuildToolsApi
public suspend fun <R> KotlinToolchains.BuildSession.executeCancellable(
    operation: CancellableBuildOperation<R>,
    executionPolicy: ExecutionPolicy,
    logger: KotlinLogger?,
): R = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        operation.cancel()
    }
    CompletableFuture.runAsync(
        {
            try {
                val result = executeOperation(operation, executionPolicy, logger)
                continuation.resume(result)
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        },
        KotlinToolchainsDispatcherProvider.executor,
    )
}