/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.v2.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.js.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.native.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class KotlinToolchainImpl(
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap(),
    override val jvm: JvmPlatformToolchain = JvmPlatformToolchainImpl(buildIdToSessionFlagFile),
) : KotlinToolchain {

    override val js: JsPlatformToolchain get() = TODO("Not implemented yet. Only JVM compilation is supported for now.")
    override val native: NativePlatformToolchain get() = TODO("Not implemented yet. Only JVM compilation is supported for now.")
    override val wasm: WasmPlatformToolchain get() = TODO("Not implemented yet. Only JVM compilation is supported for now.")

    override fun createInProcessExecutionPolicy(): ExecutionPolicy = InProcessExecutionPolicy

    override fun createDaemonExecutionPolicy(daemonJvmArgs: List<String>): ExecutionPolicy = DaemonExecutionPolicy(daemonJvmArgs)

    override fun getCompilerVersion(): String = KotlinCompilerVersion.VERSION

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
        clearJarCaches()
        val file = buildIdToSessionFlagFile.remove(projectId) ?: return
        file.delete()
    }

    private fun clearJarCaches() {
        ZipHandler.clearFileAccessorCache()
        @OptIn(K1Deprecation::class)
        KotlinCoreEnvironment.applicationEnvironment?.apply {
            (jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
            (jrtFileSystem as? CoreJrtFileSystem)?.clearRoots()
            idleCleanup()
        }
    }
}