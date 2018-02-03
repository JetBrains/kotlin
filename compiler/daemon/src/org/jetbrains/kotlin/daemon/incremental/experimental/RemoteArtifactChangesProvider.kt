/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import java.io.File

class RemoteArtifactChangesProvider(private val servicesFacade: IncrementalCompilerServicesFacade) : ArtifactChangesProvider {
    override fun getChanges(artifact: File, sinceTS: Long): Iterable<DirtyData>? =
            servicesFacade.getChanges(artifact, sinceTS)?.map { it.toDirtyData() }
}
