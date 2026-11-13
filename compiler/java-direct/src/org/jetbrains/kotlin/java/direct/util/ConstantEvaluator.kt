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
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.model.JavaPrimitiveTypeOverAst
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.classifierAdapterFor
import org.jetbrains.kotlin.java.direct.resolution.getSimpleImport
import org.jetbrains.kotlin.java.direct.resolution.getStaticImport
import org.jetbrains.kotlin.java.direct.resolution.hasEnumEntry
import org.jetbrains.kotlin.java.direct.resolution.isInheritedTo
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.java.direct.resolution.resolveExternalFieldValue
import org.jetbrains.kotlin.java.direct.resolution.resolveImportedClassName
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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

/** The syntax kind of a reference whose identifier components [collectReferenceNameParts] extracts. */
private enum class ReferenceSyntax {
    /**
     * A reference expression such as `a.b.C`: nested qualifiers are `REFERENCE_EXPRESSION`s, and any
     * other child (call, array access, literal) means the expression is not a name, so it fails.
     */
    EXPRESSION,

    /**
     * A type reference such as `java.util.@NotNull List<T>`: nested qualifiers are `JAVA_CODE_REFERENCE`s;
     * other children (type-use annotations, error elements) are skipped and never fail.
     */
    TYPE,
}

/** Extracts identifier components of a [syntax] reference; `null` if it is not a name. */
private fun LightSyntaxTree.collectReferenceNameParts(node: LightNode, syntax: ReferenceSyntax): List<String>? {
    val parts = mutableListOf<String>()

    fun collect(current: LightNode): Boolean {
        for (child in getChildren(current)) {
            when (getType(child)) {
                JavaSyntaxTokenType.IDENTIFIER -> parts += getText(child).toString()
                JavaSyntaxElementType.JAVA_CODE_REFERENCE -> if (!collect(child)) return false
                JavaSyntaxElementType.REFERENCE_EXPRESSION -> if (syntax == EXPRESSION && !collect(child)) return false

                JavaSyntaxTokenType.DOT,
                JavaSyntaxElementType.REFERENCE_PARAMETER_LIST,
                SyntaxTokenTypes.WHITE_SPACE,
                in JavaSyntaxDefinition.comments
                    -> Unit

                else -> if (syntax == EXPRESSION) return false
            }
        }
        return true
    }

    return parts.takeIf { collect(node) }
}

/** Extracts the identifier components of a simple or qualified reference expression. */
internal fun LightSyntaxTree.referenceNameParts(node: LightNode): List<String>? =
    collectReferenceNameParts(node, ReferenceSyntax.EXPRESSION)?.takeIf { it.isNotEmpty() }

/** Extracts type-name segments, skipping syntax outside nested Java code references. */
internal fun LightSyntaxTree.typeReferenceNameParts(node: LightNode): List<String> =
    collectReferenceNameParts(node, ReferenceSyntax.TYPE).orEmpty()

/**
 * A constant or enum-entry reference has the form `Identifier` or `TypeName.Identifier`, so split
 * only at the final identifier. [resolve] classifies the qualifier left to right.
 */
private fun resolveQualifiedMemberReference(referenceParts: List<String>, resolutionContext: JavaResolutionContext): StaticMemberReference {
    val qualifierParts = referenceParts.dropLast(1)
    val qualifier = qualifierParts.joinToString(".")
    return StaticMemberReference(
        qualifier,
        resolve(qualifier, c = resolutionContext),
        referenceParts.last(),
    )
}

/** Resolves a bare name through explicit single-static imports; star imports are handled later. */
private fun resolveSingleStaticImport(name: String, resolutionContext: JavaResolutionContext): StaticMemberReference? {
    val importedMember = getStaticImport(name, c = resolutionContext) ?: return null
    val qualifier = importedMember.parent()
    return StaticMemberReference(
        qualifier.asString(),
        resolveImportedClassName(qualifier, c = resolutionContext),
        importedMember.shortName().asString(),
    )
}

