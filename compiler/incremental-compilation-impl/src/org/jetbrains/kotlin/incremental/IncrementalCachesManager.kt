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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.modules.TargetId
import java.io.File

class IncrementalCachesManager (
        private val targetId: TargetId,
        private val cacheDirectory: File,
        private val outputDir: File,
        private val reporter: ICReporter
) {
    private val incrementalCacheDir = File(cacheDirectory, "increCache.${targetId.name}")
    private val lookupCacheDir = File(cacheDirectory, "lookups")
    private var incrementalCacheField: GradleIncrementalCacheImpl? = null
    private var lookupCacheField: LookupStorage? = null

    val incrementalCache: GradleIncrementalCacheImpl
        get() {
            if (incrementalCacheField == null) {
                val targetDataRoot = incrementalCacheDir.apply { mkdirs() }
                incrementalCacheField = GradleIncrementalCacheImpl(targetDataRoot, outputDir, targetId, reporter)
            }

            return incrementalCacheField!!
        }

    val lookupCache: LookupStorage
        get() {
            if (lookupCacheField == null) {
                lookupCacheField = LookupStorage(lookupCacheDir.apply { mkdirs() })
            }

            return lookupCacheField!!
        }

    fun clean() {
        close(flush = false)
        cacheDirectory.deleteRecursively()
    }

    fun close(flush: Boolean = false) {
        incrementalCacheField?.let {
            if (flush) {
                it.flush(false)
            }
            it.close()
            incrementalCacheField = null
        }

        lookupCacheField?.let {
            if (flush) {
                it.flush(false)
            }
            it.close()
            lookupCacheField = null
        }
    }
}