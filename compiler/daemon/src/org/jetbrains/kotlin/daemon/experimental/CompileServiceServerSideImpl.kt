/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.CompileServiceServerSide
import org.jetbrains.kotlin.daemon.common.experimental.CompilerId
import org.jetbrains.kotlin.daemon.common.experimental.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.experimental.DaemonOptions
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.State
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openIO
import java.net.InetSocketAddress
import java.util.*

@Suppress("UNCHECKED_CAST")
class CompileServiceServerSideImpl(
    val socket: ServerSocket,
    compiler: CompilerSelector,
    compilerId: CompilerId,
    daemonOptions: DaemonOptions,
    daemonJVMOptions: DaemonJVMOptions,
    port: Int,
    timer: Timer,
    onShutdown: () -> Unit
) : CompileServiceServerSide, AbstractCompileService(
    compiler,
    compilerId, daemonOptions,
    daemonJVMOptions,
    port,
    timer,
    onShutdown,
    ::KotlinJvmReplServiceSockets
) {

    override fun unexportSelf(force: Boolean): Boolean {
        // TODO: it possibly has no sense
        return true
    }

    override fun bindToSocket() {
//        runBlocking {
//            aSocket().tcp().bind(InetSocketAddress(port)).use { socket ->
//                serverSocket = socket
//                while (true) {
//                    serverSocket.accept().use { attachClient(it) }
//                }
//            }
//        }
    }

    lateinit var serverSocket: ServerSocket

    // Server methods :
    override suspend fun processMessage(msg: Server.AnyMessage, output: ByteWriteChannelWrapper) = when (msg) {
        is Server.EndConnectionMessage -> State.CLOSED
        is Message<*> -> State.WORKING
            .also { (msg as Message<CompileServiceServerSide>).process(this, output) }
        else -> State.ERROR
    }

    override suspend fun attachClient(client: Socket) {
        async {
            val (input, output) = client.openIO()
            while (processMessage(input.nextObject() as Message<*>, output) != State.CLOSED);
        }
    }

    init {
        initialize()
    }

}