/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaTypeOverAst(
    val node: JavaSyntaxNode,
    val source: CharSequence
) : JavaType, JavaAnnotationOwner {
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val localScope: LocalJavaScope? = null,
    private val imports: JavaImports = JavaImports.EMPTY
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
        
        if (current == null) {
            val simpleImport = imports.simpleImports[parts[0]]
            // If it's a simple import, we can't easily resolve it to a JavaClassifier here 
            // because we don't have access to the full class finder.
            // But if it's a local class, we can traverse.
        }

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
            
            val localBase = localScope?.findClass(Name.identifier(parts[0]))
            if (localBase != null) {
                var current: JavaClass? = localBase
                for (i in 1 until parts.size) {
                    current = current?.findInnerClass(Name.identifier(parts[i]))
                }
                return current?.fqName?.asString() ?: rawTypeName
            }
            
            val qualified = imports.simpleImports[parts[0]]
            if (qualified != null) {
                var result = qualified.asString()
                for (i in 1 until parts.size) {
                    result += "." + parts[i]
                }
                return result
            }
            
            return rawTypeName
        }
    
    override val presentableText: String get() = node.text
    override val isRaw: Boolean get() = false
    override val typeArguments: List<JavaType> get() = emptyList()

    override val isResolved: Boolean
        get() {
            return classifier != null 
                || rawTypeName.contains('.')
                || imports.simpleImports.containsKey(rawTypeName)
        }

    override fun resolve(tryResolve: (String) -> Boolean): String? {
        val simpleName = rawTypeName

        val javaLangFqn = "java.lang.$simpleName"
        if (tryResolve(javaLangFqn)) {
            return javaLangFqn
        }

        val starImports = imports.starImports
        var foundFqn: String? = null

        for (packageFqName in starImports) {
            val candidateFqn = "${packageFqName.asString()}.$simpleName"
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
    source: CharSequence
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
    override val componentType: JavaType
) : JavaTypeOverAst(node, source), JavaArrayType

class JavaWildcardTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    override val bound: JavaType?,
    override val isExtends: Boolean
) : JavaTypeOverAst(node, source), JavaWildcardType

fun createJavaType(
    node: JavaSyntaxNode,
    source: CharSequence,
    localScope: LocalJavaScope? = null,
    imports: JavaImports = JavaImports.EMPTY
): JavaType {
    val typeNode = node.findChildByType("TYPE") ?: node
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
    source: CharSequence
) : JavaElementOverAst(node, source), JavaTypeParameter {
    override val name: Name get() = Name.identifier(node.findChildByType("IDENTIFIER")?.text ?: "<error>")
    override val upperBounds: Collection<JavaClassifierType> get() = emptyList()
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val isFromSource: Boolean get() = true
}
