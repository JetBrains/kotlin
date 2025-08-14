/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import kotlin.collections.get

fun <T> T.anonymousContextParameterName(
    allUnnamedContextParameters: List<T>,
    invalidChars: Set<Char>,
    erasedUpperBoundName: (T) -> String,
): String? {
    val contextParameterNames = allUnnamedContextParameters
        .associateWith { erasedUpperBoundName(it).replaceInvalidChars(invalidChars) }
    val nameGroups = contextParameterNames.entries.groupBy({ it.value }, { it.key })
    val baseName = contextParameterNames[this]
    val currentNameGroup = nameGroups[baseName]!!
    return if (currentNameGroup.size == 1) $$"$context-$$baseName" else $$"$context-$$baseName#$${currentNameGroup.indexOf(this) + 1}"
}

private fun String.replaceInvalidChars(invalidChars: Set<Char>) =
    invalidChars.fold(this) { acc, ch -> if (ch in acc) acc.replace(ch, '_') else acc }