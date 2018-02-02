/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.experimental.common

import org.jetbrains.kotlin.cli.common.repl.ILineId
import java.rmi.Remote
import java.rmi.RemoteException
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server.Message
import io.ktor.network.sockets.Socket

interface ReplStateFacade : Server, Remote {

    @Throws(RemoteException::class)
    fun getId(): Int

    @Throws(RemoteException::class)
    fun getHistorySize(): Int

    @Throws(RemoteException::class)
    fun historyGet(index: Int): ILineId

    @Throws(RemoteException::class)
    fun historyReset(): List<ILineId>

    @Throws(RemoteException::class)
    fun historyResetTo(id: ILineId): List<ILineId>

    // Query messages:
    class GetIdMessage : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, clientSocket: Socket) =
            server.send(clientSocket, server.getId())
    }

    class GetHistorySizeMessage : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, clientSocket: Socket) =
            server.send(clientSocket, server.getHistorySize())
    }

    class HistoryGetMessage(val index: Int) : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, clientSocket: Socket) =
            server.send(clientSocket, server.historyGet(index))
    }

    class HistoryResetMessage : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, clientSocket: Socket) =
            server.send(clientSocket, server.historyReset())
    }

    class HistoryResetToMessage(val id: ILineId) : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, clientSocket: Socket) =
            server.send(clientSocket, server.historyResetTo(id))
    }

}