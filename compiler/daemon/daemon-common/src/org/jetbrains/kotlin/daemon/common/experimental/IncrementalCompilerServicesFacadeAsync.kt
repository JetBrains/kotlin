/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import java.io.File
import java.io.Serializable


interface IncrementalCompilerServicesFacadeAsync : CompilerServicesFacadeBaseAsync {
    // AnnotationFileUpdater
    suspend fun hasAnnotationsFileUpdater(): Boolean

    suspend fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>)

    suspend fun revert()

    // ChangesRegistry
    suspend fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData)

    suspend fun unknownChanges(timestamp: Long)

    suspend fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>?

}