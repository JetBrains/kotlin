/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

class Directives {

    private val directives = mutableMapOf<String, MutableList<String>?>()

    operator fun contains(key: String): Boolean {
        return key in directives
    }

    operator fun get(key: String): String? {
        return directives[key]?.single()
    }

    fun put(key: String, value: String?) {
        if (value == null) {
            directives[key] = null
        } else {
            directives.getOrPut(key, { arrayListOf() })!!.add(value)
        }
    }

    // Such values could be defined several times, e.g
    // MY_DIRECTIVE: XXX
    // MY_DIRECTIVE: YYY
    // or
    // MY_DIRECTIVE: XXX, YYY
    fun listValues(name: String): List<String>? {
        return directives[name]?.let { values ->
            values.flatMap { InTextDirectivesUtils.splitValues(arrayListOf(), it) }
        }
    }

    public fun asMapOfSingleValues(): Map<String, String?> {
        return directives.entries.associate { it.key to it.value?.single() }
    }
}