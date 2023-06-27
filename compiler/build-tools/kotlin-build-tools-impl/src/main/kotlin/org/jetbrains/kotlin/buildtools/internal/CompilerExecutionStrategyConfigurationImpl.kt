/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import java.io.File

internal sealed interface CompilerExecutionStrategy {
    data object InProcess : CompilerExecutionStrategy

    data class Daemon(val sessionDir: File, val jvmArguments: List<String>) : CompilerExecutionStrategy
}

internal class CompilerExecutionStrategyConfigurationImpl : CompilerExecutionStrategyConfiguration {
    internal var selectedStrategy: CompilerExecutionStrategy = CompilerExecutionStrategy.InProcess

    override fun useInProcessStrategy(): CompilerExecutionStrategyConfiguration {
        selectedStrategy = CompilerExecutionStrategy.InProcess
        return this
    }

    override fun useDaemonStrategy(sessionDir: File, jvmArguments: List<String>): CompilerExecutionStrategyConfiguration {
        selectedStrategy = CompilerExecutionStrategy.Daemon(sessionDir, jvmArguments)
        return this
    }
}