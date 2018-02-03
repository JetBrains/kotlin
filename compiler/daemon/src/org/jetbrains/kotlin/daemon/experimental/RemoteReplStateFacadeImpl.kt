/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompilerState
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.experimental.ReplStateFacade
import org.jetbrains.kotlin.daemon.common.experimental.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.rmi.server.UnicastRemoteObject

class RemoteReplStateFacadeServer(
    val _id: Int,
    val state: GenericReplCompilerState,
    port: Int = SOCKET_ANY_FREE_PORT
) : ReplStateFacade,
    UnicastRemoteObject(
        port,
        LoopbackNetworkInterface.clientLoopbackSocketFactoryRMI,
        LoopbackNetworkInterface.serverLoopbackSocketFactoryRMI
    ) {

    override val END_CONNECTION_MESSAGE = Server.EndConnectionMessage<RemoteReplStateFacadeServer>()

    suspend override fun processMessage(msg: Server.Message<*>, output: ByteWriteChannelWrapper): Server.State {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun attachClient(client: Socket) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getId(): Int = _id

    override fun getHistorySize(): Int = state.history.size

    override fun historyGet(index: Int): ILineId = state.history[index].id

    override fun historyReset(): List<ILineId> = state.history.reset().toList()

    override fun historyResetTo(id: ILineId): List<ILineId> = state.history.resetTo(id).toList()
}
