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

import com.google.common.io.Closer
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.IncrementalFileToPathConverter
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import java.io.Closeable
import java.io.File

abstract class IncrementalCachesManager<PlatformCache : AbstractIncrementalCache<*>>(
    cachesRootDir: File,
    rootProjectDir: File?,
    protected val reporter: ICReporter,
    transaction: CompilationTransaction,
    storeFullFqNamesInLookupCache: Boolean = false,
    trackChangesInLookupCache: Boolean = false,
    protected val keepChangesInMemory: Boolean = false,
) : Closeable {
    val pathConverter = IncrementalFileToPathConverter(rootProjectDir)
    private val caches = arrayListOf<BasicMapsOwner>()

    private var isClosed = false

    @Synchronized
    protected fun <T : BasicMapsOwner> T.registerCache() {
        check(!isClosed) { "This cache storage has already been closed" }
        caches.add(this)
    }

    private val inputSnapshotsCacheDir = File(cachesRootDir, "inputs").apply { mkdirs() }
    private val lookupCacheDir = File(cachesRootDir, "lookups").apply { mkdirs() }

    val inputsCache: InputsCache = InputsCache(inputSnapshotsCacheDir, reporter, pathConverter, keepChangesInMemory).apply { registerCache() }
    val lookupCache: LookupStorage =
        LookupStorage(
            lookupCacheDir,
            pathConverter,
            storeFullFqNamesInLookupCache,
            trackChangesInLookupCache,
            keepChangesInMemory,
            transaction,
        ).apply { registerCache() }
    abstract val platformCache: PlatformCache

    @Suppress("UnstableApiUsage")
    @Synchronized
    override fun close() {
        check(!isClosed) { "This cache storage has already been closed" }

        val closer = Closer.create()
        caches.forEach {
            closer.register(CacheCloser(it))
        }
        closer.close()

        isClosed = true
    }

    private class CacheCloser(private val cache: BasicMapsOwner) : Closeable {

        override fun close() {
            // It's important to flush the cache when closing (see KT-53168)
            cache.flush(memoryCachesOnly = false)
            cache.close()
        }
    }

}

class IncrementalJvmCachesManager(
    cacheDirectory: File,
    rootProjectDir: File?,
    outputDir: File,
    reporter: ICReporter,
    storeFullFqNamesInLookupCache: Boolean = false,
    trackChangesInLookupCache: Boolean = false,
    keepChangesInMemory: Boolean = false,
    transaction: CompilationTransaction = DummyCompilationTransaction(),
) : IncrementalCachesManager<IncrementalJvmCache>(
    cacheDirectory,
    rootProjectDir,
    reporter,
    transaction,
    storeFullFqNamesInLookupCache,
    trackChangesInLookupCache,
    keepChangesInMemory = keepChangesInMemory,
) {
    private val jvmCacheDir = File(cacheDirectory, "jvm").apply { mkdirs() }
    override val platformCache = IncrementalJvmCache(jvmCacheDir, outputDir, pathConverter, keepChangesInMemory).apply { registerCache() }
}

class IncrementalJsCachesManager(
    cachesRootDir: File,
    rootProjectDir: File?,
    reporter: ICReporter,
    serializerProtocol: SerializerExtensionProtocol,
    storeFullFqNamesInLookupCache: Boolean,
    keepChangesInMemory: Boolean = false,
    transaction: CompilationTransaction = DummyCompilationTransaction(),
) : IncrementalCachesManager<IncrementalJsCache>(
    cachesRootDir,
    rootProjectDir,
    reporter,
    transaction,
    storeFullFqNamesInLookupCache,
    keepChangesInMemory = keepChangesInMemory,
) {
    private val jsCacheFile = File(cachesRootDir, "js").apply { mkdirs() }
    override val platformCache = IncrementalJsCache(jsCacheFile, pathConverter, serializerProtocol, keepChangesInMemory, transaction).apply { registerCache() }
}