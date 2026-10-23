/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import org.jetbrains.kotlin.daemon.common.DaemonCallbackChannel
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import java.rmi.server.UnicastRemoteObject

internal class BtaCallbackChannel(
    val serializer: SerializationFormat,
    val messageVisitors: List<MessageVisitor>,
    port: Int = SOCKET_ANY_FREE_PORT,
) : DaemonCallbackChannel,
    UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory) {
    override fun report(message: SerializedDataContainer) {
        val message = serializer.deserialize<Message>(message)
        messageVisitors.forEach { it.accept(message) }
    }
}
