/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.v2.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.js.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.native.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.v2.js.JsPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.v2.js.WasmPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.buildtools.internal.v2.native.NativePlatformToolchainImpl

class KotlinToolchainImpl(
    override val jvm: JvmPlatformToolchain = JvmPlatformToolchainImpl(),
    override val js: JsPlatformToolchain = JsPlatformToolchainImpl(),
    override val native: NativePlatformToolchain = NativePlatformToolchainImpl(),
    override val wasm: WasmPlatformToolchain = WasmPlatformToolchainImpl(),
) : KotlinToolchain {
    override fun createInProcessExecutionPolicy(): ExecutionPolicy = InProcessExecutionPolicy

    override fun createDaemonExecutionPolicy(daemonJvmArgs: List<String>): ExecutionPolicy = DaemonExecutionPolicy(daemonJvmArgs)

    override suspend fun <R> executeOperation(operation: BuildOperation<R>): R {
        return executeOperation(operation, logger = null)
    }

    override suspend fun <R> executeOperation(
        operation: BuildOperation<R>,
        executionMode: ExecutionPolicy,
        logger: KotlinLogger?,
    ): R {
        check(operation is BuildOperationImpl<R>) { "Unknown operation type: ${operation::class.qualifiedName}" }
        return operation.execute(executionMode, logger)
    }

    override fun finishBuild(projectId: ProjectId) {
        TODO("Not yet implemented")
    }
}