/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.api

import java.time.Duration

/**
 * Allows users to customize the compiler execution strategy.
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@Deprecated("Use the new BTA API with entry points in KotlinToolchain instead")
@ExperimentalBuildToolsApi
public interface CompilerExecutionStrategyConfiguration {
    /**
     * Marks the compilation to be run inside the same JVM as the caller.
     * The default strategy.
     */
    public fun useInProcessStrategy(): CompilerExecutionStrategyConfiguration

    /**
     * Marks the compilation to be run in Kotlin daemon launched as a separate process and shared across similar compilation requests.
     * See Kotlin daemon documentation: https://kotl.in/daemon
     * @param jvmArguments a list of JVM startup arguments for the daemon
     */
    public fun useDaemonStrategy(
        jvmArguments: List<String>,
    ): CompilerExecutionStrategyConfiguration

    /**
     * Marks the compilation to be run in Kotlin daemon launched as a separate process and shared across similar compilation requests.
     * See Kotlin daemon documentation: https://kotl.in/daemon
     * @param jvmArguments a list of JVM startup arguments for the daemon
     * @param shutdownDelay the time that the daemon process continues to live after all clients have disconnected
     */
    public fun useDaemonStrategy(
        jvmArguments: List<String>,
        shutdownDelay: Duration,
    ): CompilerExecutionStrategyConfiguration
}