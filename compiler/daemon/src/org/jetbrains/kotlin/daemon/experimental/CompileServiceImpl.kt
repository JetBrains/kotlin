/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.experimental.common.CompileService
import org.jetbrains.kotlin.daemon.experimental.common.CompilerId
import org.jetbrains.kotlin.daemon.experimental.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.experimental.common.DaemonOptions
import java.rmi.NoSuchObjectException
import java.rmi.RemoteException
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.util.*
import java.util.concurrent.TimeUnit

const val REMOTE_STREAM_BUFFER_SIZE = 4096

fun nowSeconds() = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())

interface CompilerSelector {
    operator fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*>
}

interface EventManager {
    fun onCompilationFinished(f: () -> Unit)
}

private class EventManagerImpl : EventManager {
    private val onCompilationFinished = arrayListOf<() -> Unit>()

    @Throws(RemoteException::class)
    override fun onCompilationFinished(f: () -> Unit) {
        onCompilationFinished.add(f)
    }

    fun fireCompilationFinished() {
        onCompilationFinished.forEach { it() }
    }
}

class CompileServiceImpl(
    val registry: Registry,
    compiler: CompilerSelector,
    compilerId: CompilerId,
    daemonOptions: DaemonOptions,
    daemonJVMOptions: DaemonJVMOptions,
    port: Int,
    timer: Timer,
    onShutdown: () -> Unit
) : AbstractCompileService(
    compiler,
    compilerId, daemonOptions,
    daemonJVMOptions,
    port,
    timer,
    onShutdown
) {
    override fun unexportSelf(force: Boolean) = UnicastRemoteObject.unexportObject(this, force)

    override fun bindToNewSocket() {
        try {
            // cleanup for the case of incorrect restart and many other situations
            unexportSelf(false)
        } catch (e: NoSuchObjectException) {
            // ignoring if object already exported
        }

        val stub = UnicastRemoteObject.exportObject(
            this,
            port,
            LoopbackNetworkInterface.clientLoopbackSocketFactory,
            LoopbackNetworkInterface.serverLoopbackSocketFactory
        ) as org.jetbrains.kotlin.daemon.common.CompileService
        registry.rebind(org.jetbrains.kotlin.daemon.common.COMPILER_SERVICE_RMI_NAME, stub)
    }

    init {
        initialize()
    }
}
