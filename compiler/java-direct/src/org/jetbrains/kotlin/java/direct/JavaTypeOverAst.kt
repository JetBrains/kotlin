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
    private val localScope: LocalJavaScope? = null
) : JavaTypeOverAst(node, source), JavaClassifierType {
    override val classifier: JavaClassifier? by lazy {
        val typeName = node.text
        // TODO: suspicious ad-hoc check. Review later and consider more robust approach.
        val simpleName = if (typeName.contains('.')) {
            typeName.substringAfterLast('.')
        } else {
            typeName
        }
        localScope?.findClass(Name.identifier(simpleName))
    }
    
    override val classifierQualifiedName: String get() = node.text
    override val presentableText: String get() = node.text
    override val isRaw: Boolean get() = false
    override val typeArguments: List<JavaType> get() = emptyList()
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

fun createJavaType(node: JavaSyntaxNode, source: CharSequence, localScope: LocalJavaScope? = null): JavaType {
    val typeNode = node.findChildByType("TYPE") ?: node
    val primitiveNode = typeNode.children.find { it.type.toString().endsWith("_KEYWORD") }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, source)
    }
    val referenceNode = typeNode.findChildByType("JAVA_CODE_REFERENCE")
    if (referenceNode != null) {
        return JavaClassifierTypeOverAst(referenceNode, source, localScope)
    }
    return JavaClassifierTypeOverAst(typeNode, source, localScope)
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
