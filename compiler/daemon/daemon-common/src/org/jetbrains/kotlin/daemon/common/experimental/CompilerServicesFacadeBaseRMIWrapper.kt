/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import java.io.Serializable

class CompilerServicesFacadeBaseRMIWrapper(val clientSide: CompilerServicesFacadeBaseClientSide) : CompilerServicesFacadeBase {

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) = runBlocking(Unconfined) {
        clientSide.report(category, severity, message, attachment)
    }

    init {
        clientSide.connectToServer()
    }
}

fun CompilerServicesFacadeBaseClientSide.toRMI() =
    if (this is CompilerServicesFacadeBaseAsyncWrapper) this.rmiImpl
    else CompilerServicesFacadeBaseRMIWrapper(this)
