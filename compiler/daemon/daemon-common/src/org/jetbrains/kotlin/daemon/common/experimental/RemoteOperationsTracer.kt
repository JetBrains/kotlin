/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import java.rmi.Remote
import java.rmi.RemoteException


interface RemoteOperationsTracer : Remote {

    @Throws(RemoteException::class)
    fun before(id: String)

    @Throws(RemoteException::class)
    fun after(id: String)

}

interface RemoteOperationsTracerClientSide : RemoteOperationsTracer, Client

interface RemoteOperationsTracerServerSide : RemoteOperationsTracer, Server {
    // Query messages:

    class BeforeMessage(val id: String) : Message<RemoteOperationsTracerServerSide> {
        suspend override fun process(server: RemoteOperationsTracerServerSide, output: ByteWriteChannelWrapper) =
            server.before(id)
    }

    class AfterMessage(val id: String) : Message<RemoteOperationsTracerServerSide> {
        suspend override fun process(server: RemoteOperationsTracerServerSide, output: ByteWriteChannelWrapper) =
            server.after(id)
    }
}