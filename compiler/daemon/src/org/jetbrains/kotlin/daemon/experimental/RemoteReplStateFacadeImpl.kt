/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompilerState
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_FIND_PORT_ATTEMPTS
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*

@Suppress("UNCHECKED_CAST")
class RemoteReplStateFacadeServerSide(
    val _id: Int,
    val state: GenericReplCompilerState,
    override val serverSocketWithPort: ServerSocketWrapper = findPortForSocket(
        COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
        REPL_SERVER_PORTS_RANGE_START,
        REPL_SERVER_PORTS_RANGE_END
    )
) : ReplStateFacadeServerSide {

    override val clients = hashMapOf<Socket, Server.ClientInfo>()

    override suspend fun getId(): Int = _id

    override suspend fun getHistorySize(): Int = state.history.size

    override suspend fun historyGet(index: Int): ILineId = state.history[index].id

    override suspend fun historyReset(): List<ILineId> = state.history.reset().toList()

    override suspend fun historyResetTo(id: ILineId): List<ILineId> = state.history.resetTo(id).toList()

    val clientSide: RemoteReplStateFacadeClientSide
        get() = RemoteReplStateFacadeClientSide(serverSocketWithPort.port)

}


class RemoteReplStateFacadeClientSide(val serverPort: Int) : ReplStateFacadeClientSide, Client<ReplStateFacadeServerSide> by DefaultClient(serverPort) {

    override suspend fun getId(): Int {
        val id = sendMessage(ReplStateFacadeServerSide.GetIdMessage())
        return readMessage(id)
    }

    override suspend fun getHistorySize(): Int {
        val id = sendMessage(ReplStateFacadeServerSide.GetHistorySizeMessage())
        return readMessage(id)
    }

    override suspend fun historyGet(index: Int): ILineId {
        val id = sendMessage(ReplStateFacadeServerSide.HistoryGetMessage(index))
        return readMessage(id)
    }

    override suspend fun historyReset(): List<ILineId> {
        val id = sendMessage(ReplStateFacadeServerSide.HistoryResetMessage())
        return readMessage(id)
    }

    override suspend fun historyResetTo(id: ILineId): List<ILineId> {
        val id = sendMessage(ReplStateFacadeServerSide.HistoryResetToMessage(id))
        return readMessage(id)
    }

}


