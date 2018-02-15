/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import java.io.File
import java.io.Serializable

class IncrementalCompilerServicesFacadeAsyncWrapper(
    val rmiImpl: IncrementalCompilerServicesFacade
) : IncrementalCompilerServicesFacadeClientSide {

    override fun connectToServer() {} // already done by RMI

    override suspend fun hasAnnotationsFileUpdater() = runBlocking {
        rmiImpl.hasAnnotationsFileUpdater()
    }

    override suspend fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) = runBlocking {
        rmiImpl.updateAnnotations(outdatedClassesJvmNames)
    }

    override suspend fun revert() = runBlocking {
        rmiImpl.revert()
    }

    override suspend fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) = runBlocking {
        rmiImpl.registerChanges(timestamp, dirtyData)
    }

    override suspend fun unknownChanges(timestamp: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun IncrementalCompilerServicesFacade.toClient() = IncrementalCompilerServicesFacadeAsyncWrapper(this)
