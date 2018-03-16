/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import java.io.File
import java.io.Serializable

class IncrementalCompilerServicesFacadeRMIWrapper(val clientSide: IncrementalCompilerServicesFacadeClientSide) :
    IncrementalCompilerServicesFacade {

    override fun hasAnnotationsFileUpdater() = runBlocking(Unconfined) {
        clientSide.hasAnnotationsFileUpdater()
    }

    override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) = runBlocking(Unconfined) {
        clientSide.updateAnnotations(outdatedClassesJvmNames)
    }

    override fun revert() = runBlocking(Unconfined) {
        clientSide.revert()
    }

    override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) = runBlocking(Unconfined) {
        clientSide.registerChanges(timestamp, dirtyData)
    }

    override fun unknownChanges(timestamp: Long) = runBlocking(Unconfined) {
        clientSide.unknownChanges(timestamp)
    }

    override fun getChanges(artifact: File, sinceTS: Long) = runBlocking(Unconfined) {
        clientSide.getChanges(artifact, sinceTS)
    }

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) = runBlocking(Unconfined) {
        clientSide.report(category, severity, message, attachment)
    }

    init {
        clientSide.connectToServer()
    }
}

fun IncrementalCompilerServicesFacadeClientSide.toRMI() =
    if (this is IncrementalCompilerServicesFacadeAsyncWrapper) this.rmiImpl
    else IncrementalCompilerServicesFacadeRMIWrapper(this)