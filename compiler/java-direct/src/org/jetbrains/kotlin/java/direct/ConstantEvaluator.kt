/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import kotlin.experimental.inv

/**
 * Evaluates constant expressions in Java field initializers.
 * This is needed for Kotlin's const val support when using Java constants.
 *
 * Handles:
 * - Literal values (integers, longs, floats, doubles, strings, chars, booleans)
 * - Binary operations (+, -, *, /, %, &, |, ^, <<, >>, >>>)
 * - Unary operations (-, ~, !)
 * - Field references to other static final fields in the same compilation unit
 * - Parenthesized expressions
 * - Cross-language constant evaluation via callback (Java referencing Kotlin constants)
 *
 * @param containingClass the class containing the field being evaluated
 * @param resolveExternalReference optional callback to resolve references to external classes (e.g., Kotlin classes)
 */
class ConstantEvaluator(
    private val containingClass: JavaClassOverAst,
    private val resolveExternalReference: ((classQualifier: String?, fieldName: String) -> Any?)? = null,
) {
    /**
     * Evaluates a constant expression node and returns the computed value.
     * Returns null if the expression cannot be evaluated as a constant.
     */
    fun evaluate(node: JavaSyntaxNode): Any? {
        return when (node.type.toString()) {
            "LITERAL_EXPRESSION" -> evaluateLiteral(node)
            "BINARY_EXPRESSION" -> evaluateBinaryExpression(node)
            "PREFIX_EXPRESSION" -> evaluatePrefixExpression(node)
            "PARENS_EXPRESSION" -> evaluateParensExpression(node)
            "REFERENCE_EXPRESSION" -> evaluateReferenceExpression(node)
            "POLYADIC_EXPRESSION" -> evaluatePolyadicExpression(node)
            else -> null
        }
    }

    private fun evaluateLiteral(node: JavaSyntaxNode): Any? {
        val literalChild = node.children.firstOrNull() ?: return null
        val text = literalChild.text

        return when (literalChild.type.toString()) {
            "STRING_LITERAL" -> {
                if (text.length >= 2) {
                    text.substring(1, text.length - 1).unescapeJavaString()
                } else text
            }
            "CHARACTER_LITERAL" -> {
                if (text.length >= 3) {
                    text.substring(1, text.length - 1).unescapeJavaString().firstOrNull()
                } else null
            }
            "TRUE_KEYWORD" -> true
            "FALSE_KEYWORD" -> false
            "NULL_LITERAL" -> null
            "INTEGER_LITERAL" -> parseIntegerLiteral(text)
            "LONG_LITERAL" -> parseLongLiteral(text)
            "FLOAT_LITERAL" -> parseFloatLiteral(text)
            "DOUBLE_LITERAL" -> parseDoubleLiteral(text)
            else -> null
        }
    }

    private fun evaluateBinaryExpression(node: JavaSyntaxNode): Any? {
        val children = node.children.filter { it.type.toString() !in listOf("WHITE_SPACE") }
        if (children.size < 3) return null

        val lhs = evaluate(children[0]) ?: return null
        val operator = children[1].type.toString()
        val rhs = evaluate(children[2]) ?: return null

        return evaluateBinaryOp(lhs, operator, rhs)
    }

    private fun evaluatePolyadicExpression(node: JavaSyntaxNode): Any? {
        // Polyadic expression: a + b + c + d (multiple operands with same operator)
        val children = node.children.filter { it.type.toString() !in listOf("WHITE_SPACE") }
        if (children.size < 3) return null

        var result = evaluate(children[0]) ?: return null
        var i = 1
        while (i < children.size - 1) {
            val operator = children[i].type.toString()
            val operand = evaluate(children[i + 1]) ?: return null
            result = evaluateBinaryOp(result, operator, operand) ?: return null
            i += 2
        }
        return result
    }

    private fun evaluateBinaryOp(lhs: Any, operator: String, rhs: Any): Any? {
        // String concatenation
        if (lhs is String && operator == "PLUS") {
            return lhs + rhs.toString()
        }
        if (rhs is String && operator == "PLUS") {
            return lhs.toString() + rhs
        }

        // Boolean operations
        if (lhs is Boolean && rhs is Boolean) {
            return when (operator) {
                "ANDAND" -> lhs && rhs
                "OROR" -> lhs || rhs
                "EQEQ" -> lhs == rhs
                "NE" -> lhs != rhs
                "XOR" -> lhs xor rhs
                "AND" -> lhs and rhs
                "OR" -> lhs or rhs
                else -> null
            }
        }

        // Numeric operations
        if (lhs is Number && rhs is Number) {
            return evaluateNumericOp(lhs, operator, rhs)
        }

        // Char operations (treat as int)
        if (lhs is Char && rhs is Number) {
            return evaluateNumericOp(lhs.code, operator, rhs)?.let { result ->
                if (result is Int && operator == "PLUS") result.toChar() else result
            }
        }
        if (lhs is Number && rhs is Char) {
            return evaluateNumericOp(lhs, operator, rhs.code)
        }
        if (lhs is Char && rhs is Char) {
            return evaluateNumericOp(lhs.code, operator, rhs.code)
        }

        return null
    }

    private fun evaluateNumericOp(lhs: Number, operator: String, rhs: Number): Any? {
        val isFloat = lhs is Float || lhs is Double || rhs is Float || rhs is Double
        val isLong = !isFloat && (lhs is Long || rhs is Long)
        val isDouble = isFloat && (lhs is Double || rhs is Double)

        return when (operator) {
            "PLUS" -> when {
                isDouble -> lhs.toDouble() + rhs.toDouble()
                isFloat -> lhs.toFloat() + rhs.toFloat()
                isLong -> lhs.toLong() + rhs.toLong()
                else -> lhs.toInt() + rhs.toInt()
            }
            "MINUS" -> when {
                isDouble -> lhs.toDouble() - rhs.toDouble()
                isFloat -> lhs.toFloat() - rhs.toFloat()
                isLong -> lhs.toLong() - rhs.toLong()
                else -> lhs.toInt() - rhs.toInt()
            }
            "ASTERISK" -> when {
                isDouble -> lhs.toDouble() * rhs.toDouble()
                isFloat -> lhs.toFloat() * rhs.toFloat()
                isLong -> lhs.toLong() * rhs.toLong()
                else -> lhs.toInt() * rhs.toInt()
            }
            "DIV" -> when {
                isDouble -> lhs.toDouble() / rhs.toDouble()
                isFloat -> lhs.toFloat() / rhs.toFloat()
                isLong -> lhs.toLong() / rhs.toLong()
                else -> lhs.toInt() / rhs.toInt()
            }
            "PERC" -> when {
                isDouble -> lhs.toDouble() % rhs.toDouble()
                isFloat -> lhs.toFloat() % rhs.toFloat()
                isLong -> lhs.toLong() % rhs.toLong()
                else -> lhs.toInt() % rhs.toInt()
            }
            "GTGT" -> if (isLong) lhs.toLong() shr rhs.toInt() else lhs.toInt() shr rhs.toInt()
            "LTLT" -> if (isLong) lhs.toLong() shl rhs.toInt() else lhs.toInt() shl rhs.toInt()
            "GTGTGT" -> if (isLong) lhs.toLong() ushr rhs.toInt() else lhs.toInt() ushr rhs.toInt()
            "AND" -> if (isLong) lhs.toLong() and rhs.toLong() else lhs.toInt() and rhs.toInt()
            "OR" -> if (isLong) lhs.toLong() or rhs.toLong() else lhs.toInt() or rhs.toInt()
            "XOR" -> if (isLong) lhs.toLong() xor rhs.toLong() else lhs.toInt() xor rhs.toInt()
            "EQEQ" -> if (isFloat) lhs.toDouble() == rhs.toDouble() else lhs.toLong() == rhs.toLong()
            "NE" -> if (isFloat) lhs.toDouble() != rhs.toDouble() else lhs.toLong() != rhs.toLong()
            "LT" -> if (isFloat) lhs.toDouble() < rhs.toDouble() else lhs.toLong() < rhs.toLong()
            "LE" -> if (isFloat) lhs.toDouble() <= rhs.toDouble() else lhs.toLong() <= rhs.toLong()
            "GT" -> if (isFloat) lhs.toDouble() > rhs.toDouble() else lhs.toLong() > rhs.toLong()
            "GE" -> if (isFloat) lhs.toDouble() >= rhs.toDouble() else lhs.toLong() >= rhs.toLong()
            else -> null
        }
    }

    private fun evaluatePrefixExpression(node: JavaSyntaxNode): Any? {
        val children = node.children.filter { it.type.toString() !in listOf("WHITE_SPACE") }
        if (children.size < 2) return null

        val operator = children[0].type.toString()
        val operand = evaluate(children[1]) ?: return null

        return when (operator) {
            "MINUS" -> when (operand) {
                is Int -> -operand
                is Long -> -operand
                is Float -> -operand
                is Double -> -operand
                else -> null
            }
            "PLUS" -> operand // Unary plus is identity
            "TILDE" -> when (operand) {
                is Int -> operand.inv()
                is Long -> operand.inv()
                is Short -> operand.inv()
                is Byte -> operand.inv()
                else -> null
            }
            "EXCL" -> (operand as? Boolean)?.let { !it }
            else -> null
        }
    }

    private fun evaluateParensExpression(node: JavaSyntaxNode): Any? {
        // Find the expression inside parentheses
        val innerExpr = node.children.firstOrNull {
            it.type.toString() !in listOf("LPARENTH", "RPARENTH", "WHITE_SPACE")
        } ?: return null
        return evaluate(innerExpr)
    }

    private fun evaluateReferenceExpression(node: JavaSyntaxNode): Any? {
        val refText = node.text
        
        // Handle qualified references like ClassName.FIELD or just FIELD
        val lastDot = refText.lastIndexOf('.')
        
        if (lastDot < 0) {
            // Simple name - look in containing class first
            val localValue = resolveFieldValue(containingClass, refText)
            if (localValue != null) return localValue
            
            // Try external resolution (e.g., static import from Kotlin)
            return resolveExternalReference?.invoke(null, refText)
        }
        
        val className = refText.substring(0, lastDot)
        val fieldName = refText.substring(lastDot + 1)
        
        // Try to find the class locally (same file)
        val targetClass = findLocalClass(className)
        if (targetClass != null) {
            return resolveFieldValue(targetClass, fieldName)
        }
        
        // Try external class references (e.g., Kotlin classes) via callback
        return resolveExternalReference?.invoke(className, fieldName)
    }

    private fun findLocalClass(name: String): JavaClassOverAst? {
        // Check if it's the containing class itself
        if (containingClass.name.asString() == name) {
            return containingClass
        }
        
        // Check inner classes
        val innerClass = containingClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier(name))
        if (innerClass is JavaClassOverAst) {
            return innerClass
        }
        
        // Check classes in the same file (siblings)
        val root = containingClass.node.parent ?: return null
        for (child in root.children) {
            if (child.type.toString() == "CLASS") {
                val className = child.findChildByType("IDENTIFIER")?.text
                if (className == name) {
                    return JavaClassOverAst(child, containingClass.resolutionContext)
                }
            }
        }
        
        return null
    }

    private fun resolveFieldValue(javaClass: JavaClassOverAst, fieldName: String): Any? {
        val field = javaClass.fields.find { it.name.asString() == fieldName } ?: return null
        return field.initializerValue
    }

    companion object {
        private fun parseIntegerLiteral(text: String): Any {
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

        private fun parseLongLiteral(text: String): Long {
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

        private fun parseFloatLiteral(text: String): Float {
            val cleaned = text.replace("_", "").removeSuffix("F").removeSuffix("f")
            return cleaned.toFloatOrNull() ?: 0f
        }

        private fun parseDoubleLiteral(text: String): Double {
            val cleaned = text.replace("_", "").removeSuffix("D").removeSuffix("d")
            return cleaned.toDoubleOrNull() ?: 0.0
        }

        private fun String.unescapeJavaString(): String {
            val sb = StringBuilder()
            var i = 0
            while (i < length) {
                if (this[i] == '\\' && i + 1 < length) {
                    when (this[i + 1]) {
                        'n' -> { sb.append('\n'); i += 2 }
                        't' -> { sb.append('\t'); i += 2 }
                        'r' -> { sb.append('\r'); i += 2 }
                        'b' -> { sb.append('\b'); i += 2 }
                        'f' -> { sb.append('\u000C'); i += 2 }
                        '\'' -> { sb.append('\''); i += 2 }
                        '"' -> { sb.append('"'); i += 2 }
                        '\\' -> { sb.append('\\'); i += 2 }
                        'u' -> {
                            if (i + 5 < length) {
                                val hex = substring(i + 2, i + 6)
                                val code = hex.toIntOrNull(16)
                                if (code != null) {
                                    sb.append(code.toChar())
                                    i += 6
                                } else {
                                    sb.append(this[i])
                                    i++
                                }
                            } else {
                                sb.append(this[i])
                                i++
                            }
                        }
                        in '0'..'7' -> {
                            var end = i + 2
                            while (end < length && end < i + 4 && this[end] in '0'..'7') end++
                            val octal = substring(i + 1, end)
                            val code = octal.toIntOrNull(8)
                            if (code != null) {
                                sb.append(code.toChar())
                                i = end
                            } else {
                                sb.append(this[i])
                                i++
                            }
                        }
                        else -> { sb.append(this[i]); i++ }
                    }
                } else {
                    sb.append(this[i])
                    i++
                }
            }
            return sb.toString()
        }
    }
}
