/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompilerState
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_FIND_PORT_ATTEMPTS
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*

@Suppress("UNCHECKED_CAST")
class RemoteReplStateFacadeServerSide(
    val _id: Int,
    val state: GenericReplCompilerState,
    override val serverPort: Int = findPortForSocket(
        COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
        REPL_SERVER_PORTS_RANGE_START,
        REPL_SERVER_PORTS_RANGE_END
    )
) : ReplStateFacadeServerSide {

    override suspend fun getId(): Int = _id

    override suspend fun getHistorySize(): Int = state.history.size

    override suspend fun historyGet(index: Int): ILineId = state.history[index].id

    override suspend fun historyReset(): List<ILineId> = state.history.reset().toList()

    override suspend fun historyResetTo(id: ILineId): List<ILineId> = state.history.resetTo(id).toList()

    val clientSide: RemoteReplStateFacadeClientSide
        get() = RemoteReplStateFacadeClientSide(serverPort)

}


class RemoteReplStateFacadeClientSide(val serverPort: Int) : ReplStateFacadeClientSide, Client<ReplStateFacadeServerSide> by DefaultClient(serverPort) {

    override suspend fun getId(): Int {
        sendMessage(ReplStateFacadeServerSide.GetIdMessage()).await()
        return readMessage<Int>().await()
    }

    override suspend fun getHistorySize(): Int {
        sendMessage(ReplStateFacadeServerSide.GetHistorySizeMessage()).await()
        return readMessage<Int>().await()
    }

    override suspend fun historyGet(index: Int): ILineId {
        sendMessage(ReplStateFacadeServerSide.HistoryGetMessage(index)).await()
        return readMessage<ILineId>().await()
    }

    override suspend fun historyReset(): List<ILineId> {
        sendMessage(ReplStateFacadeServerSide.HistoryResetMessage()).await()
        return readMessage<List<ILineId>>().await()
    }

    override suspend fun historyResetTo(id: ILineId): List<ILineId> {
        sendMessage(ReplStateFacadeServerSide.HistoryResetToMessage(id)).await()
        return readMessage<List<ILineId>>().await()
    }

}


