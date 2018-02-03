/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException


interface CompilationResults : Remote {

    @Throws(RemoteException::class)
    fun add(compilationResultCategory: Int, value: Serializable)

}

enum class CompilationResultCategory(val code: Int) {
    IC_COMPILE_ITERATION(0)
}