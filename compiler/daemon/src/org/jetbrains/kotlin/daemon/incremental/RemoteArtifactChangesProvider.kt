/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.incremental

import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ArtifactChangesProvider
import java.io.File

class RemoteArtifactChangesProvider(private val servicesFacade: IncrementalCompilerServicesFacade) : ArtifactChangesProvider {
    override fun getChanges(artifact: File, sinceTS: Long): Iterable<DirtyData>? =
            servicesFacade.getChanges(artifact, sinceTS)?.map { it.toDirtyData() }
}
