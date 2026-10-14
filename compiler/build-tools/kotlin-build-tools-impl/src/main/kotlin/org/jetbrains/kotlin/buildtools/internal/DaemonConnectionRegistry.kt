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

        @Suppress("UNCHECKED_CAST") // to support the case when compute returns null and the mapping is not recorded
        val connectionsNullable = connections as MutableMap<DaemonExecutionPolicyImpl, CompileServiceSession?>

        return connectionsNullable.compute(policy) { policy, daemon: CompileServiceSession? ->
            val resultingDaemon = daemon.takeIfAlive() ?: policy.createDaemonConnection(loggerAdapter, sessionIsAliveFlagFile)
            resultingDaemon
        }
    }

    override fun close() {
        connections.values.distinct().forEach {
            try {
                it.compileService.releaseCompileSession(it.sessionId)
            } catch (_: Exception) {
            }
        }
        connections.clear()
    }
}

private fun CompileServiceSession?.takeIfAlive(): CompileServiceSession? {
    if (this == null) return null
    return try {
        this.takeIf { compileService.isSessionActive(sessionId).takeIf { it.isGood }?.get() ?: false }
    } catch (_: Exception) {
        null
    }
}
