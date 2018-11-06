/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.impls.CompilerServicesFacadeBase
import java.io.Serializable

class CompilerServicesFacadeBaseRMIWrapper(val clientSide: CompilerServicesFacadeBaseAsync) : CompilerServicesFacadeBase, Serializable {

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) = runBlocking {
        clientSide.report(category, severity, message, attachment)
    }

}

fun CompilerServicesFacadeBaseAsync.toRMI() =
    if (this is CompilerServicesFacadeBaseAsyncWrapper) this.rmiImpl
    else CompilerServicesFacadeBaseRMIWrapper(this)
