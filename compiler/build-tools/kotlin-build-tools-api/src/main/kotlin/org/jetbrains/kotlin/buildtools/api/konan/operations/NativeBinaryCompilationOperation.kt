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
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativeBinaryCompilationOperation : BuildOperation<CompilationResult> {
    public val klibs: List<Path>
    public val destinationDirectory: Path

    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public interface Builder : BuildOperation.Builder {
        public val klibs: List<Path>
        public val destinationDirectory: Path
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun build(): NativeBinaryCompilationOperation
    }

    public operator fun <V> get(key: Option<V>): V

    /**
     * Returns a pre-configured [NativeMonolithicCacheCompilationOperation.Builder] fit for building the correct monolithic cache
     * for this [NativeBinaryCompilationOperation].
     *
     * The result corresponds to [NativeCache.Monolithic].
     *
     * **NOTE**: Caches for all dependencies of [klib] must be built and given in [NativeMonolithicCacheCompilationOperation.CACHES]
     */
    public fun nativeMonolithicCacheCompilationOperationBuilder(
        klib: Path,
        outputCache: Path,
    ): NativeMonolithicCacheCompilationOperation.Builder

    /**
     * Returns a pre-configured [NativePerFileCacheCompilationOperation.Builder] fit for building the correct per-file cache
     * for this [NativeBinaryCompilationOperation].
     *
     * The result corresponds to [NativeCache.PerFile].
     *
     * **NOTE**: Caches for all dependencies of [klib] must be built and given in [NativePerFileCacheCompilationOperation.CACHES]
     */
    public fun nativePerFileCacheCompilationOperationBuilder(
        klib: Path,
        outputCache: Path,
    ): NativePerFileCacheCompilationOperation.Builder

    public companion object {
        // TODO: Native second stage compiler options

        /**
         * Prebuilt caches for a subset of `klibs`.
         */
        public val CACHES: Option<List<NativeCache>> = Option("CACHES")
    }
}

@ExperimentalBuildToolsApi
public inline fun NativeBinaryCompilationOperation.nativeMonolithicCacheCompilationOperation(
    klib: Path,
    outputCache: Path,
    action: NativeMonolithicCacheCompilationOperation.Builder.() -> Unit,
): NativeMonolithicCacheCompilationOperation = nativeMonolithicCacheCompilationOperationBuilder(klib, outputCache).apply(action).build()

@ExperimentalBuildToolsApi
public inline fun NativeBinaryCompilationOperation.nativePerFileCacheCompilationOperation(
    klib: Path,
    outputCache: Path,
    action: NativePerFileCacheCompilationOperation.Builder.() -> Unit,
): NativePerFileCacheCompilationOperation = nativePerFileCacheCompilationOperationBuilder(klib, outputCache).apply(action).build()
