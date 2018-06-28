/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.CompilationResultsAsync
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClient
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.Serializable

interface CompilationResultsServerSide : CompilationResultsAsync, Server<CompilationResultsServerSide> {
    class AddMessage(
        val compilationResultCategory: Int,
        val value: Serializable
    ) : Server.Message<CompilationResultsServerSide>() {
        override suspend fun processImpl(server: CompilationResultsServerSide, printObject: (Any?) -> Unit) {
            server.add(compilationResultCategory, value)
        }
    }
}

interface CompilationResultsClientSide : CompilationResultsAsync, Client<CompilationResultsServerSide>

class CompilationResultsClientSideImpl(val socketPort: Int) : CompilationResultsClientSide,
    Client<CompilationResultsServerSide> by DefaultClient(socketPort) {

    override val clientSide: CompilationResultsAsync
        get() = this

    override suspend fun add(compilationResultCategory: Int, value: Serializable) {
        sendMessage(CompilationResultsServerSide.AddMessage(compilationResultCategory, value))
    }

//    init {
//        runBlocking {
//            connectToServer()
//        }
//    }

}

enum class CompilationResultCategory(val code: Int) {
    IC_COMPILE_ITERATION(0)
}