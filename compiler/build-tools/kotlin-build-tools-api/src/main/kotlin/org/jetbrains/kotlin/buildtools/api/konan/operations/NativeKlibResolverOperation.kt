/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.konan.NativeKlibResolverResult
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativeKlibResolverOperation : BuildOperation<NativeKlibResolverResult> {
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        /**
         * Where to look up the platform libs
         */
        @JvmField
        public val PLATFORM_LIBS_ROOT: Option<Path> = Option("PLATFORM_LIBS_ROOT")
    }
}