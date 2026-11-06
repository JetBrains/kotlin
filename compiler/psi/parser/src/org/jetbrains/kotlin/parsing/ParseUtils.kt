/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.utils.hasIllegallyPositionedUnderscore
import org.jetbrains.kotlin.psi.utils.hasLongNumericLiteralSuffix
import org.jetbrains.kotlin.psi.utils.hasUnsignedLongNumericLiteralSuffix
import org.jetbrains.kotlin.psi.utils.hasUnsignedNumericLiteralSuffix
import org.jetbrains.kotlin.psi.utils.parseBooleanLiteral

fun hasIllegalUnderscore(text: String, elementType: IElementType): Boolean {
    return when {
        elementType === KtNodeTypes.INTEGER_CONSTANT -> hasIllegallyPositionedUnderscore(text, isFloatingPoint = false)
        else -> hasIllegallyPositionedUnderscore(text, isFloatingPoint = true)
    }
}

fun hasLongSuffix(text: String) = hasLongNumericLiteralSuffix(text)
fun hasUnsignedSuffix(text: String) = hasUnsignedNumericLiteralSuffix(text)
fun hasUnsignedLongSuffix(text: String) = hasUnsignedLongNumericLiteralSuffix(text)

@Deprecated(
    "Use 'org.jetbrains.kotlin.psi.utils.parseNumericLiteral' instead.",
    ReplaceWith("parseNumericLiteral(text, type)", "org.jetbrains.kotlin.psi.utils.parseNumericLiteral"),
)
fun parseNumericLiteral(text: String, type: IElementType): Number? {
    return org.jetbrains.kotlin.psi.utils.parseNumericLiteral(text, type)
}

fun parseBoolean(text: String): Boolean = parseBooleanLiteral(text)
