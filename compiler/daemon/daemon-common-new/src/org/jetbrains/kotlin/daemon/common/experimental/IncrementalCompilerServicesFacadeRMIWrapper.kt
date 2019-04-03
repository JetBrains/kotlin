/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import java.io.Serializable

class IncrementalCompilerServicesFacadeRMIWrapper(val clientSide: IncrementalCompilerServicesFacadeClientSide) :
    IncrementalCompilerServicesFacade, Serializable {

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) = runBlocking {
        clientSide.report(category, severity, message, attachment)
    }

    // TODO: consider connecting to server right here in init-block
}

fun IncrementalCompilerServicesFacadeClientSide.toRMI() =
    if (this is IncrementalCompilerServicesFacadeAsyncWrapper) this.rmiImpl
    else IncrementalCompilerServicesFacadeRMIWrapper(this)