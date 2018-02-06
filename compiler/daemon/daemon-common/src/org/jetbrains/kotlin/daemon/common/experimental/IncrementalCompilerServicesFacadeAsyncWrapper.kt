/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import java.io.File
import java.io.Serializable

class IncrementalCompilerServicesFacadeAsyncWrapper(
    val rmiImpl: IncrementalCompilerServicesFacade
) : IncrementalCompilerServicesFacadeAsync {

    suspend override fun hasAnnotationsFileUpdater() = runBlocking {
        rmiImpl.hasAnnotationsFileUpdater()
    }

    suspend override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) = runBlocking {
        rmiImpl.updateAnnotations(outdatedClassesJvmNames)
    }

    suspend override fun revert() = runBlocking {
        rmiImpl.revert()
    }

    suspend override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) = runBlocking {
        rmiImpl.registerChanges(timestamp, dirtyData)
    }

    suspend override fun unknownChanges(timestamp: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun IncrementalCompilerServicesFacade.toWrapper() = IncrementalCompilerServicesFacadeAsyncWrapper(this)