/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Gives the compiler access to what a build system recorded for each module it compiled previously.
 *
 * A compilation may cover several modules at once, each with its own record of earlier compilations.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerIncrementalCompilationComponents {
    /**
     * Returns what was recorded for [target] the last time it was compiled.
     *
     * @param target the module being asked about
     */
    public fun getIncrementalCache(target: CompilerTargetId): CompilerIncrementalCache
}
