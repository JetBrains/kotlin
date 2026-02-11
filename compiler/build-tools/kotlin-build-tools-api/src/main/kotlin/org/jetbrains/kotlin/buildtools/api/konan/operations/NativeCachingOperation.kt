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
import org.jetbrains.kotlin.buildtools.api.konan.NativeCacheKind
import org.jetbrains.kotlin.buildtools.api.konan.NativeDependencies
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativeCachingOperation : BuildOperation<CompilationResult> {
    public val klib: Path
    public val kind: NativeCacheKind
    public val outputCache: Path

    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public interface Builder : BuildOperation.Builder {
        public val klib: Path
        public val outputCache: Path
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun build(): NativeCachingOperation
    }

    public operator fun <V> get(key: Option<V>): V

    public companion object {
        // TODO: Native second stage compiler options applicable for cache building

        /**
         * When empty, the compiler may download the dependencies itself
         */
        public val NATIVE_DEPENDENCIES: Option<NativeDependencies> = Option("NATIVE_DEPENDENCIES")

        /**
         * Caches for all the dependencies of [klib]. Can be [NativeCacheKind.HEADER].
         */
        public val CACHES: Option<List<NativeCache>> = Option("CACHES")
    }
}