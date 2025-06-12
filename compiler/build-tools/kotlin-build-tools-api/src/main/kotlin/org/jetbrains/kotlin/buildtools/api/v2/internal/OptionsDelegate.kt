/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.internal

public class OptionsDelegate {
    private val optionsMap: MutableMap<Option, Any?> = mutableMapOf()

    public operator fun set(key: Any, `value`: Any?) {
        key as Option
        optionsMap[key] = `value`
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <V> get(key: Any): V {
        key as Option
        return optionsMap.getOrElse(key, {
            when (key) {
                is Option.Mandatory -> error("$key not present in options")
                is Option.WithDefault<*> -> key.defaultValue
            }
        }) as V
    }

//    @Suppress("UNCHECKED_CAST")
//    public fun <V> getOrElse(key: T, default: V): V = optionsMap.getOrElse(key) { default } as V

//    public operator fun contains(key: T): Boolean = optionsMap.containsKey(key)
}