/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.experimental.CompilerId
import org.jetbrains.kotlin.daemon.common.experimental.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.experimental.DaemonOptions
import org.jetbrains.kotlin.daemon.common.experimental.CompileService
import java.rmi.NoSuchObjectException
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.util.*

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

    override fun bindToSocket() {
        try {
            // cleanup for the case of incorrect restart and many other situations
            unexportSelf(false)
        } catch (e: NoSuchObjectException) {
            // ignoring if object already exported
        }

        val stub = UnicastRemoteObject.exportObject(
            this,
            port,
            LoopbackNetworkInterface.clientLoopbackSocketFactoryRMI,
            LoopbackNetworkInterface.serverLoopbackSocketFactoryRMI
        ) as CompileService
        registry.rebind(org.jetbrains.kotlin.daemon.common.COMPILER_SERVICE_RMI_NAME, stub)
    }

    init {
        initialize()
    }
}
