/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

public interface KotlinToolchain {
    public val jvm: JvmPlatformToolchain
    public val js: JsPlatformToolchain
    public val native: NativePlatformToolchain
    public val wasm: WasmPlatformToolchain

    public fun createExecutionPolicy(): ExecutionPolicy

    // no @JvmOverloads on interfaces :(
    public suspend fun <R> executeOperation(
        operation: BuildOperation<R>,
    ): R

    public suspend fun <R> executeOperation(
        operation: BuildOperation<R>,
        executionMode: ExecutionPolicy = createExecutionPolicy(),
        logger: KotlinLogger? = null,
    ): R

    /**
     * This must be called at the end of the project build (i.e., all build operations scoped to the project are finished)
     * iff [projectId] is configured via [BuildOperation.PROJECT_ID]
     */
    public fun finishBuild(projectId: ProjectId)

    public companion object {
        @JvmStatic
        public fun loadImplementation(classLoader: ClassLoader): KotlinToolchain =
            loadImplementation(KotlinToolchain::class, classLoader)
    }
}

// bridge for Java consumers, perhaps located in a separate module
@JvmOverloads
public fun <R> KotlinToolchain.executeOperation(
    operation: BuildOperation<R>,
    executionMode: ExecutionPolicy = createExecutionPolicy(),
    logger: KotlinLogger? = null,
    executor: Executor? = null,
): CompletableFuture<R> {
    val dispatcher = executor?.asCoroutineDispatcher() ?: Dispatchers.Default
    // GlobalScope?
    return GlobalScope.future(dispatcher) {
        executeOperation(operation)
    }
}