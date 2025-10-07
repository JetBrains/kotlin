/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy

internal object InProcessExecutionPolicyImpl : ExecutionPolicy.InProcess

internal class DaemonExecutionPolicyImpl : ExecutionPolicy.WithDaemon {

    private val options: Options = Options(ExecutionPolicy.WithDaemon::class)

    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V = options[key.id]

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        /**
         * A list of JVM arguments to pass to the Kotlin daemon.
         */
        val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS", default = null)

        /**
         * The time in milliseconds that the daemon process continues to live after all clients have disconnected.
         */
        val SHUTDOWN_DELAY_MILLIS: Option<Long?> = Option("SHUTDOWN_DELAY_MILLIS", null)
    }
}
