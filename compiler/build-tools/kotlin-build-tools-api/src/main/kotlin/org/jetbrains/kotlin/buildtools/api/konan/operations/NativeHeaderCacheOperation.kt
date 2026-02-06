/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.konan.NativeHeaderCache
import org.jetbrains.kotlin.buildtools.api.konan.NativeHeaderCacheResult

@ExperimentalBuildToolsApi
public interface NativeHeaderCacheOperation : BuildOperation<NativeHeaderCacheResult> {
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        @JvmField
        public val DEPENDENCIES: Option<List<NativeHeaderCache>> = Option("DEPENDENCIES")

        // TODO: any additional arguments that affect header cache
    }
}