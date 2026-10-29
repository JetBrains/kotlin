/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.JavaSyntaxDefinition
import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.element.SyntaxElementTypes
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.model.JavaPrimitiveTypeOverAst
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.classifierAdapterFor
import org.jetbrains.kotlin.java.direct.resolution.getStaticImport
import org.jetbrains.kotlin.java.direct.resolution.hasEnumEntry
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.java.direct.resolution.resolveImportedClassName
import org.jetbrains.kotlin.java.direct.resolution.resolveExternalFieldValue
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.experimental.inv

internal data class StaticMemberReference(
    val qualifier: String,
    val qualifierClassId: ClassId?,
    val memberName: String,
)

internal sealed interface MemberResolutionResult {
    data class EnumEntry(val enumClassId: ClassId, val entryName: Name) : MemberResolutionResult
    data class Constant(val value: Any) : MemberResolutionResult

    /** A field that is found but is not a constant variable; it still shadows later scopes (JLS 6.4.1). */
    data object NonConstantField : MemberResolutionResult
    data class Unresolved(val reference: StaticMemberReference? = null) : MemberResolutionResult
}

/** Extracts the identifier components of a simple or qualified reference expression. */
internal fun LightSyntaxTree.referenceNameParts(node: LightNode): List<String>? {
    val parts = mutableListOf<String>()

    fun collect(current: LightNode): Boolean {
        for (child in getChildren(current)) {
            val type = getType(child)
            when {
                type == JavaSyntaxTokenType.IDENTIFIER -> parts += getText(child).toString()
                type == JavaSyntaxTokenType.DOT ||
                        type == JavaSyntaxElementType.REFERENCE_PARAMETER_LIST ||
                        type == SyntaxTokenTypes.WHITE_SPACE ||
                        type in JavaSyntaxDefinition.comments -> Unit
                type == JavaSyntaxElementType.REFERENCE_EXPRESSION ||
                        type == JavaSyntaxElementType.JAVA_CODE_REFERENCE -> if (!collect(child)) return false
                else -> return false
            }
        }
        return true
    }

    return parts.takeIf { collect(node) && it.isNotEmpty() }
}

/**
 * A constant or enum-entry reference has the form `Identifier` or `TypeName.Identifier`, so split
 * only at the final identifier. [resolve] classifies the qualifier left to right.
 */
