/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

public interface ExecutionPolicy {
    public class Option<V> internal constructor(public val id: String)

    public operator fun <V> get(key: Option<V>): V?

    public operator fun <V> set(key: Option<V>, value: V)

    public enum class ExecutionMode {
        IN_PROCESS,
        DAEMON,
    }

    public companion object {
        @JvmField
        public val EXECUTION_MODE: Option<ExecutionMode> = Option("EXECUTION_MODE")

        @JvmField
        public val DAEMON_JVM_ARGUMENTS: Option<List<String>> = Option("DAEMON_JVM_ARGUMENTS")
    }
}