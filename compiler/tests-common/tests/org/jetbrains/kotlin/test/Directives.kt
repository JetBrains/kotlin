/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

class Directives {

    private val directives = mutableMapOf<String, String?>()

    operator fun contains(key: String): Boolean {
        return key in directives
    }

    operator fun get(key: String): String? {
        return directives[key]
    }

    fun put(key: String, value: String?): String? {
        return directives.put(key, value)
    }

    public fun asMapOfSingleValues(): Map<String, String?> {
        return directives.entries.associate { it.key to it.value?.single() }
    }
}