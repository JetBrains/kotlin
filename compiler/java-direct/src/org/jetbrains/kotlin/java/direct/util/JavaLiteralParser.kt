/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree

/**
 * Shared Java literal parsing and constant-expression helpers used by both
 * [ConstantEvaluator] (field initializers) and annotation argument evaluation in
 * [JavaAnnotationOverAst].
 *
 * The [parseIntegerLiteral] / [parseLongLiteral] / [parseFloatLiteral] / [parseDoubleLiteral]
 * / [unescapeJavaString] methods turn the textual form of Java literal tokens into Kotlin
 * values. They are intentionally forgiving: on malformed input a zero/empty value is returned
 * rather than throwing, because upstream AST nodes may carry partially-recovered text from
 * the KMP Java parser.
 *
 * The [evaluateLiteral] and [evaluateNumericBinaryOp] methods are the higher-level building
 * blocks both evaluators share so that literal and numeric-operator semantics cannot drift
 * between the two contexts.
 */
internal object JavaLiteralParser {
    /**
     * Parses the text of an `INTEGER_LITERAL` token. Returns either [Int] or [Long]
     * depending on whether the value fits into an Int. Recognizes hex (`0x`), binary
     * (`0b`), and octal (leading `0`) prefixes, as well as underscore separators.
     */
    fun parseIntegerLiteral(text: String): Any {
        val cleaned = text.replace("_", "")
        return when {
            cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
                cleaned.substring(2).toIntOrNull(16) ?: cleaned.substring(2).toLongOrNull(16) ?: 0
            cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
                cleaned.substring(2).toIntOrNull(2) ?: cleaned.substring(2).toLongOrNull(2) ?: 0
            cleaned.startsWith("0") && cleaned.length > 1 && cleaned.all { it in '0'..'7' } ->
                cleaned.toIntOrNull(8) ?: cleaned.toLongOrNull(8) ?: 0
            else -> cleaned.toIntOrNull() ?: cleaned.toLongOrNull() ?: 0
        }
    }

    /**
     * Parses the text of a `LONG_LITERAL` token (with trailing `L`/`l` suffix).
     * Recognizes hex, binary, and octal prefixes, as well as underscore separators.
     */
    fun parseLongLiteral(text: String): Long {
        val cleaned = text.replace("_", "").removeSuffix("L").removeSuffix("l")
        return when {
            cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
                cleaned.substring(2).toLongOrNull(16) ?: 0L
            cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
                cleaned.substring(2).toLongOrNull(2) ?: 0L
            cleaned.startsWith("0") && cleaned.length > 1 ->
                cleaned.toLongOrNull(8) ?: 0L
            else -> cleaned.toLongOrNull() ?: 0L
        }
    }

    /** Parses the text of a `FLOAT_LITERAL` token (with optional `F`/`f` suffix). */
    fun parseFloatLiteral(text: String): Float {
        val cleaned = text.replace("_", "").removeSuffix("F").removeSuffix("f")
        return cleaned.toFloatOrNull() ?: 0f
    }

