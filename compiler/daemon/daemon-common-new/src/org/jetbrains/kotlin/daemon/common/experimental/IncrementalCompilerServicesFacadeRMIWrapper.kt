/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.impls.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.impls.SimpleDirtyData
import java.io.File
import java.io.Serializable

class IncrementalCompilerServicesFacadeRMIWrapper(val clientSide: IncrementalCompilerServicesFacadeClientSide) :
    IncrementalCompilerServicesFacade, Serializable {

    override fun hasAnnotationsFileUpdater() = runBlocking {
        clientSide.hasAnnotationsFileUpdater()
    }

    override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) = runBlocking {
        clientSide.updateAnnotations(outdatedClassesJvmNames)
    }

    override fun revert() = runBlocking {
        clientSide.revert()
    }

    override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) = runBlocking {
        clientSide.registerChanges(timestamp, dirtyData)
    }

    override fun unknownChanges(timestamp: Long) = runBlocking {
        clientSide.unknownChanges(timestamp)
    }

    override fun getChanges(artifact: File, sinceTS: Long) = runBlocking {
        clientSide.getChanges(artifact, sinceTS)
    }

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) = runBlocking {
        clientSide.report(category, severity, message, attachment)
    }

    init {
//        runBlocking {
//            clientSide.connectToServer()
//        }
    }
}

fun IncrementalCompilerServicesFacadeClientSide.toRMI() =
    if (this is IncrementalCompilerServicesFacadeAsyncWrapper) this.rmiImpl
    else IncrementalCompilerServicesFacadeRMIWrapper(this)