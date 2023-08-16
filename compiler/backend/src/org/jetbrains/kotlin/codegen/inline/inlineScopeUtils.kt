/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

fun String.dropInlineScopeInfo(): String =
    substringBefore(INLINE_SCOPE_NUMBER_SEPARATOR)

fun String.getScopeNumber(): Int? =
    substringAfter(INLINE_SCOPE_NUMBER_SEPARATOR)
        .substringBefore(INLINE_SCOPE_NUMBER_SEPARATOR)
        .toIntOrNull()

fun String.getSurroundingScopeNumber(): Int? =
    substringAfter(INLINE_SCOPE_NUMBER_SEPARATOR, "")
        .substringAfter(INLINE_SCOPE_NUMBER_SEPARATOR, "")
        .toIntOrNull()
