/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBaseAsync
import org.jetbrains.kotlin.daemon.common.impls.SimpleDirtyData
import java.io.File


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