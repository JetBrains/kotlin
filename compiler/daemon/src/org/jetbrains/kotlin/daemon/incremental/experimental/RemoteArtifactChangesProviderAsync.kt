/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacadeAsync
import org.jetbrains.kotlin.daemon.incremental.toDirtyData
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import java.io.File

class RemoteArtifactChangesProviderAsync(private val servicesFacade: IncrementalCompilerServicesFacadeAsync) : ArtifactChangesProvider {
    override fun getChanges(artifact: File, sinceTS: Long): Iterable<DirtyData>? = runBlocking {
        servicesFacade.getChanges(artifact, sinceTS)?.map { it.toDirtyData() }
    }

    fun getChangesAsync(artifact: File, sinceTS: Long) = async {
        servicesFacade.getChanges(artifact, sinceTS)?.map { it.toDirtyData() }
    }
}
