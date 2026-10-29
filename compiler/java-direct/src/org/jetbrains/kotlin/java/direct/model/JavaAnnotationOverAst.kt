/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.getSimpleImport
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.java.direct.util.ConstantEvaluator
import org.jetbrains.kotlin.java.direct.util.JavaLiteralParser
import org.jetbrains.kotlin.java.direct.util.MemberResolutionResult
import org.jetbrains.kotlin.java.direct.util.StaticMemberReference
import org.jetbrains.kotlin.java.direct.util.referenceNameParts
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaAnnotationOverAst(
    node: LightNode,
    tree: LightSyntaxTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node, tree), JavaAnnotation {

    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.ANNOTATION_PARAMETER_LIST)
            return if (parameterList == null) {
                emptyList()
            } else {
                tree.getChildrenByType(parameterList, JavaSyntaxElementType.NAME_VALUE_PAIR).map { nvp ->
                    createAnnotationArgument(nvp, tree, resolutionContext)
                }
            }
        }

    /**
     * The simple or qualified name of the annotation as it appears in the source.
     * For `@Deprecated`, returns "Deprecated".
     * For `@java.lang.Deprecated`, returns "java.lang.Deprecated".
     */
    private val annotationName: String?
        get() = tree.findChildByType(node, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { tree.getText(it).toString() }

    override val classId: ClassId?
        get() = computeClassId()

    private fun computeClassId(): ClassId? {
        val reference = annotationName ?: return null

        with(resolutionContext) { resolve(reference) }?.let { return it }

        // No-symbol-provider fallback (parsing-level unit fixtures): `resolve` returned null
        // because `tryResolve` is always `false` without a provider. The `ClassId.topLevel`
        // split below misclassifies nested-class imports (`import a.b.C.D` → `ClassId(a.b.C, D)`
        // rather than `ClassId(a.b, C.D)`); the provider-backed path above is the correct one
        // for production code.
        if (reference.contains('.')) {
            return ClassId.topLevel(FqName(reference))
        }

        val imported = with(resolutionContext) { getSimpleImport(reference) }
        if (imported != null) {
            return ClassId.topLevel(imported)
        }

        return ClassId.topLevel(FqName(reference))
    }

    // Resolution is consumed via [classId]; the FIR side reads it directly.
    override fun resolve(): JavaClass? = null
}

private fun createAnnotationArgument(
    nameValuePair: LightNode,
    tree: LightSyntaxTree,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    val name = tree.findChildByType(nameValuePair, JavaSyntaxTokenType.IDENTIFIER)?.let {
        Name.identifier(tree.getText(it).toString())
    }

    val valueNode = tree.getChildren(nameValuePair).firstOrNull { child ->
        val t = tree.getType(child)
        t != JavaSyntaxTokenType.IDENTIFIER && t != JavaSyntaxTokenType.EQ
    }

    return createAnnotationArgumentFromValue(name, valueNode, tree, resolutionContext)
}

