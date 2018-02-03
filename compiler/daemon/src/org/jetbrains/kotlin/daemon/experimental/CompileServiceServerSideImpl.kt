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
    val socketHost: String,
    val socketPort: Int,
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
    onShutdown
) {

    override fun unexportSelf(force: Boolean): Boolean {
        // TODO: мб и не надо
        return true
    }

    override fun bindToNewSocket() {
        runBlocking {
            aSocket().tcp().bind(InetSocketAddress(socketHost, socketPort)).use { socket ->
                serverSocket = socket
                while (true) {
                    serverSocket.accept().use { attachClient(it) }
                }
            }
        }
    }

    lateinit var serverSocket: ServerSocket

    // Server methods :
    suspend override fun processMessage(msg: Message<*>, output: ByteWriteChannelWrapper): State {
        msg as Message<CompileServiceServerSide>
        return if (msg is Server.EndConnectionMessage)
            State.CLOSED
        else
            State.WORKING.also { msg.process(this, output) }
    }

    override suspend fun attachClient(client: Socket) {
        async {
            val (input, output) = client.openIO()
            while (processMessage(input.nextObject() as Message<*>, output) != State.CLOSED);
        }
    }

    override val END_CONNECTION_MESSAGE: Server.EndConnectionMessage<CompileServiceServerSide> by lazy {
        Server.EndConnectionMessage<CompileServiceServerSide>()
    }

    init {
        initialize()
    }

}