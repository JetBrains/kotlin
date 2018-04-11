/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.File
import java.io.Serializable

open class BasicCompilerServicesWithResultsFacadeServerServerSide(
    val messageCollector: MessageCollector,
    val outputsCollector: ((File, List<File>) -> Unit)? = null,
    override val serverSocketWithPort: ServerSocketWrapper = findCallbackServerSocket()
) : CompilerServicesFacadeBaseServerSide {

    override val clients = hashMapOf<Socket, Server.ClientInfo>()

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        messageCollector.reportFromDaemon(outputsCollector, category, severity, message, attachment)
    }

    val clientSide : CompilerServicesFacadeBaseClientSide
        get() = CompilerServicesFacadeBaseClientSideImpl(serverSocketWithPort.port)
}