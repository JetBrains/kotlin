/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption

/**
 * A set of option values, keyed by option id.
 *
 * Mirrors the option storage of `kotlin-build-tools-impl`, which is internal to that module. Keying by id is what
 * makes the option an API consumer sets and the option this module reads the same slot.
 */
internal class Options private constructor(
    private val optionsName: String,
    private val values: MutableMap<String, Any?>,
) {
    constructor(optionsName: String, defaults: List<Option<*>>) : this(
        optionsName,
        defaults.associateTo(mutableMapOf()) { it.id to it.defaultValue },
    )

    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(key: BaseOption<V>): V = get(key.id)

    operator fun <V> set(key: BaseOption<V>, value: V) {
        values[key.id] = value
    }

    operator fun <V> get(key: Option<V>): V = get(key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        values[key.id] = value
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V> get(id: String): V = when (id) {
        in values -> values[id] as V
        else -> error("$id was not set in $optionsName")
    }

    fun copy(): Options = Options(optionsName, values.toMutableMap())
}

/**
 * An option of a type in this module, carrying the value to use when a consumer sets none.
 */
internal class Option<V>(val id: String, val defaultValue: V)
