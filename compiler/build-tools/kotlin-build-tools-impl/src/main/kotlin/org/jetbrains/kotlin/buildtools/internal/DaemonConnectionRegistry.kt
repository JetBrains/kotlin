/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal class DaemonConnectionRegistry(private val sessionIsAliveFlagFile: Lazy<File>) : AutoCloseable {
    private val connections = ConcurrentHashMap<DaemonExecutionPolicyImpl, CompileServiceSession>()

    fun getCompileServiceSession(
        policy: DaemonExecutionPolicyImpl,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompileServiceSession? {
        @Suppress("UNCHECKED_CAST") // to support the case when computeIfAbsent returns null and the mapping is not recorded
        return (connections as MutableMap<DaemonExecutionPolicyImpl, CompileServiceSession?>).computeIfAbsent(policy) { policyImpl ->
            policyImpl.createDaemonConnection(loggerAdapter, sessionIsAliveFlagFile)
        }
    }

    override fun close() {
        connections.values.distinct().forEach {
            try {
                it.compileService.releaseCompileSession(it.sessionId)
            } catch (_: Exception) {}
        }
        connections.clear()
    }
}
