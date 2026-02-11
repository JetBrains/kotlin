/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.konan.NativeDependencies
import org.jetbrains.kotlin.buildtools.api.konan.NativeResolvedKlib
import java.nio.file.Path

/**
 * The result is all the dependencies of [klibs], topologically sorted respecting the dependencies.
 */
@ExperimentalBuildToolsApi
public interface NativeResolveKlibsOperation : BuildOperation<List<NativeResolvedKlib>> {
    public val klibs: List<Path>

    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public interface Builder : BuildOperation.Builder {
        public val klibs: List<Path>
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun build(): NativeResolveKlibsOperation
    }

    public operator fun <V> get(key: Option<V>): V

    public companion object {
        /**
         * When empty, the compiler may download the dependencies itself.
         *
         * *NOTE:* just the distribution is enough.
         */
        public val NATIVE_DEPENDENCIES: Option<NativeDependencies> = Option("NATIVE_DEPENDENCIES")
    }
}