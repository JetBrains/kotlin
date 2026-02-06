/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativeLinkingOperation : BuildOperation<CompilationResult> {
    public val klibs: List<Path>
    public val destinationDirectory: Path

    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public interface Builder : BuildOperation.Builder {
        public val klibs: List<Path>
        public val destinationDirectory: Path
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun build(): NativeLinkingOperation
    }

    public operator fun <V> get(key: Option<V>): V

    public companion object {
        // TODO: Native second stage compiler options
    }
}