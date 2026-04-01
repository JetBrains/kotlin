/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

class Directives {
    val allDirectives: Map<String, List<String>?>
        field = mutableMapOf<String, MutableList<String>?>()

    operator fun contains(key: String): Boolean {
        return key in allDirectives
    }

    operator fun get(key: String): String? {
        return allDirectives[key]?.single()
    }

    fun put(key: String, value: String?) {
        if (value == null) {
            allDirectives[key] = null
        } else {
            allDirectives.getOrPut(key, { arrayListOf() }).let {
                it?.add(value) ?: error("Null value was already passed to $key via smth like // $key")
            }
        }
    }

    // Such values could be defined several times, e.g
    // MY_DIRECTIVE: XXX
    // MY_DIRECTIVE: YYY
    // or
    // MY_DIRECTIVE: XXX, YYY
    fun listValues(name: String): List<String>? {
        return allDirectives[name]?.let { values ->
            values.flatMap { InTextDirectivesUtils.splitValues(arrayListOf(), it) }
        }
    }
}