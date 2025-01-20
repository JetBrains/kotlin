/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

public interface KotlinToolchain {
    public val jvm: JvmPlatformToolchain
    public val js: JsPlatformToolchain
    public val native: NativePlatformToolchain
    public val wasm: WasmPlatformToolchain

    public fun makeExecutionPolicy(): ExecutionPolicy

    // no @JvmOverloads on interfaces :(
    public fun executeOperation(
        operation: BuildOperation,
    )

    public fun executeOperation(
        operation: BuildOperation,
        executionMode: ExecutionPolicy = makeExecutionPolicy(),
        logger: KotlinLogger? = null,
    )

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