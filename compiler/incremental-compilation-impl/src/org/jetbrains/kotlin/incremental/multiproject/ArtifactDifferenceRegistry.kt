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

package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.ICReporter
import java.io.File

internal interface ArtifactDifferenceRegistry {
    operator fun get(artifact: File): Iterable<ArtifactDifference>?
    fun add(artifact: File, difference: ArtifactDifference)
    fun remove(artifact: File)
    fun flush(memoryCachesOnly: Boolean)
}

internal class ArtifactDifference(val buildTS: Long, val dirtyData: DirtyData)

internal interface ArtifactDifferenceRegistryProvider {
    fun <T> withRegistry(
            report: (String) -> Unit,
            fn: (ArtifactDifferenceRegistry) -> T
    ): T?

    fun <T> withRegistry(
            reporter: ICReporter,
            fn: (ArtifactDifferenceRegistry) -> T
    ): T? {
        return withRegistry({reporter.report {it}}, fn)
    }

    fun clean()
}
