/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.preference

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.test.InTextDirectivesUtils

class DebuggerPreferences(val project: Project, fileContents: String) {
    private val values: Map<String, Any?>

    init {
        val values = HashMap<String, Any?>()
        for (key in DebuggerPreferenceKeys.values) {
            val list = findValues(fileContents, key.name).takeIf { it.isNotEmpty() } ?: continue

            fun errorValue(): Nothing = error("Error value for key ${key.name}")

            val convertedValue: Any = when (key.type) {
                java.lang.Boolean::class.java -> list.singleOrNull()?.toBoolean() ?: errorValue()
                String::class.java -> list.singleOrNull() ?: errorValue()
                java.lang.Integer::class.java -> list.singleOrNull()?.toIntOrNull() ?: errorValue()
                List::class.java -> list
                else -> error("Cannot find a converter for type ${key.type}")
            }
            values[key.name] = convertedValue
        }
        this.values = values
    }

    private fun findValues(fileContents: String, key: String): List<String> {
        val list: List<String> = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContents, "// $key: ")
        if (list.isNotEmpty()) {
            return list
        }

        if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContents, true, false, "// $key").isNotEmpty()) {
            return listOf("true")
        }

        return emptyList()
    }

    operator fun <T : Any> get(key: DebuggerPreferenceKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return values[key.name] as T? ?: key.defaultValue
    }
}