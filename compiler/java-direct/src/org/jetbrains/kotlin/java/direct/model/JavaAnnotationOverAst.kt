/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.getSimpleImport
import org.jetbrains.kotlin.java.direct.resolution.getStaticImport
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.java.direct.resolution.resolveConstFieldValue
import org.jetbrains.kotlin.java.direct.resolution.resolveExternalFieldValue
import org.jetbrains.kotlin.java.direct.util.ConstantEvaluator
import org.jetbrains.kotlin.java.direct.util.JavaLiteralParser
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaAnnotationOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
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
    nameValuePair: JavaLightNode,
    tree: JavaLightTree,
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
    valueNode: JavaLightNode?,
    tree: JavaLightTree,
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
            // Could be an enum entry reference (e.g. `RetentionPolicy.RUNTIME`) or a const-val
            // reference (e.g. `KotlinClass.FOO_INT`). These are disambiguated here through
            // `JavaResolutionContext.resolveConstFieldValue` (session-backed): if the reference
            // does not resolve to a const value (or the session has no symbol provider —
            // parsing-level unit fixtures), fall back to the enum-entry shape.
            val enumArg = JavaEnumValueAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
            val classId = enumArg.enumClassId
            val constValue = if (classId != null) {
                with(resolutionContext) { resolveConstFieldValue(classId, enumArg.entryName) }
            } else null
            // Not a `const val`: the reference may still name a Java `static final` constant of
            // this compilation unit, that is evaluated on the AST.
            val value = constValue ?: ConstantEvaluator(tree, resolutionContext).evaluate(valueNode)
            if (value != null) JavaLiteralAnnotationArgumentOverAst(name, value) else enumArg
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
            val value = annotationConstantEvaluator(tree, resolutionContext).evaluate(valueNode)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        else -> {
            JavaUnknownAnnotationArgumentOverAst(name)
        }
    }
}

/**
 * The evaluator for a constant expression written as an annotation argument or as an
 * annotation-method default value (JLS 9.6.1 / 9.7).
 *
 * It is the same [ConstantEvaluator] the field initializers use, so that references to
 * `static final` constants, parenthesized and polyadic sub-expressions and casts are evaluated
 * identically in both positions; cross-language `const val` references inside the expression go
 * through the session-backed `resolveExternalFieldValue`, as they do for fields.
 */
private fun annotationConstantEvaluator(tree: JavaLightTree, resolutionContext: JavaResolutionContext): ConstantEvaluator =
    ConstantEvaluator(tree, resolutionContext) { classQualifier, fieldName ->
        with(resolutionContext) { resolveExternalFieldValue(classQualifier, fieldName) }
    }

class JavaLiteralAnnotationArgumentOverAst(
    override val name: Name?,
    override val value: Any?,
) : JavaLiteralAnnotationArgument

class JavaArrayAnnotationArgumentOverAst(
    override val name: Name?,
    private val arrayNode: JavaLightNode,
    private val tree: JavaLightTree,
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
    private val refNode: JavaLightNode,
    private val tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaEnumValueAnnotationArgument {

    /**
     * For bare identifiers (no dots), tries to resolve via *static* single-imports only
     * (`import static example.KotlinDtoMapping.ID;` makes `ID` resolvable as
     * `className="example.KotlinDtoMapping"`, `entryName="ID"`). Non-static type imports
     * are deliberately *not* consulted here: an `import a.b.X;` of a type cannot bind a
     * bare enum-entry reference — only a static-member import can.
     *
     * Returns a pair of (className, memberName) if the static import is found, null otherwise.
     */
    private val staticImportResolution: Pair<String, String>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val text = tree.getText(refNode).toString()
        if (text.contains('.')) return@lazy null
        val importedFqn = with(resolutionContext) { getStaticImport(text) } ?: return@lazy null
        val fqnStr = importedFqn.asString()
        val lastDot = fqnStr.lastIndexOf('.')
        if (lastDot < 0) return@lazy null
        fqnStr.substring(0, lastDot) to fqnStr.substring(lastDot + 1)
    }

    private val className: String?
        get() {
            val text = tree.getText(refNode).toString()
            val lastDot = text.lastIndexOf('.')
            if (lastDot >= 0) return text.substring(0, lastDot)
            return staticImportResolution?.first
        }

    override val enumClassId: ClassId?
        get() {
            val className = className ?: return null

            val imported = with(resolutionContext) { getSimpleImport(className) }
            if (imported != null) {
                return ClassId.topLevel(imported)
            }

            // Consult the model's resolver for the full JLS scope walk (local nested-class,
            // inherited inner classes, same-package, java.lang, star imports). `resolve` returns
            // null without a symbol provider (parsing-level unit fixtures), letting the
            // package+name heuristic below take over.
            with(resolutionContext) { resolve(className) }?.let { return it }

            // Already-dotted className (qualified or static-import-resolved FQN) is treated
            // as a top-level FQN — mirrors `JavaAnnotationOverAst.classId`'s dotted-name
            // shortcut, and avoids prefixing it with the file's package.
            if (className.contains('.')) {
                return ClassId.topLevel(FqName(className))
            }

            val packageFqName = resolutionContext.packageFqName
            return if (packageFqName.isRoot) {
                ClassId.topLevel(FqName(className))
            } else {
                ClassId.topLevel(FqName("${packageFqName.asString()}.$className"))
            }
        }

    override val entryName: Name
        get() {
            val text = tree.getText(refNode).toString()
            val lastDot = text.lastIndexOf('.')
            if (lastDot >= 0) return Name.identifier(text.substring(lastDot + 1))
            staticImportResolution?.let { return Name.identifier(it.second) }
            return Name.identifier(text)
        }
}

class JavaClassObjectAnnotationArgumentOverAst(
    override val name: Name?,
    private val classObjNode: JavaLightNode,
    private val tree: JavaLightTree,
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
    private val annotationNode: JavaLightNode,
    private val tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation(): JavaAnnotation {
        return JavaAnnotationOverAst(annotationNode, tree, resolutionContext)
    }
}

class JavaUnknownAnnotationArgumentOverAst(
    override val name: Name?,
) : JavaUnknownAnnotationArgument
