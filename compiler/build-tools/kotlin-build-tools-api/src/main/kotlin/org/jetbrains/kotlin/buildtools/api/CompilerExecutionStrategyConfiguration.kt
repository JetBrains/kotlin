/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.io.File

/**
 * Allows users to customize the compiler execution strategy.
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface CompilerExecutionStrategyConfiguration {
    /**
     * Marks the compilation to be run inside the same JVM as the caller.
     * The default strategy.
     */
    public fun useInProcessStrategy(): CompilerExecutionStrategyConfiguration

    /**
     * Marks the compilation to be run in Kotlin daemon launched as a separate process and shared across similar compilation requests.
     * See Kotlin daemon documentation here: https://kotlinlang.org/docs/gradle-compilation-and-caches.html#the-kotlin-daemon-and-how-to-use-it-with-gradle
     * @param jvmArguments a list of JVM startup arguments for the daemon
     */
    public fun useDaemonStrategy(
        jvmArguments: List<String>,
    ): CompilerExecutionStrategyConfiguration
}