internal fun resolveStaticMemberReference(referenceParts: List<String>, resolutionContext: JavaResolutionContext): StaticMemberReference? {
    if (referenceParts.isEmpty()) return null
    if (referenceParts.size == 1) {
        val importedMember = getStaticImport(referenceParts.single(), c = resolutionContext) ?: return null
        val qualifier = importedMember.parent()
        return StaticMemberReference(
            qualifier.asString(),
            resolveImportedClassName(qualifier, c = resolutionContext),
            importedMember.shortName().asString(),
        )
    }

    val qualifierParts = referenceParts.dropLast(1)
    val qualifier = qualifierParts.joinToString(".")
    return StaticMemberReference(
        qualifier,
        resolve(qualifier, c = resolutionContext),
        referenceParts.last(),
    )
}

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
     * - `(qualifier, classId, fieldName)` when the qualifier resolves to a class; the text is
     *   informational and the class ID is authoritative;
     * - `(qualifier, null, fieldName)` when the qualifier does not resolve to a class, which can
     *   still name a JVM facade class and must be split at its last dot.
     *
     * Simple names are never resolved externally. Production resolution is performed directly
     * through [resolveExternalFieldValue] when no test hook is supplied.
     */
    fun interface ExternalConstantResolver {
        fun resolve(classQualifier: String, qualifierClassId: ClassId?, fieldName: String): Any?
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

    private fun evaluateReferenceExpression(node: LightNode): Any? =
        (resolveReference(node) as? MemberResolutionResult.Constant)?.value

    /** Resolves a reference expression node to the static member it denotes. */
    internal fun resolveReference(node: LightNode): MemberResolutionResult {
        val nameParts = tree.referenceNameParts(node) ?: return MemberResolutionResult.Unresolved()
        if (nameParts.size == 1) return resolveSimpleName(nameParts.single()) ?: MemberResolutionResult.Unresolved()
        val reference = resolveStaticMemberReference(nameParts, resolutionContext)
            ?: return MemberResolutionResult.Unresolved()

        // Resolve the class qualifier as a type name (via the unit's imports + same-package + java.lang
        // probes, JLS 6.5.5) before passing it to the cross-language callback. Without this,
        // `SdkConstants.R_CLASS` — written under `import com.android.SdkConstants;` in a Java source —
        // could only be interpreted as either a current-package class or a `<root>.SdkConstants`
        // top-level class — neither of which exists. A qualified name like `p.X.Y` is classified left
        // to right (JLS 6.5.4.2): it is the nested class `p.X.Y` if `p.X` is a class, never a class `Y`
        // of a package `p.X`, which splitting the string at its last dot would suggest.
        val resolvedClassId = reference.qualifierClassId
        if (resolvedClassId != null) {
            val javaClass = classifierAdapterFor(resolvedClassId, c = resolutionContext)
            if (javaClass != null) {
                return findMember(javaClass, reference.memberName, mutableSetOf())
                    ?: MemberResolutionResult.Unresolved(reference)
            }
        }
        return resolveExternalReference(reference.qualifier, resolvedClassId, reference.memberName)
            ?: MemberResolutionResult.Unresolved(reference)
    }

    private fun resolveExternalReference(classQualifier: String, qualifierClassId: ClassId?, fieldName: String): MemberResolutionResult? {
        val resolver = customExternalConstantResolver
        val value = if (resolver != null) {
            resolver.resolve(classQualifier, qualifierClassId, fieldName)
        } else {
            resolutionContext.fileContext.session.resolveExternalFieldValue(
                classQualifier,
                qualifierClassId,
                fieldName,
                resolutionContext.packageFqName,
            )
        }
        return value?.let(MemberResolutionResult::Constant)
    }

    private fun resolveSimpleName(name: String): MemberResolutionResult? {
        val visited = mutableSetOf<ClassId>()
        var currentClass: JavaClass? = containingClass
        while (currentClass != null) {
            findMember(currentClass, name, visited)?.let { return it }
            currentClass = currentClass.outerClass
        }

        var unresolvedImport: StaticMemberReference? = null
        resolveStaticMemberReference(listOf(name), resolutionContext)?.let { imported ->
            val javaClass = imported.qualifierClassId?.let { classifierAdapterFor(it, c = resolutionContext) }
            if (javaClass != null) {
                findMember(javaClass, imported.memberName, mutableSetOf())?.let { return it }
                unresolvedImport = imported
            } else {
                resolveExternalReference(imported.qualifier, imported.qualifierClassId, imported.memberName)?.let { return it }
                unresolvedImport = imported
            }
        }

        for (className in resolutionContext.fileContext.imports.staticStarImports) {
            val classId = resolveImportedClassName(className, c = resolutionContext) ?: continue
            val javaClass = classifierAdapterFor(classId, c = resolutionContext) ?: continue
            findMember(javaClass, name, mutableSetOf())?.let { return it }
        }
        return unresolvedImport?.let(MemberResolutionResult::Unresolved)
    }

    /** Looks [memberName] up among the fields of [javaClass] and then of its supertypes (JLS 8.3). */
    private fun findMember(javaClass: JavaClass, memberName: String, visited: MutableSet<ClassId>): MemberResolutionResult? {
        val classId = javaClass.classId ?: return null
        if (!visited.add(classId)) return null

        if (javaClass is JavaClassOverAst) {
            val field = javaClass.fields.firstOrNull { it.name.asString() == memberName }
            if (field != null) {
                if (field.isEnumEntry) return MemberResolutionResult.EnumEntry(classId, field.name)
                return field.initializerValue?.let(MemberResolutionResult::Constant)
                    ?: MemberResolutionResult.NonConstantField
            }
        } else {
            val session = resolutionContext.fileContext.session
            if (session.hasEnumEntry(classId, memberName)) {
                return MemberResolutionResult.EnumEntry(classId, Name.identifier(memberName))
            }
            resolveExternalReference(classId.asSingleFqName().asString(), classId, memberName)?.let { return it }
            if (session.getClassDeclaredPropertySymbols(classId, Name.identifier(memberName)).any()) {
                return MemberResolutionResult.NonConstantField
            }
        }

        for (supertype in javaClass.supertypes) {
            val superClass = supertype.classifier as? JavaClass ?: continue
            findMember(superClass, memberName, visited)?.let { return it }
        }
        return null
    }
}