    /** Parses the text of a `DOUBLE_LITERAL` token (with optional `D`/`d` suffix). */
    fun parseDoubleLiteral(text: String): Double {
        val cleaned = text.replace("_", "").removeSuffix("D").removeSuffix("d")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /**
     * Unescapes the textual body of a Java string/character literal (the content
     * between the surrounding quotes). Supports standard escapes (`\n`, `\t`, `\r`,
     * `\b`, `\f`, `\'`, `\"`, `\\`), unicode escapes (`\uXXXX`), and 1–3 digit octal
     * escapes.
     */
    fun unescapeJavaString(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    'n' -> {
                        sb.append('\n'); i += 2
                    }
                    't' -> {
                        sb.append('\t'); i += 2
                    }
                    'r' -> {
                        sb.append('\r'); i += 2
                    }
                    'b' -> {
                        sb.append('\b'); i += 2
                    }
                    'f' -> {
                        sb.append('\u000C'); i += 2
                    }
                    '\'' -> {
                        sb.append('\''); i += 2
                    }
                    '"' -> {
                        sb.append('"'); i += 2
                    }
                    '\\' -> {
                        sb.append('\\'); i += 2
                    }
                    'u' -> {
                        if (i + 5 < text.length) {
                            val hex = text.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                            } else {
                                sb.append(text[i])
                                i++
                            }
                        } else {
                            sb.append(text[i])
                            i++
                        }
                    }
                    in '0'..'7' -> {
                        // Octal escape: 1–3 digits
                        var end = i + 2
                        while (end < text.length && end < i + 4 && text[end] in '0'..'7') end++
                        val octal = text.substring(i + 1, end)
                        val code = octal.toIntOrNull(8)
                        if (code != null) {
                            sb.append(code.toChar())
                            i = end
                        } else {
                            sb.append(text[i])
                            i++
                        }
                    }
                    else -> {
                        sb.append(text[i]); i++
                    }
                }
            } else {
                sb.append(text[i])
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Evaluates a `LITERAL_EXPRESSION` AST node to its Java value (String, Char, Boolean,
     * Number, or `null`). The single token child's type determines the kind:
     * string/character literals are unescaped via [unescapeJavaString]; numeric literal
     * tokens are routed through the [parseIntegerLiteral] / [parseLongLiteral] /
     * [parseFloatLiteral] / [parseDoubleLiteral] helpers; boolean and null keywords
     * produce the obvious constants.
     *
     * Returns `null` when the literal is the `null` keyword **or** when the child token type
     * is not a recognised literal kind; callers that need to distinguish those two cases must
     * inspect the node structure directly.
     */
    fun evaluateLiteral(node: JavaLightNode, tree: JavaLightTree): Any? {
        val literalChild = tree.getChildren(node).firstOrNull() ?: return null
        val text = tree.getText(literalChild).toString()

        return when (tree.getType(literalChild)) {
            JavaSyntaxTokenType.STRING_LITERAL -> {
                if (text.length >= 2) unescapeJavaString(text.substring(1, text.length - 1)) else text
            }
            JavaSyntaxTokenType.CHARACTER_LITERAL -> {
                if (text.length >= 3) unescapeJavaString(text.substring(1, text.length - 1)).firstOrNull() else null
            }
            JavaSyntaxTokenType.TRUE_KEYWORD -> true
            JavaSyntaxTokenType.FALSE_KEYWORD -> false
            JavaSyntaxTokenType.NULL_KEYWORD -> null
            JavaSyntaxTokenType.INTEGER_LITERAL -> parseIntegerLiteral(text)
            JavaSyntaxTokenType.LONG_LITERAL -> parseLongLiteral(text)
            JavaSyntaxTokenType.FLOAT_LITERAL -> parseFloatLiteral(text)
            JavaSyntaxTokenType.DOUBLE_LITERAL -> parseDoubleLiteral(text)
            else -> null
        }
    }

    /**
     * Evaluates a numeric binary operation with Java-ish promotion
     * (`Double → Float → Long → Int`). Supports the full set of numeric operators plus
     * shifts, bitwise ops, equality, and comparisons. Returns `null` for an operator that
     * doesn't produce a numeric/boolean result on two numeric operands.
     *
     * Shared between [ConstantEvaluator] (field initializer evaluation) and
     * [JavaAnnotationOverAst] (annotation argument evaluation) so that numeric semantics stay
     * consistent. Callers that need special cases (e.g. `String + x` for annotations) handle
     * those before delegating here.
     */
    fun evaluateNumericBinaryOp(lhs: Number, operator: SyntaxElementType, rhs: Number): Any? {
        val isFloat = lhs is Float || lhs is Double || rhs is Float || rhs is Double
        val isLong = !isFloat && (lhs is Long || rhs is Long)
        val isDouble = isFloat && (lhs is Double || rhs is Double)

        return when (operator) {
            JavaSyntaxTokenType.PLUS -> when {
                isDouble -> lhs.toDouble() + rhs.toDouble()
                isFloat -> lhs.toFloat() + rhs.toFloat()
                isLong -> lhs.toLong() + rhs.toLong()
                else -> lhs.toInt() + rhs.toInt()
            }
            JavaSyntaxTokenType.MINUS -> when {
                isDouble -> lhs.toDouble() - rhs.toDouble()
                isFloat -> lhs.toFloat() - rhs.toFloat()
                isLong -> lhs.toLong() - rhs.toLong()
                else -> lhs.toInt() - rhs.toInt()
            }
            JavaSyntaxTokenType.ASTERISK -> when {
                isDouble -> lhs.toDouble() * rhs.toDouble()
                isFloat -> lhs.toFloat() * rhs.toFloat()
                isLong -> lhs.toLong() * rhs.toLong()
                else -> lhs.toInt() * rhs.toInt()
            }
            JavaSyntaxTokenType.DIV -> when {
                isDouble -> lhs.toDouble() / rhs.toDouble()
                isFloat -> lhs.toFloat() / rhs.toFloat()
                isLong -> lhs.toLong() / rhs.toLong()
                else -> lhs.toInt() / rhs.toInt()
            }
            JavaSyntaxTokenType.PERC -> when {
                isDouble -> lhs.toDouble() % rhs.toDouble()
                isFloat -> lhs.toFloat() % rhs.toFloat()
                isLong -> lhs.toLong() % rhs.toLong()
                else -> lhs.toInt() % rhs.toInt()
            }
            JavaSyntaxTokenType.GTGT -> if (isLong) lhs.toLong() shr rhs.toInt() else lhs.toInt() shr rhs.toInt()
            JavaSyntaxTokenType.LTLT -> if (isLong) lhs.toLong() shl rhs.toInt() else lhs.toInt() shl rhs.toInt()
            JavaSyntaxTokenType.GTGTGT -> if (isLong) lhs.toLong() ushr rhs.toInt() else lhs.toInt() ushr rhs.toInt()
            JavaSyntaxTokenType.AND -> if (isLong) lhs.toLong() and rhs.toLong() else lhs.toInt() and rhs.toInt()
            JavaSyntaxTokenType.OR -> if (isLong) lhs.toLong() or rhs.toLong() else lhs.toInt() or rhs.toInt()
            JavaSyntaxTokenType.XOR -> if (isLong) lhs.toLong() xor rhs.toLong() else lhs.toInt() xor rhs.toInt()
            JavaSyntaxTokenType.EQEQ -> if (isFloat) lhs.toDouble() == rhs.toDouble() else lhs.toLong() == rhs.toLong()
            JavaSyntaxTokenType.NE -> if (isFloat) lhs.toDouble() != rhs.toDouble() else lhs.toLong() != rhs.toLong()
            JavaSyntaxTokenType.LT -> if (isFloat) lhs.toDouble() < rhs.toDouble() else lhs.toLong() < rhs.toLong()
            JavaSyntaxTokenType.LE -> if (isFloat) lhs.toDouble() <= rhs.toDouble() else lhs.toLong() <= rhs.toLong()
            JavaSyntaxTokenType.GT -> if (isFloat) lhs.toDouble() > rhs.toDouble() else lhs.toLong() > rhs.toLong()
            JavaSyntaxTokenType.GE -> if (isFloat) lhs.toDouble() >= rhs.toDouble() else lhs.toLong() >= rhs.toLong()
            else -> null
        }
    }
}
