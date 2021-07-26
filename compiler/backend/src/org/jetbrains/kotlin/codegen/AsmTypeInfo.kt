/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.utils.threadLocal

object AsmTypeInfo {
    private val mappings by threadLocal { mutableMapOf<String, MutableSet<String>>() }

    fun put(type: String, superType: String) {
        if (mappings[type] == null) {
            mappings[type] = mutableSetOf()
        }
        mappings[type]!!.add(superType)
    }

    fun isSubType(type: String, superType: String): Boolean {
        // ASM caches the result of getCommonSuperClass, so it is safe to do remove
        val result = mappings[type]?.remove(superType) == true
        if (result && mappings[type]!!.isEmpty()) {
            mappings.remove(type)
        }
        return result
    }

    fun clear() = mappings.clear()
}