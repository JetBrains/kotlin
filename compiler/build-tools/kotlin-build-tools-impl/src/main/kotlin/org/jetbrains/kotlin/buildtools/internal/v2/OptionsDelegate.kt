/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2

import org.jetbrains.kotlin.buildtools.api.v2.internal.BaseOption

class OptionsDelegate {
    private val optionsMap: MutableMap<BaseOption<*>, Any?> = mutableMapOf()

    operator fun <V> set(key: BaseOption<V>, value: Any?) {
        optionsMap[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: BaseOption<V>): V {
        return if (key !in optionsMap) {
            error("$key not present in options")
        } else optionsMap[key] as V
    }
}