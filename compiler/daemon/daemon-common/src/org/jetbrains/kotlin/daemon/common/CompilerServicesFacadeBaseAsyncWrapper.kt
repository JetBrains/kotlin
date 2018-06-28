/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.daemon.common.impls.CompilerServicesFacadeBase
import java.io.Serializable


class CompilerServicesFacadeBaseAsyncWrapper(
    val rmiImpl: CompilerServicesFacadeBase
) : CompilerServicesFacadeBaseAsync {

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) =
        rmiImpl.report(category, severity, message, attachment)

}

fun CompilerServicesFacadeBase.toClient() =
    if (this is CompilerServicesFacadeBaseRMIWrapper) this.clientSide
    else CompilerServicesFacadeBaseAsyncWrapper(this)
