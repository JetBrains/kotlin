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
        forceRecreate: Boolean = false,
    ): CompileServiceSession? {
        val daemonConnectionFactory = { policyImpl: DaemonExecutionPolicyImpl ->
            policyImpl.createDaemonConnection(loggerAdapter, sessionIsAliveFlagFile)
        }
        return if (!forceRecreate) {
            @Suppress("UNCHECKED_CAST") // to support the case when computeIfAbsent returns null and the mapping is not recorded
            (connections as MutableMap<DaemonExecutionPolicyImpl, CompileServiceSession?>).computeIfAbsent(
                policy, daemonConnectionFactory
            )?.let { daemon ->
                if (clientIsAliveFile.absolutePath in (daemon.compileService.getClients().takeIf { it.isGood }?.get() ?: emptyList())) {
                    daemon
                } else {
                    getCompileServiceSession(policy, loggerAdapter, forceRecreate = true)
                }
            }
        } else {
            @Suppress("UNCHECKED_CAST") // to support the case when computeIfAbsent returns null and the mapping is not recorded
            (connections as MutableMap<DaemonExecutionPolicyImpl, CompileServiceSession?>).compute(policy) { policy, _ ->
                daemonConnectionFactory(policy)
            }
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
