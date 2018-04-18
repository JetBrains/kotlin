/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.Serializable

interface CompilerServicesFacadeBaseServerSide : CompilerServicesFacadeBaseAsync, Server<CompilerServicesFacadeBaseServerSide> {

    class ReportMessage(
        val category: Int,
        val severity: Int,
        val message: String?,
        val attachment: Serializable?
    ) : Server.Message<CompilerServicesFacadeBaseServerSide>() {

        override suspend fun processImpl(server: CompilerServicesFacadeBaseServerSide, sendReply: (Any?) -> Unit) {
            log.info("reporting_-_-_-_-")
            server.report(category, severity, message, attachment)
        }

    }
}