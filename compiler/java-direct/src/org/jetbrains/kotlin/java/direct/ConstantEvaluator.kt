/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
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
        return when (node.type) {
            JavaSyntaxElementType.LITERAL_EXPRESSION -> evaluateLiteral(node)
            JavaSyntaxElementType.BINARY_EXPRESSION -> evaluateBinaryExpression(node)
            JavaSyntaxElementType.PREFIX_EXPRESSION -> evaluatePrefixExpression(node)
            JavaSyntaxElementType.PARENTH_EXPRESSION -> evaluateParensExpression(node)
            JavaSyntaxElementType.REFERENCE_EXPRESSION -> evaluateReferenceExpression(node)
            JavaSyntaxElementType.POLYADIC_EXPRESSION -> evaluatePolyadicExpression(node)
            else -> null
        }
    }

    private fun evaluateLiteral(node: JavaSyntaxNode): Any? {
        val literalChild = node.children.firstOrNull() ?: return null
        val text = literalChild.text

        return when (literalChild.type) {
            JavaSyntaxTokenType.STRING_LITERAL -> {
                if (text.length >= 2) {
                    JavaLiteralParser.unescapeJavaString(text.substring(1, text.length - 1))
                } else text
            }
            JavaSyntaxTokenType.CHARACTER_LITERAL -> {
                if (text.length >= 3) {
                    JavaLiteralParser.unescapeJavaString(text.substring(1, text.length - 1)).firstOrNull()
                } else null
            }
            JavaSyntaxTokenType.TRUE_KEYWORD -> true
            JavaSyntaxTokenType.FALSE_KEYWORD -> false
            JavaSyntaxTokenType.NULL_KEYWORD -> null
            JavaSyntaxTokenType.INTEGER_LITERAL -> JavaLiteralParser.parseIntegerLiteral(text)
            JavaSyntaxTokenType.LONG_LITERAL -> JavaLiteralParser.parseLongLiteral(text)
            JavaSyntaxTokenType.FLOAT_LITERAL -> JavaLiteralParser.parseFloatLiteral(text)
            JavaSyntaxTokenType.DOUBLE_LITERAL -> JavaLiteralParser.parseDoubleLiteral(text)
            else -> null
        }
    }

    private fun evaluateBinaryExpression(node: JavaSyntaxNode): Any? {
        val children = node.children.filter { it.type != SyntaxTokenTypes.WHITE_SPACE }
        if (children.size < 3) return null

        val lhs = evaluate(children[0]) ?: return null
        val operator = children[1].type
        val rhs = evaluate(children[2]) ?: return null

        return evaluateBinaryOp(lhs, operator, rhs)
    }

    private fun evaluatePolyadicExpression(node: JavaSyntaxNode): Any? {
        // Polyadic expression: a + b + c + d (multiple operands with same operator)
        val children = node.children.filter { it.type != SyntaxTokenTypes.WHITE_SPACE }
        if (children.size < 3) return null

        var result = evaluate(children[0]) ?: return null
        var i = 1
        while (i < children.size - 1) {
            val operator = children[i].type
            val operand = evaluate(children[i + 1]) ?: return null
            result = evaluateBinaryOp(result, operator, operand) ?: return null
            i += 2
        }
        return result
    }

    private fun evaluateBinaryOp(lhs: Any, operator: SyntaxElementType, rhs: Any): Any? {
        // String concatenation
        if (lhs is String && operator == JavaSyntaxTokenType.PLUS) {
            return lhs + rhs.toString()
        }
        if (rhs is String && operator == JavaSyntaxTokenType.PLUS) {
            return lhs.toString() + rhs
        }

        // Boolean operations
        if (lhs is Boolean && rhs is Boolean) {
            return when (operator) {
                JavaSyntaxTokenType.ANDAND -> lhs && rhs
                JavaSyntaxTokenType.OROR -> lhs || rhs
                JavaSyntaxTokenType.EQEQ -> lhs == rhs
                JavaSyntaxTokenType.NE -> lhs != rhs
                JavaSyntaxTokenType.XOR -> lhs xor rhs
                JavaSyntaxTokenType.AND -> lhs and rhs
                JavaSyntaxTokenType.OR -> lhs or rhs
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
                if (result is Int && operator == JavaSyntaxTokenType.PLUS) result.toChar() else result
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

    private fun evaluateNumericOp(lhs: Number, operator: SyntaxElementType, rhs: Number): Any? {
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

    private fun evaluatePrefixExpression(node: JavaSyntaxNode): Any? {
        val children = node.children.filter { it.type != SyntaxTokenTypes.WHITE_SPACE }
        if (children.size < 2) return null

        val operator = children[0].type
        val operand = evaluate(children[1]) ?: return null

        return when (operator) {
            JavaSyntaxTokenType.MINUS -> when (operand) {
                is Int -> -operand
                is Long -> -operand
                is Float -> -operand
                is Double -> -operand
                else -> null
            }
            JavaSyntaxTokenType.PLUS -> operand // Unary plus is identity
            JavaSyntaxTokenType.TILDE -> when (operand) {
                is Int -> operand.inv()
                is Long -> operand.inv()
                is Short -> operand.inv()
                is Byte -> operand.inv()
                else -> null
            }
            JavaSyntaxTokenType.EXCL -> (operand as? Boolean)?.let { !it }
            else -> null
        }
    }

    private fun evaluateParensExpression(node: JavaSyntaxNode): Any? {
        // Find the expression inside parentheses
        val innerExpr = node.children.firstOrNull {
            it.type != JavaSyntaxTokenType.LPARENTH && it.type != JavaSyntaxTokenType.RPARENTH && it.type != SyntaxTokenTypes.WHITE_SPACE
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
            if (child.type == JavaSyntaxElementType.CLASS) {
                val className = child.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text
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

}
