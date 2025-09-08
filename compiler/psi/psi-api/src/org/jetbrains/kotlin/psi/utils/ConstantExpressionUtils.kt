/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ConstantExpressionUtils")

package org.jetbrains.kotlin.psi.utils

import com.intellij.util.text.LiteralFormatUtil
import org.jetbrains.kotlin.KtStubBasedElementTypes.BOOLEAN_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.CHARACTER_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.FLOAT_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.INTEGER_CONSTANT
import org.jetbrains.kotlin.KtStubBasedElementTypes.NULL
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.utils.extractRadix

/**
 * Checks whether the [text] representation of a number literal has a long number suffix.
 */
fun hasLongNumericLiteralSuffix(text: String): Boolean {
    return text.endsWith('l') || text.endsWith('L')
}

/**
 * Checks whether the [text] representation of a number literal has an unsigned number suffix.
 */
fun hasUnsignedNumericLiteralSuffix(text: String): Boolean {
    return text.endsWith('u') || text.endsWith('U')
}

/**
 * Checks whether the [text] representation of a number literal has both along and unsigned number suffixes.
 */
fun hasUnsignedLongNumericLiteralSuffix(text: String): Boolean {
    return text.endsWith("ul") || text.endsWith("uL") ||
            text.endsWith("Ul") || text.endsWith("UL")
}

fun hasFloatNumericLiteralSuffix(text: String): Boolean {
    return text.endsWith('f') || text.endsWith('F')
}

/**
 * Parses the [text] representation of a number literal into a [Number] instance.
 *
 * For integer literals, always returns a [Long] instance.
 * For floating-point literals, returns either a [Float] or a [Double], depending on the literal precision.
 * Returns `null` if the text is not a valid representation of a number literal.
 *
 * @param isFloatingPointLiteral Specifies whether the number is floating-point (`Float` or `Double`)
 * or integer (`Byte`, `Short`, `Int` or `Long`).
 */
fun parseNumericLiteral(text: String, isFloatingPointLiteral: Boolean): Number? {
    val canonicalText = LiteralFormatUtil.removeUnderscores(text)
    return if (isFloatingPointLiteral) parseDecimalNumberLiteral(canonicalText) else parseLongNumericLiteral(canonicalText)
}

private fun parseLongNumericLiteral(text: String): Long? {
    fun String.removeSuffix(i: Int): String = this.substring(0, this.length - i)

    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
            hasUnsignedLongNumericLiteralSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(2)
            }
            hasUnsignedNumericLiteralSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(1)
            }
            hasLongNumericLiteralSuffix(text) -> {
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
    } catch (_: NumberFormatException) {
        null
    }
}

private fun parseDecimalNumberLiteral(text: String): Number? {
    if (hasFloatNumericLiteralSuffix(text)) {
        return parseFloat(text)
    }
    return parseDouble(text)
}

private fun parseDouble(text: String): Double? {
    return try {
        java.lang.Double.parseDouble(text)
    } catch (_: NumberFormatException) {
        null
    }
}

private fun parseFloat(text: String): Float? {
    return try {
        java.lang.Float.parseFloat(text)
    } catch (_: NumberFormatException) {
        null
    }
}

/**
 * Converts the given [text], either `true` or `false`, to a boolean value.
 *
 * @throws IllegalStateException if the [text] does not represent a valid boolean value.
 */
@Throws(IllegalStateException::class)
fun parseBooleanLiteral(text: String): Boolean {
    return when (text) {
        "true" -> true
        "false" -> false
        else -> throw IllegalStateException("'$text' is not a valid boolean literal")
    }
}

private val FP_LITERAL_PARTS = "([_\\d]*)\\.?([_\\d]*)e?[+-]?([_\\d]*)[f]?".toRegex()

/**
 * Checks whether the given [text] contains an underscore in an illegal position.
 * Underscores are allowed only between digits, not at the beginning or end of the number or one of its parts.
 */
fun hasIllegallyPositionedUnderscore(text: String, isFloatingPoint: Boolean): Boolean {
    val parts: List<String?> = if (isFloatingPoint) {
        FP_LITERAL_PARTS.findAll(text).flatMap { it.groupValues }.toList()
    } else {
        var start = 0
        var end: Int = text.length
        if (text.startsWith("0x", ignoreCase = true) || text.startsWith("0b", ignoreCase = true)) start += 2
        if (text.endsWith('l', ignoreCase = true)) --end
        listOf(text.substring(start, end))
    }

    return parts.any { it != null && (it.startsWith("_") || it.endsWith("_")) }
}

/**
 * Converts the given [ConstantValueKind] to the corresponding [com.intellij.psi.tree.IElementType].
 */
fun ConstantValueKind.toConstantExpressionElementType(): KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression> {
    return when (this) {
        ConstantValueKind.NULL -> NULL
        ConstantValueKind.BOOLEAN_CONSTANT -> BOOLEAN_CONSTANT
        ConstantValueKind.FLOAT_CONSTANT -> FLOAT_CONSTANT
        ConstantValueKind.CHARACTER_CONSTANT -> CHARACTER_CONSTANT
        ConstantValueKind.INTEGER_CONSTANT -> INTEGER_CONSTANT
    }
}

/**
 * Converts the given [com.intellij.psi.tree.IElementType] to the corresponding [ConstantValueKind].
 * The element type must be one of the constant expression types. Otherwise, an [IllegalArgumentException] is thrown.
 */
fun KtStubElementType<out KotlinConstantExpressionStub, KtConstantExpression>.toConstantValueKind(): ConstantValueKind {
    return when (this) {
        NULL -> ConstantValueKind.NULL
        BOOLEAN_CONSTANT -> ConstantValueKind.BOOLEAN_CONSTANT
        FLOAT_CONSTANT -> ConstantValueKind.FLOAT_CONSTANT
        CHARACTER_CONSTANT -> ConstantValueKind.CHARACTER_CONSTANT
        INTEGER_CONSTANT -> ConstantValueKind.INTEGER_CONSTANT
        else -> throw IllegalArgumentException("Unknown constant node type: $this")
    }
}