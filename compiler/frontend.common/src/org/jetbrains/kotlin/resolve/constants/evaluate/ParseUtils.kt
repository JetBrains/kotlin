/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants.evaluate

import com.intellij.psi.tree.IElementType
import com.intellij.util.text.LiteralFormatUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.utils.extractRadix

fun hasLongSuffix(text: String) = text.endsWith('l') || text.endsWith('L')
fun hasUnsignedSuffix(text: String) = text.endsWith('u') || text.endsWith('U')
fun hasUnsignedLongSuffix(text: String) =
    text.endsWith("ul") || text.endsWith("uL") ||
            text.endsWith("Ul") || text.endsWith("UL")

fun parseNumericLiteral(text: String, type: IElementType): Number? {
    val canonicalText = LiteralFormatUtil.removeUnderscores(text)
    return when (type) {
        KtNodeTypes.INTEGER_CONSTANT -> parseLong(canonicalText)
        KtNodeTypes.FLOAT_CONSTANT -> parseFloatingLiteral(canonicalText)
        else -> null
    }
}

private fun parseLong(text: String): Long? {
    fun String.removeSuffix(i: Int): String = this.substring(0, this.length - i)

    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
            hasUnsignedLongSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(2)
            }
            hasUnsignedSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(1)
            }
            hasLongSuffix(text) -> {
                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(1)
            }
            else -> {
                isUnsigned = false
                numberWithoutSuffix = text
            }
        }

        val (number, radix) = extractRadix(numberWithoutSuffix)

        if (isUnsigned) {
            java.lang.Long.parseUnsignedLong(number, radix)
        } else {
            java.lang.Long.parseLong(number, radix)
        }
    } catch (e: NumberFormatException) {
        null
    }
}

private fun parseFloatingLiteral(text: String): Number? {
    if (text.toLowerCase().endsWith('f')) {
        return parseFloat(text)
    }
    return parseDouble(text)
}

private fun parseDouble(text: String): Double? {
    try {
        return java.lang.Double.parseDouble(text)
    } catch (e: NumberFormatException) {
        return null
    }
}

private fun parseFloat(text: String): Float? {
    try {
        return java.lang.Float.parseFloat(text)
    } catch (e: NumberFormatException) {
        return null
    }
}

fun parseBoolean(text: String): Boolean {
    if ("true".equals(text)) {
        return true
    } else if ("false".equals(text)) {
        return false
    }

    throw IllegalStateException("Must not happen. A boolean literal has text: " + text)
}
