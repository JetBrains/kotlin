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
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.model.JavaPrimitiveTypeOverAst
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.classifierAdapterFor
import org.jetbrains.kotlin.java.direct.resolution.getStaticImport
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.java.direct.resolution.resolveImportedClassName
import org.jetbrains.kotlin.java.direct.resolution.resolveExternalFieldValue
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.experimental.inv

/**
 * Evaluates the constant expressions of the JLS 15.29 subset: Java field initializers as well as
 * annotation arguments and annotation-method default values (JLS 9.6.1 / 9.7).
 *
 * @param containingClass the class the expression is written in, if any; simple names are looked up
 *   among its fields first.
 * @param customExternalConstantResolver optional override for external constant resolution,
 *   primarily used by tests.
 */
class ConstantEvaluator private constructor(
    private val tree: LightSyntaxTree,
    private val resolutionContext: JavaResolutionContext,
    private val containingClass: JavaClassOverAst?,
    private val customExternalConstantResolver: ExternalConstantResolver?,
) {
    /**
     * Override for external constant resolution, primarily used by tests. Calls use one of these
     * argument shapes:
     * - `(null, null, fieldName)` for an unqualified reference;
     * - `(qualifier, classId, fieldName)` when the qualifier resolves to a class; the text is
     *   informational and the class ID is authoritative;
     * - `(qualifier, null, fieldName)` when the qualifier does not resolve to a class, which can
     *   still name a JVM facade class and must be split at its last dot.
     *
     * A null qualifier is never paired with a class ID. Production resolution is performed
     * directly through [resolveExternalFieldValue] when no test hook is supplied.
     */
    fun interface ExternalConstantResolver {
        fun resolve(classQualifier: String?, qualifierClassId: ClassId?, fieldName: String): Any?
    }

    /** Evaluator for an expression written inside [containingClass], i.e. a field initializer. */
    constructor(
        containingClass: JavaClassOverAst,
        customExternalConstantResolver: ExternalConstantResolver? = null,
    ) : this(containingClass.tree, containingClass.resolutionContext, containingClass, customExternalConstantResolver)

    /**
     * Evaluator for an expression written outside of any member — an annotation argument or an
     * annotation-method default value.
     */
    constructor(
        tree: LightSyntaxTree,
        resolutionContext: JavaResolutionContext,
        customExternalConstantResolver: ExternalConstantResolver? = null,
    ) : this(tree, resolutionContext, resolutionContext.scopeContext.containingClass as? JavaClassOverAst, customExternalConstantResolver)

    /**
     * Evaluates a constant expression node and returns the computed value.
     * Returns null if the expression cannot be evaluated as a constant.
     */
    fun evaluate(node: LightNode): Any? {
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

    private fun evaluateLiteral(node: LightNode): Any? =
        JavaLiteralParser.evaluateLiteral(node, tree)

    private fun evaluateBinaryExpression(node: LightNode): Any? {
        val children = tree.getChildren(node)
        if (children.size < 3) return null

        val lhs = evaluate(children[0]) ?: return null
        val operator = tree.getType(children[1])
        val rhs = evaluate(children[2]) ?: return null

        return evaluateBinaryOp(lhs, operator, rhs)
    }

    private fun evaluatePolyadicExpression(node: LightNode): Any? {
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

    private fun evaluatePrefixExpression(node: LightNode): Any? {
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

    private fun evaluateParensExpression(node: LightNode): Any? {
        val innerExpr = tree.getChildren(node).firstOrNull {
            val t = tree.getType(it)
            t != JavaSyntaxTokenType.LPARENTH && t != JavaSyntaxTokenType.RPARENTH
        } ?: return null
        return evaluate(innerExpr)
    }

    private fun evaluateTypeCastExpression(node: LightNode): Any? {
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

    private fun evaluateReferenceExpression(node: LightNode): Any? {
        val refText = tree.getText(node).toString()

        val lastDot = refText.lastIndexOf('.')

        val className: String
        val fieldName: String
        if (lastDot < 0) {
            return resolveUnqualifiedFieldValue(refText)?.value
        } else {
            className = refText.substring(0, lastDot)
            fieldName = refText.substring(lastDot + 1)
        }

        // Resolve the class qualifier as a type name (via the unit's imports + same-package + java.lang
        // probes, JLS 6.5.5) before passing it to the cross-language callback. Without this,
        // `SdkConstants.R_CLASS` — written under `import com.android.SdkConstants;` in a Java source —
        // could only be interpreted as either a current-package class or a `<root>.SdkConstants`
        // top-level class — neither of which exists. A qualified name like `p.X.Y` is classified left
        // to right (JLS 6.5.4.2): it is the nested class `p.X.Y` if `p.X` is a class, never a class `Y`
        // of a package `p.X`, which splitting the string at its last dot would suggest.
        val resolvedClassId = resolve(className, c = resolutionContext)
        if (resolvedClassId != null) {
            val javaClass = classifierAdapterFor(resolvedClassId, c = resolutionContext)
            findFieldValue(javaClass, fieldName, mutableSetOf())?.let { return it.value }
        }
        return resolveExternalReference(className, resolvedClassId, fieldName)
    }

    private fun resolveExternalReference(classQualifier: String?, qualifierClassId: ClassId?, fieldName: String): Any? {
        customExternalConstantResolver?.let { return it.resolve(classQualifier, qualifierClassId, fieldName) }
        return resolutionContext.fileContext.session.resolveExternalFieldValue(
            classQualifier,
            qualifierClassId,
            fieldName,
            resolutionContext.packageFqName,
        )
    }

    private fun resolveUnqualifiedFieldValue(fieldName: String): FieldValue? {
        val visited = mutableSetOf<ClassId>()
        var currentClass: JavaClass? = containingClass
        while (currentClass != null) {
            findFieldValue(currentClass, fieldName, visited)?.let { return it }
            currentClass = currentClass.outerClass
        }

        getStaticImport(fieldName, c = resolutionContext)?.let { importedField ->
            findImportedFieldValue(importedField.parent(), importedField.shortName().asString())?.let { return it }
            val className = importedField.parent().asString()
            resolveExternalReference(className, resolve(className, c = resolutionContext), importedField.shortName().asString())
                ?.let { return FieldValue(it) }
        }

        for (className in resolutionContext.fileContext.imports.staticStarImports) {
            findImportedFieldValue(className, fieldName)?.let { return it }
        }
        return resolveExternalReference(null, null, fieldName)?.let(::FieldValue)
    }

    private fun findImportedFieldValue(className: FqName, fieldName: String): FieldValue? {
        val classId = resolveImportedClassName(className, c = resolutionContext) ?: return null
        val javaClass = classifierAdapterFor(classId, c = resolutionContext) ?: return null
        return findFieldValue(javaClass, fieldName, mutableSetOf())
    }

    private fun findFieldValue(javaClass: JavaClass?, fieldName: String, visited: MutableSet<ClassId>): FieldValue? {
        javaClass ?: return null
        val classId = javaClass.classId ?: return null
        if (!visited.add(classId)) return null

        if (javaClass is JavaClassOverAst) {
            val field = javaClass.fields.firstOrNull { it.name.asString() == fieldName }
            if (field != null) return FieldValue(field.initializerValue)
        } else {
            val value = resolveExternalReference(classId.asSingleFqName().asString(), classId, fieldName)
            if (value != null) return FieldValue(value)
            val declaredField = resolutionContext.fileContext.session.getClassDeclaredPropertySymbols(classId, Name.identifier(fieldName)).any()
            if (declaredField) return FieldValue(null)
        }

        for (supertype in javaClass.supertypes) {
            val superClass = supertype.classifier as? JavaClass ?: continue
            findFieldValue(superClass, fieldName, visited)?.let { return it }
        }
        return null
    }

    private data class FieldValue(val value: Any?)
}
