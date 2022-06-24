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

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.IncrementalFileToPathConverter
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import java.io.File


abstract class IncrementalCachesManager<PlatformCache : AbstractIncrementalCache<*>>(
    cachesRootDir: File,
    rootProjectDir: File?,
    protected val reporter: ICReporter,
    storeFullFqNamesInLookupCache: Boolean = false,
    trackChangesInLookupCache: Boolean = false
) {
    val pathConverter = IncrementalFileToPathConverter(rootProjectDir)
    private val caches = arrayListOf<BasicMapsOwner>()

    var isClosed = false
    var isSuccessfulyClosed = false

    @Synchronized
    protected fun <T : BasicMapsOwner> T.registerCache() {
        assert(!isClosed) { "Attempted to add new cache into closed storage." }
        caches.add(this)
    }

    private val inputSnapshotsCacheDir = File(cachesRootDir, "inputs").apply { mkdirs() }
    private val lookupCacheDir = File(cachesRootDir, "lookups").apply { mkdirs() }

    val inputsCache: InputsCache = InputsCache(inputSnapshotsCacheDir, reporter, pathConverter).apply { registerCache() }
    val lookupCache: LookupStorage =
        LookupStorage(lookupCacheDir, pathConverter, storeFullFqNamesInLookupCache, trackChangesInLookupCache).apply { registerCache() }
    abstract val platformCache: PlatformCache

    @Synchronized
    fun close(flush: Boolean = false): Boolean {
        if (isClosed) {
            return isSuccessfulyClosed
        }
        isSuccessfulyClosed = true
        for (cache in caches) {
            if (flush) {
                try {
                    cache.flush(false)
                } catch (e: Throwable) {
                    isSuccessfulyClosed = false
                    reporter.info { "Exception when flushing cache ${cache.javaClass}: $e" }
                }
            }

            try {
                cache.close()
            } catch (e: Throwable) {
                isSuccessfulyClosed = false
                reporter.info { "Exception when closing cache ${cache.javaClass}: $e" }
            }
        }

        isClosed = true
        return isSuccessfulyClosed
    }
}

class IncrementalJvmCachesManager(
    cacheDirectory: File,
    rootProjectDir: File?,
    outputDir: File,
    reporter: ICReporter,
    storeFullFqNamesInLookupCache: Boolean = false,
    trackChangesInLookupCache: Boolean = false
) : IncrementalCachesManager<IncrementalJvmCache>(
    cacheDirectory,
    rootProjectDir,
    reporter,
    storeFullFqNamesInLookupCache,
    trackChangesInLookupCache
) {
    private val jvmCacheDir = File(cacheDirectory, "jvm").apply { mkdirs() }
    override val platformCache = IncrementalJvmCache(jvmCacheDir, outputDir, pathConverter).apply { registerCache() }
}

class IncrementalJsCachesManager(
    cachesRootDir: File,
    rootProjectDir: File?,
    reporter: ICReporter,
    serializerProtocol: SerializerExtensionProtocol,
    storeFullFqNamesInLookupCache: Boolean
) : IncrementalCachesManager<IncrementalJsCache>(cachesRootDir, rootProjectDir, reporter, storeFullFqNamesInLookupCache) {
    private val jsCacheFile = File(cachesRootDir, "js").apply { mkdirs() }
    override val platformCache = IncrementalJsCache(jsCacheFile, pathConverter, serializerProtocol).apply { registerCache() }
}