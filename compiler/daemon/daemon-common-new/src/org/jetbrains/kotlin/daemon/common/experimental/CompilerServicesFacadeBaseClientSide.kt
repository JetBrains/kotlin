/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBaseAsync
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClient
import java.io.Serializable

interface CompilerServicesFacadeBaseClientSide : CompilerServicesFacadeBaseAsync, Client<CompilerServicesFacadeBaseServerSide>

class CompilerServicesFacadeBaseClientSideImpl(val serverPort: Int) :
    CompilerServicesFacadeBaseClientSide,
    Client<CompilerServicesFacadeBaseServerSide> by DefaultClient(serverPort) {

    init {
//        runBlocking { connectToServer() }
        log.info("CompilerServicesFacadeBaseClientSideImpl on $serverPort - inited")
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        log.info("client $serverPort - fun report")
        sendNoReplyMessage(
            CompilerServicesFacadeBaseServerSide.ReportMessage(
                category, severity, message, attachment
            )
        )
    }

}