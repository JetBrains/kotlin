/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Standard Java annotations that do NOT have TYPE_USE in their @Target.
 * These should be filtered out when they appear on types (from method modifier lists).
 * Includes both fully qualified names and simple names (for java.lang.* annotations).
 */
private val NON_TYPE_USE_ANNOTATION_FQ_NAMES: Set<FqName> = setOf(
    FqName("java.lang.Override"),           // @Target(METHOD)
    FqName("java.lang.SafeVarargs"),        // @Target({CONSTRUCTOR, METHOD})
    FqName("java.lang.FunctionalInterface"), // @Target(TYPE)
    FqName("java.lang.Deprecated"),         // @Target({CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, MODULE, PARAMETER, TYPE})
)

/**
 * Simple names of java.lang annotations that are NOT TYPE_USE.
 * Used to filter unqualified annotations like @Override.
 */
private val NON_TYPE_USE_ANNOTATION_SIMPLE_NAMES: Set<String> = setOf(
    "Override",
    "SafeVarargs",
    "FunctionalInterface",
    "Deprecated",
)

/**
 * Filters annotations to exclude known non-TYPE_USE annotations.
 * This mimics javac-wrapper's filterTypeAnnotations() but uses a hardcoded list
 * since java-direct doesn't have access to annotation class resolution at model level.
 * TODO: consider other approach with some annotations resolution
 */
private fun Collection<JavaAnnotation>.filterTypeAnnotations(): Collection<JavaAnnotation> {
    if (isEmpty()) return this
    return filter { annotation ->
        val classId = annotation.classId ?: return@filter true
        val fqName = classId.asSingleFqName()
        // Check both FQ name and simple name (for unqualified java.lang.* annotations)
        fqName !in NON_TYPE_USE_ANNOTATION_FQ_NAMES &&
            fqName.shortName().asString() !in NON_TYPE_USE_ANNOTATION_SIMPLE_NAMES
    }
}

abstract class JavaTypeOverAst(
    val node: JavaSyntaxNode,
    protected val resolutionContext: JavaResolutionContext,
    private val extraAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaType, JavaAnnotationOwner {
    override val annotations: Collection<JavaAnnotation>
        get() {
            val nodeAnnotations = node.findChildByType("MODIFIER_LIST")?.getChildrenByType("ANNOTATION")
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
            // Filter extra annotations (from method modifier list) to exclude non-TYPE_USE annotations.
            // Node annotations are already TYPE_USE by definition (they appear directly on the type).
            return extraAnnotations.filterTypeAnnotations() + nodeAnnotations
        }

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations), JavaClassifierType {

    private val rawTypeName: String by lazy {
        var text = node.text.trim()
        while (text.endsWith("]")) {
            val bracketIndex = text.lastIndexOf('[')
            if (bracketIndex < 0) break
            text = text.substring(0, bracketIndex).trimEnd()
        }
        val genericIndex = text.indexOf('<')
        if (genericIndex >= 0) {
            text = text.substring(0, genericIndex).trimEnd()
        }
        text
    }

    override val classifier: JavaClassifier? by lazy {
        val parts = rawTypeName.split('.')

        // 1. Check type parameters in scope FIRST (e.g., T, E, K, V)
        if (parts.size == 1) {
            resolutionContext.findTypeParameter(parts[0])?.let { return@lazy it }
        }

        // 2. Check local classes (same compilation unit)
        var current: JavaClassifier? = resolutionContext.findLocalClass(Name.identifier(parts[0]))

        if (current is JavaClass) {
            for (i in 1 until parts.size) {
                current = (current as JavaClass).findInnerClass(Name.identifier(parts[i]))
                    ?: return@lazy null
            }
        }
        current
    }

    override val classifierQualifiedName: String
        get() {
            val parts = rawTypeName.split('.')

            // 1. Check type parameters - return name as-is (FIR handles type params specially)
            if (parts.size == 1 && resolutionContext.findTypeParameter(parts[0]) != null) {
                return rawTypeName
            }

            // 2. Check local scope (same compilation unit)
            val localBase = resolutionContext.findLocalClass(Name.identifier(parts[0]))
            if (localBase != null) {
                var current: JavaClass? = localBase
                for (i in 1 until parts.size) {
                    current = current?.findInnerClass(Name.identifier(parts[i]))
                }
                return current?.fqName?.asString() ?: rawTypeName
            }

            // 3. Check explicit single-type imports
            val qualified = resolutionContext.getSimpleImport(parts[0])
            if (qualified != null) {
                var result = qualified.asString()
                for (i in 1 until parts.size) {
                    result += "." + parts[i]
                }
                return result
            }

            // 4. Return as-is - FIR will resolve via callback (same package, star imports, java.lang)
            return rawTypeName
        }

    override val presentableText: String get() = node.text

    override val isRaw: Boolean by lazy {
        val hasParameterList = node.findChildByType("REFERENCE_PARAMETER_LIST") != null
        !hasParameterList && (classifier as? JavaClass)?.typeParameters?.isNotEmpty() == true
    }

    override val typeArguments: List<JavaType> by lazy {
        val parameterList = node.findChildByType("REFERENCE_PARAMETER_LIST") ?: return@lazy emptyList()
        parameterList.children
            .filter { it.type.toString() == "TYPE" }
            .map { typeNode -> createJavaType(typeNode, resolutionContext) }
    }

    override val isResolved: Boolean
        get() = classifier != null
                || rawTypeName.contains('.')
                || resolutionContext.getSimpleImport(rawTypeName) != null
                || resolutionContext.findTypeParameter(rawTypeName) != null

    override fun resolve(tryResolve: (String) -> Boolean): String? {
        return resolutionContext.resolveWithCallback(rawTypeName, tryResolve)
    }
}

class JavaPrimitiveTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations), JavaPrimitiveType {
    override val type: org.jetbrains.kotlin.builtins.PrimitiveType?
        get() = when (node.text) {
            "void" -> null
            "boolean" -> org.jetbrains.kotlin.builtins.PrimitiveType.BOOLEAN
            "char" -> org.jetbrains.kotlin.builtins.PrimitiveType.CHAR
            "byte" -> org.jetbrains.kotlin.builtins.PrimitiveType.BYTE
            "short" -> org.jetbrains.kotlin.builtins.PrimitiveType.SHORT
            "int" -> org.jetbrains.kotlin.builtins.PrimitiveType.INT
            "float" -> org.jetbrains.kotlin.builtins.PrimitiveType.FLOAT
            "long" -> org.jetbrains.kotlin.builtins.PrimitiveType.LONG
            "double" -> org.jetbrains.kotlin.builtins.PrimitiveType.DOUBLE
            else -> null
        }
}

class JavaArrayTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    override val componentType: JavaType,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations), JavaArrayType

class JavaWildcardTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    override val bound: JavaType?,
    override val isExtends: Boolean,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations), JavaWildcardType

fun createJavaType(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
): JavaType {
    // If input node is a TYPE with array brackets or vararg ellipsis, handle it directly
    // (don't look for nested TYPE first, as that would skip the array dimension)
    if (node.type.toString() == "TYPE") {
        val hasArrayBracket = node.findChildByType("LBRACKET") != null
        val hasVarargEllipsis = node.findChildByType("ELLIPSIS") != null
        if (hasArrayBracket || hasVarargEllipsis) {
            val componentTypeNode = node.findChildByType("TYPE")
            if (componentTypeNode != null) {
                val componentType = createJavaType(componentTypeNode, resolutionContext)
                return JavaArrayTypeOverAst(node, resolutionContext, componentType, extraAnnotations)
            }
        }
    }

    val typeNode = node.findChildByType("TYPE") ?: node

    // Wildcard type: TYPE contains QUEST (the '?'), optionally with EXTENDS_KEYWORD or SUPER_KEYWORD
    // AST structure: TYPE -> [QUEST, (EXTENDS_KEYWORD|SUPER_KEYWORD)?, TYPE?]
    if (typeNode.findChildByType("QUEST") != null) {
        val hasSuper = typeNode.findChildByType("SUPER_KEYWORD") != null
        val boundTypeNode = typeNode.findChildByType("TYPE")
        val bound = boundTypeNode?.let { createJavaType(it, resolutionContext) as? JavaClassifierType }
        // isExtends = true for "? extends X" or unbounded "?"
        // isExtends = false for "? super X"
        val isExtends = !hasSuper
        return JavaWildcardTypeOverAst(typeNode, resolutionContext, bound, isExtends, extraAnnotations)
    }

    // Array type or vararg: TYPE contains nested TYPE + LBRACKET/RBRACKET or ELLIPSIS
    val hasArrayBracket = typeNode.findChildByType("LBRACKET") != null
    val hasVarargEllipsis = typeNode.findChildByType("ELLIPSIS") != null
    if (hasArrayBracket || hasVarargEllipsis) {
        val componentTypeNode = typeNode.findChildByType("TYPE")
        if (componentTypeNode != null) {
            val componentType = createJavaType(componentTypeNode, resolutionContext)
            return JavaArrayTypeOverAst(typeNode, resolutionContext, componentType, extraAnnotations)
        }
    }

    val primitiveNode = typeNode.children.find { it.type.toString().endsWith("_KEYWORD") }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, resolutionContext, extraAnnotations)
    }

    val referenceNode = typeNode.findChildByType("JAVA_CODE_REFERENCE")
    if (referenceNode != null) {
        return JavaClassifierTypeOverAst(referenceNode, resolutionContext, extraAnnotations)
    }
    return JavaClassifierTypeOverAst(typeNode, resolutionContext, extraAnnotations)
}

/**
 * Creates a JavaType with additional annotations from a modifier list.
 * Used to handle TYPE_USE annotations that syntactically appear in the
 * method/field modifier list but semantically belong to the type.
 */
fun createJavaTypeWithAnnotations(
    typeNode: JavaSyntaxNode,
    modifierList: JavaSyntaxNode?,
    resolutionContext: JavaResolutionContext,
): JavaType {
    val extraAnnotations = modifierList?.getChildrenByType("ANNOTATION")
        ?.map { JavaAnnotationOverAst(it, resolutionContext) }
        ?: emptyList()
    return createJavaType(typeNode, resolutionContext, extraAnnotations)
}

class JavaTypeParameterOverAst(
    node: JavaSyntaxNode,
    initialResolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaTypeParameter {

    // Resolution context can be updated after construction to include all sibling type parameters.
    // This is needed for resolving bounds like `S extends JsStubElement<E>` where E is another type param.
    private var resolutionContext: JavaResolutionContext = initialResolutionContext

    /**
     * Updates the resolution context used for resolving upper bounds.
     * Called after all type parameters are created to add them all to scope.
     */
    fun updateResolutionContext(newContext: JavaResolutionContext) {
        resolutionContext = newContext
    }

    override val name: Name
        get() = Name.identifier(node.findChildByType("IDENTIFIER")?.text ?: "<error>")

    override val upperBounds: Collection<JavaClassifierType> by lazy {
        val extendsList = node.findChildByType("EXTENDS_BOUND_LIST") ?: return@lazy emptyList()
        extendsList.getChildrenByType("JAVA_CODE_REFERENCE").map { ref ->
            JavaClassifierTypeOverAst(ref, resolutionContext)
        }
    }

    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val isFromSource: Boolean get() = true
}
