/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.konan.NativeCache
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeMonolithicCacheCompilationOperation.Companion.BUILD_HEADER_CACHE
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativePerFileCacheCompilationOperation : BuildOperation<CompilationResult> {
    public val klib: Path
    public val outputCache: Path

    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public interface Builder : BuildOperation.Builder {
        public val klib: Path
        public val outputCache: Path
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun build(): NativePerFileCacheCompilationOperation
    }

    public operator fun <V> get(key: Option<V>): V

    public companion object {
        // TODO: Native second stage compiler options applicable for per-file cache building

        /**
         * Caches for all the dependencies of [klib].
         *
         * [NativeCache.Monolithic] caches could have been built with [BUILD_HEADER_CACHE] set to true.
         */
        public val CACHES: Option<List<NativeCache>> = Option("CACHES")
    }
}