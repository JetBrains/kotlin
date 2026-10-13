/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps.incremental

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import java.io.File

/**
 * The compiler doesn't read the members missing from [CompilerIncrementalCache] when the cache is managed by JPS.
 */
@OptIn(InternalBuildToolsApi::class)
internal class IncrementalCacheAdapter(private val cache: CompilerIncrementalCache) : IncrementalCache {
    override fun getObsoletePackageParts(): Collection<String> = cache.getObsoletePackageParts()

    override fun getObsoleteMultifileClasses(): Collection<String> = unsupported()

    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String> = unsupported()

    override fun getPackagePartData(partInternalName: String): JvmPackagePartProto = unsupported()

    override fun getModuleMappingData(): ByteArray? = cache.getModuleMappingData()

    override fun getMetadata(fragmentName: String): Map<File, ByteArray> = unsupported()

    override fun getClassFilePath(internalClassName: String): String = unsupported()

    // JPS owns the lifetime of its caches
    override fun close() {
    }

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("Not provided by the JPS incremental cache through the Build Tools API")
}
