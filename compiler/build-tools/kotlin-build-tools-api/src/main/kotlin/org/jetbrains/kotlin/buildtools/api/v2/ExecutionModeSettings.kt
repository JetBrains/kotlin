/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2

import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.WithDefault
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation

public interface ExecutionPolicy {
    public interface Option<V>

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public enum class ExecutionMode {
        IN_PROCESS,
        DAEMON,
    }

    public companion object {
        private fun <V> optional(id: String, defaultValue: V): Option<V> =
            object : WithDefault<V>(id, defaultValue), Option<V> {}

        @JvmField
        public val EXECUTION_MODE: Option<ExecutionMode> = optional("EXECUTION_MODE", ExecutionMode.IN_PROCESS)

        @JvmField
        public val DAEMON_JVM_ARGUMENTS: Option<List<String>> = optional("DAEMON_JVM_ARGUMENTS", emptyList())
    }
}