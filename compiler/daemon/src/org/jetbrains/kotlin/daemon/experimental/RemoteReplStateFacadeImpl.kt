/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompilerState
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import java.rmi.server.UnicastRemoteObject

class RemoteReplStateFacadeServer(val _id: Int,
                                  val state: GenericReplCompilerState,
                                  port: Int = SOCKET_ANY_FREE_PORT
) : ReplStateFacade,
    UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
{
    override fun getId(): Int = _id

    override fun getHistorySize(): Int = state.history.size

    override fun historyGet(index: Int): ILineId = state.history[index].id

    override fun historyReset(): List<ILineId> = state.history.reset().toList()

    override fun historyResetTo(id: ILineId): List<ILineId> = state.history.resetTo(id).toList()
}
