/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.internal

public class OptionsDelegate<T> {
    private val optionsMap: MutableMap<T, Any?> = mutableMapOf()
    public operator fun <V> set(key: T, `value`: V) {
        optionsMap[key] = `value`
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <V> get(key: T): V = optionsMap.getOrElse(key, { error("$key not present in options") }) as V

    @Suppress("UNCHECKED_CAST")
    public fun <V> getOrElse(key: T, default: V): V = optionsMap.getOrElse(key) { default } as V
}