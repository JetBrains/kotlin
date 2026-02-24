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
    override val classifier: JavaClassifier? by lazy {
        val typeName = node.text
        val simpleName = if (typeName.contains('.')) {
            typeName.substringAfterLast('.')
        } else {
            typeName
        }
        localScope?.findClass(Name.identifier(simpleName))
    }
    
    override val classifierQualifiedName: String
        get() {
            val typeName = node.text
            
            if (typeName.contains('.')) {
                return typeName
            }
            
            val qualified = imports.simpleImports[typeName]
            if (qualified != null) {
                return qualified.asString()
            }
            
            return typeName
        }
    
    override val presentableText: String get() = node.text
    override val isRaw: Boolean get() = false
    override val typeArguments: List<JavaType> get() = emptyList()

    override val isResolved: Boolean
        get() {
            val typeName = node.text
            return classifier != null 
                || typeName.contains('.')
                || imports.simpleImports.containsKey(typeName)
        }

    override fun resolve(tryResolve: (String) -> Boolean): String? {
        val simpleName = node.text

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
