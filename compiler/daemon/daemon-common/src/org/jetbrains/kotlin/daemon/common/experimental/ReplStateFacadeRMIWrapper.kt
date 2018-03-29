/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import java.io.Serializable

class ReplStateFacadeRMIWrapper(val clientSide: ReplStateFacadeClientSide) : ReplStateFacade, Serializable {

    override fun getId() = runBlocking { clientSide.getId() }

    override fun getHistorySize() = runBlocking { clientSide.getHistorySize() }

    override fun historyGet(index: Int) = runBlocking { clientSide.historyGet(index) }

    override fun historyReset() = runBlocking { clientSide.historyReset() }

    override fun historyResetTo(id: ILineId) = runBlocking { clientSide.historyResetTo(id) }

    init {
        clientSide.connectToServer()
    }

}

fun ReplStateFacadeClientSide.toRMI() = ReplStateFacadeRMIWrapper(this)
fun CompileService.CallResult<ReplStateFacadeClientSide>.toRMI() = when (this) {
    is CompileService.CallResult.Good -> CompileService.CallResult.Good(this.result.toRMI())
    is CompileService.CallResult.Dying -> this
    is CompileService.CallResult.Error -> this
    is CompileService.CallResult.Ok -> this
}