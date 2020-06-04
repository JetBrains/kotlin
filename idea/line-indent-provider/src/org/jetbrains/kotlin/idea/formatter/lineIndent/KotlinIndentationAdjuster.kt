/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.lineIndent

interface KotlinIndentationAdjuster {
    // ALIGN_MULTILINE_METHOD_BRACKETS
    val alignWhenMultilineFunctionParentheses: Boolean
        get() = false

    // ALIGN_MULTILINE_BINARY_OPERATION
    val alignWhenMultilineBinaryExpression: Boolean
        get() = false
}