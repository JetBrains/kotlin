/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacadeAsync
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacadeServerSide.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClient
import java.io.File
import java.io.Serializable

interface IncrementalCompilerServicesFacadeClientSide : IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseClientSide

class IncrementalCompilerServicesFacadeClientSideImpl(val serverPort: Int) :
    IncrementalCompilerServicesFacadeClientSide,
    Client<CompilerServicesFacadeBaseServerSide> by DefaultClient(serverPort) {

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        sendNoReplyMessage(CompilerServicesFacadeBaseServerSide.ReportMessage(category, severity, message, attachment))
    }

}
