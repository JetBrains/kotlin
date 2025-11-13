/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import java.nio.file.Path
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain

@ExperimentalBuildToolsApi
public interface NativeLinkingOperation : BuildOperation<CompilationResult> {
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    // todo: linker arguments

    public companion object {
        @JvmField
        public val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> = Option("COMPILER_ARGUMENTS_LOG_LEVEL")

        /**
         * Caches built through the machinery of [NativePlatformToolchain.createCachesOrchestrationOperation] to speed up the actual linking
         * If unset, they might be automatically created and managed enough for local executions
         */
        @JvmField
        public val CACHES: Option<Set<Path>> = Option("CACHES")
    }
}