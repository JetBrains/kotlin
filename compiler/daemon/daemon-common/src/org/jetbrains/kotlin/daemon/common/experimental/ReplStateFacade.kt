/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import java.rmi.Remote
import java.rmi.RemoteException

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
        suspend override fun process(server: ReplStateFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getId())
    }

    class GetHistorySizeMessage : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getHistorySize())
    }

    class HistoryGetMessage(val index: Int) : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.historyGet(index))
    }

    class HistoryResetMessage : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.historyReset())
    }

    class HistoryResetToMessage(val id: ILineId) : Message<ReplStateFacade> {
        suspend override fun process(server: ReplStateFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.historyResetTo(id))
    }

}