/**
 * Evaluates the constant expressions of the JLS 15.29 subset: Java field initializers as well as
 * annotation arguments and annotation-method default values (JLS 9.6.1 / 9.7).
 *
 * @param containingClass the class the expression is written in, if any; simple names are looked up
 *   in it and its enclosing classes before imports.
 */
class ConstantEvaluator private constructor(
    private val tree: LightSyntaxTree,
    private val resolutionContext: JavaResolutionContext,
    private val containingClass: JavaClassOverAst?,
) {
    /** Evaluator for an expression written inside [containingClass], i.e. a field initializer. */
    constructor(containingClass: JavaClassOverAst) : this(containingClass.tree, containingClass.memberResolutionContext, containingClass)

    /**
     * Evaluator for an expression written outside of any member — an annotation argument or an
     * annotation-method default value.
     */
    constructor(
        tree: LightSyntaxTree,
        resolutionContext: JavaResolutionContext,
    ) : this(tree, resolutionContext, resolutionContext.scopeContext.containingClass as? JavaClassOverAst)

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
        val children = tree.childrenWithoutCommentsAndWhitespace(node)
        if (children.size < 3) return null

        val lhs = evaluate(children[0]) ?: return null
        val operator = tree.getType(children[1])
        val rhs = evaluate(children[2]) ?: return null

        return evaluateBinaryOp(lhs, operator, rhs)
    }

    private fun evaluatePolyadicExpression(node: LightNode): Any? {
        val children = tree.childrenWithoutCommentsAndWhitespace(node)
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

        // Binary numeric promotion (JLS 5.6): a `char` operand is promoted to `int`, so `'a' + 1` is `int` 98.
        if (lhs is Char && rhs is Number) {
            return JavaLiteralParser.evaluateNumericBinaryOp(lhs.code, operator, rhs)
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
        val children = tree.childrenWithoutCommentsAndWhitespace(node)
        if (children.size < 2) return null

        val operator = tree.getType(children[0])
        val operand = evaluate(children[1]) ?: return null

        // Unary numeric promotion (JLS 5.6): `byte`, `short` and `char` operands are promoted to `int`.
        val promoted = when (operand) {
            is Byte -> operand.toInt()
            is Short -> operand.toInt()
            is Char -> operand.code
            else -> operand
        }
        return when (operator) {
            JavaSyntaxTokenType.MINUS -> when (promoted) {
                is Int -> -promoted
                is Long -> -promoted
                is Float -> -promoted
                is Double -> -promoted
                else -> null
            }
            JavaSyntaxTokenType.PLUS -> if (promoted is Number) promoted else null
            JavaSyntaxTokenType.TILDE -> when (promoted) {
                is Int -> promoted.inv()
                is Long -> promoted.inv()
                else -> null
            }
            JavaSyntaxTokenType.EXCL -> (operand as? Boolean)?.let { !it }
            else -> null
        }
    }

    private fun evaluateParensExpression(node: LightNode): Any? {
        val innerExpr = tree.childrenWithoutCommentsAndWhitespace(node).firstOrNull {
            val t = tree.getType(it)
            t != JavaSyntaxTokenType.LPARENTH && t != JavaSyntaxTokenType.RPARENTH
        } ?: return null
        return evaluate(innerExpr)
    }

    private fun evaluateTypeCastExpression(node: LightNode): Any? {
        val children = tree.childrenWithoutCommentsAndWhitespace(node)
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
        if (nameParts.size == 1) return resolveSimpleName(nameParts.single())
        val reference = resolveQualifiedMemberReference(nameParts, resolutionContext)

        // Resolve the class qualifier as a type name (via the unit's imports + same-package + java.lang
        // probes, JLS 6.5.5) before passing it to the cross-language resolver. Without this,
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
        // No class adapter: the qualifier is a JVM facade or a class FIR serves as a builtin.
        resolveExternalReference(reference.qualifier, resolvedClassId, reference.memberName)?.let { return it }

        // A Kotlin file facade is a JVM class, but it does not have a class symbol of its own.
        // If a single-type import supplied the qualifier, use its fully qualified name for facade lookup.
        if (nameParts.size == 2) {
            val importedClassName = getSimpleImport(nameParts.first(), c = resolutionContext)
            if (importedClassName != null) {
                resolveExternalReference(importedClassName.asString(), null, reference.memberName)?.let { return it }
            }
        }
        return MemberResolutionResult.Unresolved(reference)
    }

    private fun resolveExternalReference(
        classQualifier: String,
        qualifierClassId: ClassId?,
        fieldName: String,
        inheritingPackage: FqName? = null,
    ): MemberResolutionResult? = resolutionContext.fileContext.session.resolveExternalFieldValue(
        classQualifier,
        qualifierClassId,
        fieldName,
        resolutionContext.packageFqName,
        inheritingPackage,
    )

    /**
     * Resolves a bare name through lexical scopes, single-static imports, then static star
     * imports. A missing member in one scope is `null` so lookup can continue; `Unresolved` is the
     * final result when no scope supplies a member, optionally retaining an explicit import for
     * annotation-argument fallback.
     */
    private fun resolveSimpleName(name: String): MemberResolutionResult {
        val visited = mutableSetOf<ClassId>()
        var currentClass: JavaClass? = containingClass
        while (currentClass != null) {
            findMember(currentClass, name, visited)?.let { return it }
            currentClass = currentClass.outerClass
        }

        var unresolvedImport: StaticMemberReference? = null
        resolveSingleStaticImport(name, resolutionContext)?.let { imported ->
            val javaClass = imported.qualifierClassId?.let { classifierAdapterFor(it, c = resolutionContext) }
            if (javaClass != null) {
                findMember(javaClass, imported.memberName, mutableSetOf())?.let { return it }
            } else {
                resolveExternalReference(imported.qualifier, imported.qualifierClassId, imported.memberName)?.let { return it }
            }
            unresolvedImport = imported
        }

        for (className in resolutionContext.fileContext.imports.staticStarImports) {
            val classId = resolveImportedClassName(className, c = resolutionContext)
            val javaClass = classId?.let { classifierAdapterFor(it, c = resolutionContext) }
            if (javaClass != null) {
                findMember(javaClass, name, mutableSetOf())?.let { return it }
            }
            resolveExternalReference(className.asString(), null, name)?.let { return it }
        }
        return MemberResolutionResult.Unresolved(unresolvedImport)
    }

    /** Looks [memberName] up among the fields of [javaClass] and then of its supertypes (JLS 8.3). */
    private fun findMember(
        javaClass: JavaClass,
        memberName: String,
        visited: MutableSet<ClassId>,
        inheritingPackage: FqName? = null,
    ): MemberResolutionResult? {
        val classId = javaClass.classId ?: return null
        if (!visited.add(classId)) return null

        if (javaClass is JavaClassOverAst) {
            val field = javaClass.fields.firstOrNull { it.name.asString() == memberName }
            if (field != null && field.visibility.isInheritedTo(inheritingPackage, classId.packageFqName)) {
                if (field.isEnumEntry) return MemberResolutionResult.EnumEntry(classId, field.name)
                return field.initializerValue?.let(MemberResolutionResult::Constant)
                    ?: MemberResolutionResult.NonConstantField
            }
        } else {
            val session = resolutionContext.fileContext.session
            if (session.hasEnumEntry(classId, memberName)) {
                return MemberResolutionResult.EnumEntry(classId, Name.identifier(memberName))
            }
            resolveExternalReference(classId.asSingleFqName().asString(), classId, memberName, inheritingPackage)
                ?.let { return it }
        }

        for (supertype in javaClass.supertypes) {
            val superClass = supertype.classifier as? JavaClass ?: continue
            findMember(superClass, memberName, visited, classId.packageFqName)?.let { return it }
        }
        return null
    }
}
