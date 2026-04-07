/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import kotlin.reflect.KClass


class Options(
    private val optionsName: String,
) : DeepCopyable<Options> {
    constructor(typeForName: KClass<*>) : this(typeForName.toString())

    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    operator fun <V> set(key: BaseOption<V>, value: Any?) {
        optionsMap[key.id] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: BaseOption<V>): V = get(key.id)

    operator fun <V> get(key: BaseOptionWithDefault<V>): V = get(key.id)

    operator fun <V> set(key: BaseOptionWithDefault<V>, value: Any?) {
        optionsMap[key.id] = value
    }

    operator fun set(key: String, value: Any?) {
        optionsMap[key] = value
    }

    operator fun <V> get(key: String): V {
        @Suppress("UNCHECKED_CAST")
        return when (key) {
            in optionsMap -> optionsMap[key] as V
            else -> error("$key was not set in $optionsName")
        }
    }

    override fun deepCopy(): Options {
        return Options(optionsName).also { newOptions ->
            newOptions.optionsMap.putAll(optionsMap.entries.map {
                it.key to when (val value = it.value) {
                    is DeepCopyable<*> -> value.deepCopy()
                    else -> value
                }
            })
        }
    }
}
