/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClientRMIWrapper
import java.io.File
import java.io.Serializable

class IncrementalCompilerServicesFacadeAsyncWrapper(
    val rmiImpl: IncrementalCompilerServicesFacade
) : IncrementalCompilerServicesFacadeClientSide, Client by DefaultClientRMIWrapper() {

    override suspend fun hasAnnotationsFileUpdater() = runBlocking(Unconfined) {
        rmiImpl.hasAnnotationsFileUpdater()
    }

    override suspend fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) = runBlocking(Unconfined) {
        rmiImpl.updateAnnotations(outdatedClassesJvmNames)
    }

    override suspend fun revert() = runBlocking(Unconfined) {
        rmiImpl.revert()
    }

    override suspend fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) = runBlocking(Unconfined) {
        rmiImpl.registerChanges(timestamp, dirtyData)
    }

    override suspend fun unknownChanges(timestamp: Long) = runBlocking(Unconfined) {
        rmiImpl.unknownChanges(timestamp)
    }

    override suspend fun getChanges(artifact: File, sinceTS: Long) = runBlocking(Unconfined) {
        rmiImpl.getChanges(artifact, sinceTS)
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) = runBlocking(Unconfined) {
        rmiImpl.report(category, severity, message, attachment)
    }

}

fun IncrementalCompilerServicesFacade.toClient() = IncrementalCompilerServicesFacadeAsyncWrapper(this)
