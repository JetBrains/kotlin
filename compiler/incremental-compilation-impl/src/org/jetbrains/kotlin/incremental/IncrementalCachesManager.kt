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
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.IncrementalFileToPathConverter
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import java.io.File


abstract class IncrementalCachesManager<PlatformCache : AbstractIncrementalCache<*>>(
    cachesRootDir: File,
    rootProjectDir: File?,
    protected val reporter: ICReporter
) {
    val pathConverter = IncrementalFileToPathConverter(rootProjectDir)
    private val caches = arrayListOf<BasicMapsOwner>()
    protected fun <T : BasicMapsOwner> T.registerCache() {
        caches.add(this)
    }

    private val inputSnapshotsCacheDir = File(cachesRootDir, "inputs").apply { mkdirs() }
    private val lookupCacheDir = File(cachesRootDir, "lookups").apply { mkdirs() }

    val inputsCache: InputsCache = InputsCache(inputSnapshotsCacheDir, reporter).apply { registerCache() }
    val lookupCache: LookupStorage = LookupStorage(lookupCacheDir, pathConverter).apply { registerCache() }
    abstract val platformCache: PlatformCache

    fun close(flush: Boolean = false): Boolean {
        var successful = true

        for (cache in caches) {
            if (flush) {
                try {
                    cache.flush(false)
                } catch (e: Throwable) {
                    successful = false
                    reporter.report { "Exception when flushing cache ${cache.javaClass}: $e" }
                }
            }

            try {
                cache.close()
            } catch (e: Throwable) {
                successful = false
                reporter.report { "Exception when closing cache ${cache.javaClass}: $e" }
            }
        }

        return successful
    }
}

class IncrementalJvmCachesManager(
    cacheDirectory: File,
    rootProjectDir: File?,
    outputDir: File,
    reporter: ICReporter
) : IncrementalCachesManager<IncrementalJvmCache>(cacheDirectory, rootProjectDir, reporter) {
    private val jvmCacheDir = File(cacheDirectory, "jvm").apply { mkdirs() }
    override val platformCache = IncrementalJvmCache(jvmCacheDir, outputDir, pathConverter).apply { registerCache() }
}

class IncrementalJsCachesManager(
    cachesRootDir: File,
    rootProjectDir: File,
    reporter: ICReporter,
    serializerProtocol: SerializerExtensionProtocol
) : IncrementalCachesManager<IncrementalJsCache>(cachesRootDir, rootProjectDir, reporter) {
    private val jsCacheFile = File(cachesRootDir, "js").apply { mkdirs() }
    override val platformCache = IncrementalJsCache(jsCacheFile, pathConverter, serializerProtocol).apply { registerCache() }
}