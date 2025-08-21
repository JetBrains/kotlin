/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.ProjectId.Companion.RandomProjectUUID
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.knative.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmPlatformToolchainImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class KotlinToolchainImpl(
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap(),
    override val jvm: JvmPlatformToolchain = JvmPlatformToolchainImpl(buildIdToSessionFlagFile),
) : KotlinToolchain {

    override val js: JsPlatformToolchain get() = TODO("Not implemented yet. Only JVM compilation is supported for now.")
    override val native: NativePlatformToolchain get() = TODO("Not implemented yet. Only JVM compilation is supported for now.")
    override val wasm: WasmPlatformToolchain get() = TODO("Not implemented yet. Only JVM compilation is supported for now.")

    override fun createInProcessExecutionPolicy(): ExecutionPolicy.InProcess = InProcessExecutionPolicyImpl

    override fun createDaemonExecutionPolicy(): ExecutionPolicy.WithDaemon = DaemonExecutionPolicyImpl()

    override fun getCompilerVersion(): String = KotlinCompilerVersion.VERSION

    override fun createBuildSession(): KotlinToolchain.BuildSession {
        return BuildSessionImpl(this, RandomProjectUUID(), buildIdToSessionFlagFile)
    }

    private class BuildSessionImpl(
        override val kotlinToolchain: KotlinToolchain,
        override val projectId: ProjectId,
        private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    ) : KotlinToolchain.BuildSession {
        override fun <R> executeOperation(operation: BuildOperation<R>): R {
            return executeOperation(operation, logger = null)
        }

        override fun <R> executeOperation(
            operation: BuildOperation<R>,
            executionPolicy: ExecutionPolicy,
            logger: KotlinLogger?,
        ): R {
            check(operation is BuildOperationImpl<R>) { "Unknown operation type: ${operation::class.qualifiedName}" }
            return operation.execute(projectId, executionPolicy, logger)
        }

        override fun close() {
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
}