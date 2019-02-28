/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.cli.common.repl.ILineId

class ReplStateFacadeAsyncWrapper(val rmiReplStateFacade: ReplStateFacade) : ReplStateFacadeAsync {

    override suspend fun getId() = rmiReplStateFacade.getId()

    override suspend fun getHistorySize() = rmiReplStateFacade.getHistorySize()

    override suspend fun historyGet(index: Int) = rmiReplStateFacade.historyGet(index)

    override suspend fun historyReset() = rmiReplStateFacade.historyReset()

    override suspend fun historyResetTo(id: ILineId) = rmiReplStateFacade.historyResetTo(id)

}

fun ReplStateFacade.toClient() = ReplStateFacadeAsyncWrapper(this)
fun CompileService.CallResult<ReplStateFacade>.toClient() = when (this) {
    is CompileService.CallResult.Good -> CompileService.CallResult.Good(this.result.toClient())
    is CompileService.CallResult.Dying -> this
    is CompileService.CallResult.Error -> this
    is CompileService.CallResult.Ok -> this
}