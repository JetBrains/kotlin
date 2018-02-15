/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openAndWrapWriteChannel
import java.beans.Transient
import java.io.Serializable
import java.net.InetSocketAddress

interface CompilerServicesFacadeBaseClientSide : CompilerServicesFacadeBaseAsync, Client

class CompilerServicesFacadeBaseClientSideImpl(val serverPort: Int) : CompilerServicesFacadeBaseClientSide {

    @Transient
    lateinit var output: ByteWriteChannelWrapper

    suspend override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        async {
            output.writeObject(
                CompilerServicesFacadeBaseServerSide.ReportMessage(
                    category, severity, message, attachment
                )
            )
        }
    }

    override fun connectToServer() {
        async {
            output = aSocket().tcp().connect(InetSocketAddress(serverPort)).openAndWrapWriteChannel()
        }
    }
}