/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.ReplStateFacade

class ReplStateFacadeAsyncWrapper(val rmiReplStateFacade: ReplStateFacade): ReplStateFacadeAsync {

    suspend override fun getId() = runBlocking {
        rmiReplStateFacade.getId()
    }

    suspend override fun getHistorySize() = runBlocking {
        rmiReplStateFacade.getHistorySize()
    }

    suspend override fun historyGet(index: Int) = runBlocking {
        rmiReplStateFacade.historyGet(index)
    }

    suspend override fun historyReset() = runBlocking {
        rmiReplStateFacade.historyReset()
    }

    suspend override fun historyResetTo(id: ILineId) = runBlocking {
        rmiReplStateFacade.historyResetTo(id)
    }

}

fun ReplStateFacade.toWrapper() = ReplStateFacadeAsyncWrapper(this)
fun CompileService.CallResult<ReplStateFacade>.toWrapper()= when (this) {
    is CompileService.CallResult.Good -> CompileService.CallResult.Good(this.result.toWrapper())
    is CompileService.CallResult.Dying -> this
    is CompileService.CallResult.Error -> this
    is CompileService.CallResult.Ok -> this
}