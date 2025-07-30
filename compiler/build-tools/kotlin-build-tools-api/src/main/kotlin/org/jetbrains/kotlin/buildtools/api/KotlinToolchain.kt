/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.KotlinToolchain.Companion.loadImplementation
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.knative.NativePlatformToolchain

/**
 * The main entry point to the Build Tools API.
 *
 * Allows access to the target-specific toolchains for creating build operations.
 * Currently only the [jvm] toolchain is supported.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [loadImplementation].
 *
 * An example of the basic usage is:
 *  ```
 *   val toolchain = KotlinToolchain.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.jvm.createJvmCompilationOperation(listOf(Path("/path/foo.kt")), Path("/path/to/outputDirectory"))
 *   toolchain.executeOperation(operation)
 *  ```
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface KotlinToolchain {
    public val jvm: JvmPlatformToolchain
    public val js: JsPlatformToolchain
    public val native: NativePlatformToolchain
    public val wasm: WasmPlatformToolchain

    public fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess
    public fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon

    /**
     * Returns the version of the Kotlin compiler used to run compilation.
     *
     * @return A string representing the version of the Kotlin compiler, for example `2.3.0`.
     */
    public fun getCompilerVersion(): String

    /**
     * Create a new [Build] that can be used to execute multiple [BuildOperations][BuildOperation] while retaining certain caches.
     *
     * Remember to call [Build.close] when all build operations are finished.
     *
     * @param projectId an optional [ProjectId] to identify this build. If not specified, a random one will be generated.
     */
    public fun createBuild(projectId: ProjectId? = null): Build

    /**
     * Represents a project build that can execute (see [executeOperation])
     * multiple [BuildOperations][BuildOperation] while retaining certain caches.
     *
     * Remember to call [close] after all operations are finished and no more operations in this build are planned.
     */
    public interface Build : AutoCloseable {
        /**
         * Access to the [KotlinToolchain] that created this build.
         */
        public val kotlinToolchain: KotlinToolchain

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
            executionPolicy: ExecutionPolicy = kotlinToolchain.createInProcessExecutionPolicy(),
            logger: KotlinLogger? = null,
        ): R

        /**
         * This must be called at the end of the project build (that is when all build operations scoped to the project are finished).
         */
        public override fun close()
    }

    public companion object {
        /**
         * Create an instance of [KotlinToolchain] using the given [classLoader].
         *
         * Make sure that the classloader has access to a Build Tools API implementation,
         * which usually means that it has the Kotlin compiler and related dependencies in its classpath.
         *
         * @param classLoader a [ClassLoader] that contains exactly one implementation of KotlinToolchain
         */
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): KotlinToolchain =
            try {
                loadImplementation(KotlinToolchain::class, classLoader)
            } catch (_: IllegalStateException) {
                @Suppress("DEPRECATION")
                classLoader.loadClass("org.jetbrains.kotlin.buildtools.internal.compat.KotlinToolchainV1Adapter").constructors.first()
                    .newInstance(CompilationService.loadImplementation(classLoader)) as KotlinToolchain
            }
    }
}
