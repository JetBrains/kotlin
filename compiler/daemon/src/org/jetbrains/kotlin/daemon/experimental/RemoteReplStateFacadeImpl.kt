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
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompilerState
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.experimental.ReplStateFacadeServerSide
import org.jetbrains.kotlin.daemon.common.experimental.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openIO
import java.net.InetAddress
import java.rmi.server.UnicastRemoteObject

open class RemoteReplStateFacadeImpl(
    val _id: Int,
    val state: GenericReplCompilerState,
    port: Int = SOCKET_ANY_FREE_PORT
) : ReplStateFacade,
    UnicastRemoteObject(
        port,
        LoopbackNetworkInterface.clientLoopbackSocketFactoryRMI,
        LoopbackNetworkInterface.serverLoopbackSocketFactoryRMI
    ) {

    override fun getId(): Int = _id

    override fun getHistorySize(): Int = state.history.size

    override fun historyGet(index: Int): ILineId = state.history[index].id

    override fun historyReset(): List<ILineId> = state.history.reset().toList()

    override fun historyResetTo(id: ILineId): List<ILineId> = state.history.resetTo(id).toList()
}

@Suppress("UNCHECKED_CAST")
class RemoteReplStateFacadeServerSideImpl(
    val _id: Int,
    val state: GenericReplCompilerState,
    port: Int = SOCKET_ANY_FREE_PORT
) : ReplStateFacadeServerSide {

    override suspend fun getId(): Int = _id

    override suspend fun getHistorySize(): Int = state.history.size

    override suspend fun historyGet(index: Int): ILineId = state.history[index].id

    override suspend fun historyReset(): List<ILineId> = state.history.reset().toList()

    override suspend fun historyResetTo(id: ILineId): List<ILineId> = state.history.resetTo(id).toList()

    override suspend fun processMessage(msg: Server.AnyMessage, output: ByteWriteChannelWrapper) = when (msg) {
        is Server.EndConnectionMessage -> Server.State.CLOSED
        is Server.Message<*> -> Server.State.WORKING
            .also { (msg as Server.Message<ReplStateFacadeServerSide>).process(this, output) }
        else -> Server.State.ERROR
    }

    override suspend fun attachClient(client: Socket) {
        async {
            val (input, output) = client.openIO()
            while (processMessage(input.nextObject() as Server.Message<*>, output) != Server.State.CLOSED);
        }
    }

    init {
        runBlocking {
            aSocket().tcp().bind(InetAddress()).use {
                while (true) {
                    attachClient(it.accept())
                }
            }
        }
    }
}
