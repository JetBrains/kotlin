/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import java.rmi.Remote
import java.rmi.RemoteException

interface ReplStateFacade : Remote {

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

}

interface ReplStateFacadeClientSide: ReplStateFacade, Client

interface ReplStateFacadeServerSide: ReplStateFacade, Server {
    // Query messages:
    class GetIdMessage : Message<ReplStateFacadeServerSide> {
        suspend override fun process(server: ReplStateFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getId())
    }

    class GetHistorySizeMessage : Message<ReplStateFacadeServerSide> {
        suspend override fun process(server: ReplStateFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getHistorySize())
    }

    class HistoryGetMessage(val index: Int) : Message<ReplStateFacadeServerSide> {
        suspend override fun process(server: ReplStateFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.historyGet(index))
    }

    class HistoryResetMessage : Message<ReplStateFacadeServerSide> {
        suspend override fun process(server: ReplStateFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.historyReset())
    }

    class HistoryResetToMessage(val id: ILineId) : Message<ReplStateFacadeServerSide> {
        suspend override fun process(server: ReplStateFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.historyResetTo(id))
    }
}