internal fun createAnnotationArgumentFromValue(
    name: Name?,
    valueNode: LightNode?,
    tree: LightSyntaxTree,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    if (valueNode == null) {
        return JavaUnknownAnnotationArgumentOverAst(name)
    }

    return when (tree.getType(valueNode)) {
        JavaSyntaxElementType.LITERAL_EXPRESSION -> {
            val value = JavaLiteralParser.evaluateLiteral(valueNode, tree)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        JavaSyntaxElementType.ARRAY_INITIALIZER_EXPRESSION, JavaSyntaxElementType.ANNOTATION_ARRAY_INITIALIZER -> {
            JavaArrayAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.REFERENCE_EXPRESSION -> {
            // An enum entry or a constant field; an enum class can declare both.
            when (val resolution = ConstantEvaluator(tree, resolutionContext).resolveReference(valueNode)) {
                is MemberResolutionResult.EnumEntry ->
                    JavaEnumValueAnnotationArgumentOverAst(name, resolution.enumClassId, resolution.entryName)
                is MemberResolutionResult.Constant -> JavaLiteralAnnotationArgumentOverAst(name, resolution.value)
                MemberResolutionResult.NonConstantField -> JavaUnknownAnnotationArgumentOverAst(name)
                is MemberResolutionResult.Unresolved ->
                    createUnresolvedReferenceArgument(name, tree.referenceNameParts(valueNode), resolution.reference)
            }
        }
        JavaSyntaxElementType.CLASS_OBJECT_ACCESS_EXPRESSION -> {
            JavaClassObjectAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.ANNOTATION -> {
            JavaAnnotationAsAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.PREFIX_EXPRESSION, JavaSyntaxElementType.BINARY_EXPRESSION,
        JavaSyntaxElementType.POLYADIC_EXPRESSION, JavaSyntaxElementType.PARENTH_EXPRESSION,
        JavaSyntaxElementType.TYPE_CAST_EXPRESSION -> {
            val value = ConstantEvaluator(tree, resolutionContext).evaluate(valueNode)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        else -> {
            JavaUnknownAnnotationArgumentOverAst(name)
        }
    }
}

/**
 * Keeps the enum-argument shape when the reference has a member name. A resolved qualifier supplies
 * its class ID; otherwise FIR derives the enum class from the annotation parameter type. Malformed
 * references and known non-constant fields become unknown arguments.
 */
private fun createUnresolvedReferenceArgument(
    name: Name?,
    referenceParts: List<String>?,
    reference: StaticMemberReference?,
): JavaAnnotationArgument {
    val parts = referenceParts ?: return JavaUnknownAnnotationArgumentOverAst(name)
    return JavaEnumValueAnnotationArgumentOverAst(
        name,
        reference?.qualifierClassId,
        Name.identifier(reference?.memberName ?: parts.last()),
    )
}

class JavaLiteralAnnotationArgumentOverAst(
    override val name: Name?,
    override val value: Any?,
) : JavaLiteralAnnotationArgument

class JavaArrayAnnotationArgumentOverAst(
    override val name: Name?,
    private val arrayNode: LightNode,
    private val tree: LightSyntaxTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaArrayAnnotationArgument {
    override fun getElements(): List<JavaAnnotationArgument> {
        return tree.getChildren(arrayNode)
            .filter {
                val t = tree.getType(it)
                t != JavaSyntaxTokenType.LBRACE && t != JavaSyntaxTokenType.RBRACE &&
                        t != JavaSyntaxTokenType.COMMA
            }
            .map { createAnnotationArgumentFromValue(null, it, tree, resolutionContext) }
    }
}

class JavaEnumValueAnnotationArgumentOverAst(
    override val name: Name?,
    override val enumClassId: ClassId?,
    override val entryName: Name,
) : JavaEnumValueAnnotationArgument

class JavaClassObjectAnnotationArgumentOverAst(
    override val name: Name?,
    private val classObjNode: LightNode,
    private val tree: LightSyntaxTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaClassObjectAnnotationArgument {
    override fun getReferencedType(): JavaType {
        val typeNode = tree.findChildByType(classObjNode, JavaSyntaxElementType.TYPE)
            ?: tree.findChildByType(classObjNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE)

        return if (typeNode != null) {
            createJavaType(typeNode, tree, resolutionContext)
        } else {
            JavaClassifierTypeOverAst(classObjNode, tree, resolutionContext)
        }
    }
}

class JavaAnnotationAsAnnotationArgumentOverAst(
    override val name: Name?,
    private val annotationNode: LightNode,
    private val tree: LightSyntaxTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation(): JavaAnnotation {
        return JavaAnnotationOverAst(annotationNode, tree, resolutionContext)
    }
}

class JavaUnknownAnnotationArgumentOverAst(
    override val name: Name?,
) : JavaUnknownAnnotationArgument
