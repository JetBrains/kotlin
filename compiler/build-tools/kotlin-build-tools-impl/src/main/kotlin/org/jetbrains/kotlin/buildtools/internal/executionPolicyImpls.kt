/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import kotlin.time.Duration

object InProcessExecutionPolicyImpl : ExecutionPolicy.InProcess

class DaemonExecutionPolicyImpl : ExecutionPolicy.WithDaemon {

    private val optionsDelegate = OptionsDelegate()

    init {
        this[JVM_ARGUMENTS] = null
        this[SHUTDOWN_DELAY] = null
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: ExecutionPolicy.WithDaemon.Option<V>): V = optionsDelegate[key.id]

    @UseFromImplModuleRestricted
    override fun <V> set(key: ExecutionPolicy.WithDaemon.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> get(key: Option<V>): V = optionsDelegate[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        /**
         * A list of JVM arguments to pass to the Kotlin daemon.
         */
        val JVM_ARGUMENTS: Option<List<String>?> = Option("JVM_ARGUMENTS")

        /**
         * The time that the daemon process continues to live after all clients have disconnected.
         */
        val SHUTDOWN_DELAY: Option<Duration?> = Option("SHUTDOWN_DELAY")
    }
}
