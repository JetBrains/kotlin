/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import java.io.Serializable

interface CompilationResultsAsync {
    suspend fun add(compilationResultCategory: Int, value: Serializable)
}

interface CompilationResultsServerSide : CompilationResultsAsync, Server<CompilationResultsServerSide> {
    val clientSide: CompilationResultsClientSide

    class AddMessage(
        val compilationResultCategory: Int,
        val value: Serializable
    ) : Server.Message<CompilationResultsServerSide> {
        override suspend fun process(server: CompilationResultsServerSide, output: ByteWriteChannelWrapper) {
            server.add(compilationResultCategory, value)
        }
    }
}

interface CompilationResultsClientSide : CompilationResultsAsync, Client<CompilationResultsServerSide>

class CompilationResultsClientSideImpl(val socketPort: Int) : CompilationResultsClientSide,
    Client<CompilationResultsServerSide> by DefaultClient(socketPort) {

    override suspend fun add(compilationResultCategory: Int, value: Serializable) {
        sendMessage(CompilationResultsServerSide.AddMessage(compilationResultCategory, value))
    }

    init {
        connectToServer()
    }

}

class CompilationResultsAsyncWrapper(val rmiImpl: CompilationResults) : CompilationResultsClientSide,
    Client<CompilationResultsServerSide> by DefaultClientRMIWrapper() {

    override suspend fun add(compilationResultCategory: Int, value: Serializable) {
        rmiImpl.add(compilationResultCategory, value)
    }

}

class CompilationResultsRMIWrapper(val clientSide: CompilationResultsClientSide) : CompilationResults, Serializable {

    override fun add(compilationResultCategory: Int, value: Serializable) = runBlocking {
        clientSide.add(compilationResultCategory, value)
    }

    init {
        clientSide.connectToServer()
    }

}

fun CompilationResults.toClient() =
    if (this is CompilationResultsRMIWrapper) this.clientSide
    else CompilationResultsAsyncWrapper(this)

fun CompilationResultsClientSide.toRMI() =
    if (this is CompilationResultsAsyncWrapper) this.rmiImpl
    else CompilationResultsRMIWrapper(this)

enum class CompilationResultCategory(val code: Int) {
    IC_COMPILE_ITERATION(0)
}