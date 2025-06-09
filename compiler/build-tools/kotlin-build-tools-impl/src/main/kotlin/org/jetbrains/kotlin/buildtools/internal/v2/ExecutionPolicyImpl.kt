/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate

class ExecutionPolicyImpl : ExecutionPolicy {
    private val optionsDelegate = OptionsDelegate<ExecutionPolicy.Option<*>>()

    override fun <V> get(key: ExecutionPolicy.Option<V>): V = optionsDelegate[key]

    override fun <V> set(key: ExecutionPolicy.Option<V>, value: V) {
        optionsDelegate[key] = value
    }
}
