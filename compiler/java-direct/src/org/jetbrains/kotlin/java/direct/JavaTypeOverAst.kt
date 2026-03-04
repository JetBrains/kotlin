/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaTypeOverAst(
    val node: JavaSyntaxNode,
    val source: CharSequence,
) : JavaType, JavaAnnotationOwner {
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val localScope: LocalJavaScope? = null,
    private val imports: JavaImports = JavaImports.EMPTY,
) : JavaTypeOverAst(node, source), JavaClassifierType {
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

        var current: JavaClassifier? = localScope?.findClass(Name.identifier(parts[0]))

        if (current is JavaClass) {
            for (i in 1 until parts.size) {
                val nextPart = parts[i]
                val prev = current as JavaClass
                current = prev.findInnerClass(Name.identifier(nextPart))
                if (current == null) return@lazy null
            }
        }

        current
    }

    override val classifierQualifiedName: String
        get() {
            val parts = rawTypeName.split('.')

            // 1. Check local scope (same compilation unit)
            val localBase = localScope?.findClass(Name.identifier(parts[0]))
            if (localBase != null) {
                var current: JavaClass? = localBase
                for (i in 1 until parts.size) {
                    current = current?.findInnerClass(Name.identifier(parts[i]))
                }
                return current?.fqName?.asString() ?: rawTypeName
            }

            // 2. Check explicit single-type imports
            val qualified = imports.simpleImports[parts[0]]
            if (qualified != null) {
                var result = qualified.asString()
                for (i in 1 until parts.size) {
                    result += "." + parts[i]
                }
                return result
            }

            // 3. Return as-is - FIR will resolve via callback (same package, star imports, java.lang)
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
            .map { typeNode -> createJavaType(typeNode, source, localScope, imports) }
    }

    override val isResolved: Boolean
        get() {
            return classifier != null
                    || rawTypeName.contains('.')
                    || imports.simpleImports.containsKey(rawTypeName)
        }

    override fun resolve(tryResolve: (String) -> Boolean): String? {
        val simpleName = rawTypeName

        // 1. Try current package first (Java same-package visibility)
        // TODO: it should be extracted from the local context, and removed from import itself
        val packageFqName = imports.packageFqName
        if (!packageFqName.isRoot) {
            val samePackageFqn = "${packageFqName.asString()}.$simpleName"
            if (tryResolve(samePackageFqn)) {
                return samePackageFqn
            }
        }

        // 2. Try java.lang.* (automatic import per JLS)
        val javaLangFqn = "java.lang.$simpleName"
        if (JavaToKotlinClassMap.mapJavaToKotlin(FqName(javaLangFqn)) != null || tryResolve(javaLangFqn)) {
            return javaLangFqn
        }

        // 3. Try explicit star imports
        val starImports = imports.starImports
        var foundFqn: String? = null

        for (starPackage in starImports) {
            val candidateFqn = "${starPackage.asString()}.$simpleName"
            if (tryResolve(candidateFqn)) {
                if (foundFqn != null) {
                    return null
                }
                foundFqn = candidateFqn
            }
        }

        return foundFqn
    }
}

class JavaPrimitiveTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
) : JavaTypeOverAst(node, source), JavaPrimitiveType {
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
    source: CharSequence,
    override val componentType: JavaType,
) : JavaTypeOverAst(node, source), JavaArrayType

class JavaWildcardTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    override val bound: JavaType?,
    override val isExtends: Boolean,
) : JavaTypeOverAst(node, source), JavaWildcardType

fun createJavaType(
    node: JavaSyntaxNode,
    source: CharSequence,
    localScope: LocalJavaScope? = null,
    imports: JavaImports = JavaImports.EMPTY,
): JavaType {
    // If input node is a TYPE with array brackets or vararg ellipsis, handle it directly
    // (don't look for nested TYPE first, as that would skip the array dimension)
    if (node.type.toString() == "TYPE") {
        val hasArrayBracket = node.findChildByType("LBRACKET") != null
        val hasVarargEllipsis = node.findChildByType("ELLIPSIS") != null
        if (hasArrayBracket || hasVarargEllipsis) {
            val componentTypeNode = node.findChildByType("TYPE")
            if (componentTypeNode != null) {
                val componentType = createJavaType(componentTypeNode, source, localScope, imports)
                return JavaArrayTypeOverAst(node, source, componentType)
            }
        }
    }

    val typeNode = node.findChildByType("TYPE") ?: node

    // Array type or vararg: TYPE contains nested TYPE + LBRACKET/RBRACKET or ELLIPSIS
    val hasArrayBracket = typeNode.findChildByType("LBRACKET") != null
    val hasVarargEllipsis = typeNode.findChildByType("ELLIPSIS") != null
    if (hasArrayBracket || hasVarargEllipsis) {
        val componentTypeNode = typeNode.findChildByType("TYPE")
        if (componentTypeNode != null) {
            val componentType = createJavaType(componentTypeNode, source, localScope, imports)
            return JavaArrayTypeOverAst(typeNode, source, componentType)
        }
    }

    val primitiveNode = typeNode.children.find { it.type.toString().endsWith("_KEYWORD") }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, source)
    }
    val referenceNode = typeNode.findChildByType("JAVA_CODE_REFERENCE")
    if (referenceNode != null) {
        return JavaClassifierTypeOverAst(referenceNode, source, localScope, imports)
    }
    return JavaClassifierTypeOverAst(typeNode, source, localScope, imports)
}

class JavaTypeParameterOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val localScope: LocalJavaScope? = null,
    private val imports: JavaImports = JavaImports.EMPTY,
) : JavaElementOverAst(node, source), JavaTypeParameter {
    override val name: Name get() = Name.identifier(node.findChildByType("IDENTIFIER")?.text ?: "<error>")
    override val upperBounds: Collection<JavaClassifierType> by lazy {
        val extendsList = node.findChildByType("EXTENDS_BOUND_LIST") ?: return@lazy emptyList()
        extendsList.getChildrenByType("JAVA_CODE_REFERENCE").map { ref ->
            JavaClassifierTypeOverAst(ref, source, localScope, imports)
        }
    }
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val isFromSource: Boolean get() = true
}
