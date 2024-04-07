/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

data class InlineScopeInfo(val scopeNumber: Int, val callSiteLineNumber: Int?, val surroundingScopeNumber: Int?)

fun String.dropInlineScopeInfo(): String =
    substringBefore(INLINE_SCOPE_NUMBER_SEPARATOR)

fun String.getInlineScopeInfo(): InlineScopeInfo? {
    val inlineScopeInfoSuffix = substringAfter(INLINE_SCOPE_NUMBER_SEPARATOR)
    val numbers = arrayOf(StringBuilder(), StringBuilder(), StringBuilder())
    var currentIndex = 0
    for (char in inlineScopeInfoSuffix) {
        if (char == INLINE_SCOPE_NUMBER_SEPARATOR) {
            if (currentIndex >= numbers.size) {
                return null
            }
            currentIndex += 1
        } else {
            numbers[currentIndex].append(char)
        }
    }

    val scopeNumber = numbers[0].toString().toIntOrNull() ?: return null
    val callSiteLineNumber = numbers[1].toString().toIntOrNull()
    val surroundingScopeNumber = numbers[2].toString().toIntOrNull()
    return InlineScopeInfo(scopeNumber, callSiteLineNumber, surroundingScopeNumber)
}
