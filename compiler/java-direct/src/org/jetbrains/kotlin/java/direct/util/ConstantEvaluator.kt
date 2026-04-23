/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.name.Name
import kotlin.experimental.inv

/**
 * Evaluates constant expressions in Java field initializers.
 *
 * @param containingClass the class containing the field being evaluated
 * @param resolveExternalReference optional callback to resolve references to external classes (e.g., Kotlin classes)
 */
class ConstantEvaluator(
    private val containingClass: JavaClassOverAst,
    private val resolveExternalReference: ((classQualifier: String?, fieldName: String) -> Any?)? = null,
) {
    private val tree: JavaLightTree get() = containingClass.tree

    /**
     * Evaluates a constant expression node and returns the computed value.
     * Returns null if the expression cannot be evaluated as a constant.
     */
    fun evaluate(node: JavaLightNode): Any? {
        return when (tree.getType(node)) {
            JavaSyntaxElementType.LITERAL_EXPRESSION -> evaluateLiteral(node)
            JavaSyntaxElementType.BINARY_EXPRESSION -> evaluateBinaryExpression(node)
            JavaSyntaxElementType.PREFIX_EXPRESSION -> evaluatePrefixExpression(node)
            JavaSyntaxElementType.PARENTH_EXPRESSION -> evaluateParensExpression(node)
            JavaSyntaxElementType.REFERENCE_EXPRESSION -> evaluateReferenceExpression(node)
            JavaSyntaxElementType.POLYADIC_EXPRESSION -> evaluatePolyadicExpression(node)
            else -> null
        }
    }

    private fun evaluateLiteral(node: JavaLightNode): Any? =
        JavaLiteralParser.evaluateLiteral(node, tree)

    private fun evaluateBinaryExpression(node: JavaLightNode): Any? {
        val children = tree.getChildren(node)
        if (children.size < 3) return null

        val lhs = evaluate(children[0]) ?: return null
        val operator = tree.getType(children[1])
        val rhs = evaluate(children[2]) ?: return null

        return evaluateBinaryOp(lhs, operator, rhs)
    }

    private fun evaluatePolyadicExpression(node: JavaLightNode): Any? {
        val children = tree.getChildren(node)
        if (children.size < 3) return null

        var result = evaluate(children[0]) ?: return null
        var i = 1
        while (i < children.size - 1) {
            val operator = tree.getType(children[i])
            val operand = evaluate(children[i + 1]) ?: return null
            result = evaluateBinaryOp(result, operator, operand) ?: return null
            i += 2
        }
        return result
    }

    private fun evaluateBinaryOp(lhs: Any, operator: SyntaxElementType, rhs: Any): Any? {
        if (lhs is String && operator == JavaSyntaxTokenType.PLUS) {
            return lhs + rhs.toString()
        }
        if (rhs is String && operator == JavaSyntaxTokenType.PLUS) {
            return lhs.toString() + rhs
        }

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

        if (lhs is Number && rhs is Number) {
            return JavaLiteralParser.evaluateNumericBinaryOp(lhs, operator, rhs)
        }

        if (lhs is Char && rhs is Number) {
            return JavaLiteralParser.evaluateNumericBinaryOp(lhs.code, operator, rhs)?.let { result ->
                if (result is Int && operator == JavaSyntaxTokenType.PLUS) result.toChar() else result
            }
        }
        if (lhs is Number && rhs is Char) {
            return JavaLiteralParser.evaluateNumericBinaryOp(lhs, operator, rhs.code)
        }
        if (lhs is Char && rhs is Char) {
            return JavaLiteralParser.evaluateNumericBinaryOp(lhs.code, operator, rhs.code)
        }

        return null
    }

    private fun evaluatePrefixExpression(node: JavaLightNode): Any? {
        val children = tree.getChildren(node)
        if (children.size < 2) return null

        val operator = tree.getType(children[0])
        val operand = evaluate(children[1]) ?: return null

        return when (operator) {
            JavaSyntaxTokenType.MINUS -> when (operand) {
                is Int -> -operand
                is Long -> -operand
                is Float -> -operand
                is Double -> -operand
                else -> null
            }
            JavaSyntaxTokenType.PLUS -> operand
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

    private fun evaluateParensExpression(node: JavaLightNode): Any? {
        val innerExpr = tree.getChildren(node).firstOrNull {
            val t = tree.getType(it)
            t != JavaSyntaxTokenType.LPARENTH && t != JavaSyntaxTokenType.RPARENTH
        } ?: return null
        return evaluate(innerExpr)
    }

    private fun evaluateReferenceExpression(node: JavaLightNode): Any? {
        val refText = tree.getText(node).toString()

        val lastDot = refText.lastIndexOf('.')

        if (lastDot < 0) {
            val localValue = resolveFieldValue(containingClass, refText)
            if (localValue != null) return localValue

            return resolveExternalReference?.invoke(null, refText)
        }

        val className = refText.substring(0, lastDot)
        val fieldName = refText.substring(lastDot + 1)

        val targetClass = findLocalClass(className)
        if (targetClass != null) {
            return resolveFieldValue(targetClass, fieldName)
        }

        return resolveExternalReference?.invoke(className, fieldName)
    }

    private fun findLocalClass(name: String): JavaClassOverAst? {
        if (containingClass.name.asString() == name) {
            return containingClass
        }
        // Route through the shared resolution context so that sibling top-level classes are
        // retrieved from the file-level cache (same JavaClassOverAst instance, same type-parameter
        // identity) instead of being freshly constructed here.
        return containingClass.resolutionContext.findLocalClass(Name.identifier(name)) as? JavaClassOverAst
    }

    private fun resolveFieldValue(javaClass: JavaClassOverAst, fieldName: String): Any? {
        val field = javaClass.fields.find { it.name.asString() == fieldName } ?: return null
        return field.initializerValue
    }
}
