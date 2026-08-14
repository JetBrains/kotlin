/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.element.SyntaxElementTypes
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.model.JavaPrimitiveTypeOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.findClassInCurrentScope
import org.jetbrains.kotlin.java.direct.resolution.getStaticImport
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.name.Name
import kotlin.experimental.inv

/**
 * Evaluates the constant expressions of the JLS 15.29 subset: Java field initializers as well as
 * annotation arguments and annotation-method default values (JLS 9.6.1 / 9.7).
 *
 * @param containingClass the class the expression is written in, if any; simple names are looked up
 *   among its fields first.
 * @param resolveExternalReference optional callback to resolve references to external classes (e.g., Kotlin classes)
 */
class ConstantEvaluator private constructor(
    private val tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
    private val containingClass: JavaClassOverAst?,
    private val resolveExternalReference: ((classQualifier: String?, fieldName: String) -> Any?)?,
) {
    /** Evaluator for an expression written inside [containingClass], i.e. a field initializer. */
    constructor(
        containingClass: JavaClassOverAst,
        resolveExternalReference: ((classQualifier: String?, fieldName: String) -> Any?)? = null,
    ) : this(containingClass.tree, containingClass.resolutionContext, containingClass, resolveExternalReference)

    /**
     * Evaluator for an expression written outside of any member — an annotation argument or an
     * annotation-method default value.
     */
    constructor(
        tree: JavaLightTree,
        resolutionContext: JavaResolutionContext,
        resolveExternalReference: ((classQualifier: String?, fieldName: String) -> Any?)? = null,
    ) : this(tree, resolutionContext, resolutionContext.scopeContext.containingClass as? JavaClassOverAst, resolveExternalReference)

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
            JavaSyntaxElementType.TYPE_CAST_EXPRESSION -> evaluateTypeCastExpression(node)
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

    private fun evaluateTypeCastExpression(node: JavaLightNode): Any? {
        val children = tree.getChildren(node)
        val typeNode = children.firstOrNull { tree.getType(it) == JavaSyntaxElementType.TYPE } ?: return null
        val rparenthIndex = children.indexOfFirst { tree.getType(it) == JavaSyntaxTokenType.RPARENTH }
        if (rparenthIndex < 0) return null
        val operand = children.getOrNull(rparenthIndex + 1) ?: return null

        val value = evaluate(operand) ?: return null
        val primitiveNode = tree.getChildren(typeNode).firstOrNull {
            tree.getType(it) in SyntaxElementTypes.PRIMITIVE_TYPE_BIT_SET
        } ?: return value
        val primitive = JavaPrimitiveTypeOverAst(primitiveNode, tree, resolutionContext).type ?: return value
        return JavaLiteralParser.coerceToPrimitive(value, primitive)
    }

    private fun evaluateReferenceExpression(node: JavaLightNode): Any? {
        val refText = tree.getText(node).toString()

        val lastDot = refText.lastIndexOf('.')

        val className: String
        val fieldName: String
        if (lastDot < 0) {
            val localValue = containingClass?.let { resolveFieldValue(it, refText) }
            if (localValue != null) return localValue

            val staticImportFqn = with(resolutionContext) { getStaticImport(refText) }?.asString()
            val importDot = staticImportFqn?.lastIndexOf('.') ?: -1
            if (staticImportFqn == null || importDot < 0) {
                return resolveExternalReference?.invoke(null, refText)
            }
            className = staticImportFqn.substring(0, importDot)
            fieldName = staticImportFqn.substring(importDot + 1)
        } else {
            className = refText.substring(0, lastDot)
            fieldName = refText.substring(lastDot + 1)
        }

        val targetClass = findLocalClass(className)
        if (targetClass != null) {
            return resolveFieldValue(targetClass, fieldName)
        }

        // Promote a simple class name to its FQN (via the unit's imports + same-package + java.lang
        // probes) before passing to the cross-language callback. Without this, `SdkConstants.R_CLASS`
        // — written under `import com.android.SdkConstants;` in a Java source — would reach the
        // callback as the simple name `"SdkConstants"`, which it can only interpret as either a
        // current-package class or a `<root>.SdkConstants` top-level class — neither of which
        // exists. Passing the resolved FQN `"com.android.SdkConstants"` lets the callback construct
        // `ClassId(com.android, SdkConstants)` and look up the `FirJavaField` on the binary class.
        val resolvedClassQualifier = if (className.contains('.')) {
            className
        } else {
            with(resolutionContext) { resolve(className) }?.asSingleFqName()?.asString() ?: className
        }

        return resolveExternalReference?.invoke(resolvedClassQualifier, fieldName)
    }

    private fun findLocalClass(name: String): JavaClassOverAst? {
        if (containingClass != null && containingClass.name.asString() == name) {
            return containingClass
        }
        // Route through the shared resolution context so that sibling top-level classes are
        // retrieved from the file-level cache (same JavaClassOverAst instance, same type-parameter
        // identity) instead of being freshly constructed here.
        return with(resolutionContext) { findClassInCurrentScope(name) } as? JavaClassOverAst
    }

    private fun resolveFieldValue(javaClass: JavaClassOverAst, fieldName: String): Any? {
        val field = javaClass.fields.find { it.name.asString() == fieldName } ?: return null
        return field.initializerValue
    }
}
