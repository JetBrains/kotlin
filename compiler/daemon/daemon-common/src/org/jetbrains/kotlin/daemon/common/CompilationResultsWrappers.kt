/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.impls.CompilationResults
import java.io.Serializable

class CompilationResultsAsyncWrapper(val rmiImpl: CompilationResults) : CompilationResultsAsync {

    override val clientSide: CompilationResultsAsync
        get() = this

    override suspend fun add(compilationResultCategory: Int, value: Serializable) {
        rmiImpl.add(compilationResultCategory, value)
    }

}

class CompilationResultsRMIWrapper(val clientSide: CompilationResultsAsync) : CompilationResults, Serializable {

    override fun add(compilationResultCategory: Int, value: Serializable) = runBlocking {
        clientSide.add(compilationResultCategory, value)
    }

//    init {
//        runBlocking {
//            clientSide.connectToServer()
//        }
//    }

}

fun CompilationResults.toClient() =
    if (this is CompilationResultsRMIWrapper) this.clientSide
    else CompilationResultsAsyncWrapper(this)

fun CompilationResultsAsync.toRMI() =
    if (this is CompilationResultsAsyncWrapper) this.rmiImpl
    else CompilationResultsRMIWrapper(this)
