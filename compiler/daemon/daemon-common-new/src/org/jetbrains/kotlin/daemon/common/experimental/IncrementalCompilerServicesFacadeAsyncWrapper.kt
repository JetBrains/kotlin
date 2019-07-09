/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClientRMIWrapper
import java.io.File
import java.io.Serializable

class IncrementalCompilerServicesFacadeAsyncWrapper(
    val rmiImpl: IncrementalCompilerServicesFacade
) : IncrementalCompilerServicesFacadeClientSide, Client<CompilerServicesFacadeBaseServerSide> by DefaultClientRMIWrapper() {

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) =
        rmiImpl.report(category, severity, message, attachment)

}

fun IncrementalCompilerServicesFacade.toClient() = IncrementalCompilerServicesFacadeAsyncWrapper(this)
