/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.jvm.CompilerIncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import java.io.File

/*
 * The API mirror exchanges file paths as strings, so that the API module does not have to commit to `java.io.File`.
 * `File.path` and `File(String)` round-trip a path unchanged, so the conversions below are lossless.
 */
internal class IncrementalCacheAdapter(private val cache: CompilerIncrementalCache) : IncrementalCache {
    override fun getObsoletePackageParts(): Collection<String> = cache.getObsoletePackageParts()

    override fun getObsoleteMultifileClasses(): Collection<String> = cache.getObsoleteMultifileClasses()

    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? =
        cache.getStableMultifileFacadeParts(facadeInternalName)

    override fun getPackagePartData(partInternalName: String): JvmPackagePartProto? =
        cache.getPackagePartData(partInternalName)?.let { JvmPackagePartProto(it.data, it.strings) }

    override fun getModuleMappingData(): ByteArray? = cache.getModuleMappingData()

    override fun getMetadata(fragmentName: String): Map<File, ByteArray> =
        cache.getMetadata(fragmentName).mapKeys { File(it.key) }

    override fun getClassFilePath(internalClassName: String): String = cache.getClassFilePath(internalClassName)

    override fun close() {
        cache.close()
    }
}
