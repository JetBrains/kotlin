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

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.common.repl.IReplStageState
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import java.rmi.server.UnicastRemoteObject

class RemoteReplStateFacadeServer(
    val _id: Int,
    val state: IReplStageState<*>,
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
