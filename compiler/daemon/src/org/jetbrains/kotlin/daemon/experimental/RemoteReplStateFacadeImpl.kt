/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompilerState
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.IOPair
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openIO
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
    id: Int,
    state: GenericReplCompilerState,
    port: Int = SOCKET_ANY_FREE_PORT
) : ReplStateFacadeServerSide, RemoteReplStateFacadeImpl(
    id,
    state,
    port
) {
    suspend override fun processMessage(msg: Server.AnyMessage, output: ByteWriteChannelWrapper) = when (msg) {
        is Server.EndConnectionMessage -> Server.State.CLOSED
        is Server.Message<*> -> Server.State.WORKING
            .also { (msg as Server.Message<ReplStateFacadeServerSide>).process(this, output) }
        else -> Server.State.ERROR
    }

    suspend override fun attachClient(client: Socket) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class RemoteReplStateFacadeClientSideImpl: ReplStateFacadeClientSide {

    lateinit var socket: Socket

    val io: IOPair

    init {
        io = socket.openIO()
    }

    override fun getId(): Int = runBlocking {
        io.output.writeObject(ReplStateFacadeServerSide.GetIdMessage())
        io.input.nextObject() as Int
    }

    override fun getHistorySize(): Int = runBlocking {
        io.output.writeObject(ReplStateFacadeServerSide.GetHistorySizeMessage())
        io.input.nextObject() as Int
    }

    override fun historyGet(index: Int): ILineId = runBlocking {
        io.output.writeObject(ReplStateFacadeServerSide.HistoryGetMessage(index))
        io.input.nextObject() as ILineId
    }

    override fun historyReset(): List<ILineId> = runBlocking {
        io.output.writeObject(ReplStateFacadeServerSide.HistoryResetMessage())
        io.input.nextObject() as List<ILineId>
    }

    override fun historyResetTo(id: ILineId): List<ILineId> = runBlocking {
        io.output.writeObject(ReplStateFacadeServerSide.HistoryResetToMessage(id))
        io.input.nextObject() as List<ILineId>
    }

    override fun attachToServer(socket: Socket) {
        this.socket = socket
    }

}
