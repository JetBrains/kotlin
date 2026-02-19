/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

inline fun <E> Collection<E>.joinToEnglishString(conjunction: String, format: (E) -> String): String =
    when {
        isEmpty() -> ""
        size == 1 -> format(first())
        else -> buildString {
            val list = this@joinToEnglishString.toList()

            for (it in 0 until list.size - 1) {
                append(format(list[it]))
                append(", ")
            }

            append("$conjunction ${format(list.last())}")
        }
    }

inline fun <E> Collection<E>.joinToEnglishOrString(format: (E) -> String = { it.toString() }): String =
    joinToEnglishString("or", format)

inline fun <E> Collection<E>.joinToEnglishAndString(format: (E) -> String = { it.toString() }): String =
    joinToEnglishString("and", format)
