/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * The class represents different strategies for running a compilation process and their settings (if applicable).
 */
@ExperimentalBuildToolsApi
sealed class CompilationStrategySettings {
    /**
     * A strategy that performs the compilation process in the same process as the caller.
     */
    object InProcess : CompilationStrategySettings()

//    class Daemon : CompilationStrategySettings()
}