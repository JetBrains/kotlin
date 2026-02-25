/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.Name

abstract class JavaMemberOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    override val containingClass: JavaClass
) : JavaElementOverAst(node, source), JavaMember {

    override val name: Name
        get() = Name.identifier(node.findChildByType("IDENTIFIER")?.text ?: "<error>")

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType("MODIFIER_LIST")

    private fun hasModifier(modifier: String): Boolean {
        return modifierList?.children?.any { it.type.toString() == modifier } ?: false
    }

    override val isAbstract: Boolean get() = hasModifier("ABSTRACT_KEYWORD")
    override val isStatic: Boolean get() = hasModifier("STATIC_KEYWORD")
    override val isFinal: Boolean get() = hasModifier("FINAL_KEYWORD")

    override val visibility: Visibility
        get() = when {
            containingClass.isInterface -> Visibilities.Public
            hasModifier("PUBLIC_KEYWORD") -> Visibilities.Public
            hasModifier("PROTECTED_KEYWORD") -> Visibilities.Protected
            hasModifier("PRIVATE_KEYWORD") -> Visibilities.Private
            else -> JavaVisibilities.PackageVisibility
        }

    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: org.jetbrains.kotlin.name.FqName): JavaAnnotation? = null
}

class JavaFieldOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    containingClass: JavaClass
) : JavaMemberOverAst(node, source, containingClass), JavaField {
    override val isEnumEntry: Boolean get() = node.type.toString() == "ENUM_CONSTANT"
    override val type: JavaType
        get() = createJavaType(
            node, source,
            localScope = (containingClass as? JavaClassOverAst)?.localScope,
            imports = (containingClass as? JavaClassOverAst)?.imports ?: JavaImports.EMPTY
        )
    override val initializerValue: Any? get() = null
    override val hasConstantNotNullInitializer: Boolean get() = false
    override val isFromSource: Boolean get() = true
}

class JavaMethodOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    containingClass: JavaClass
) : JavaMemberOverAst(node, source, containingClass), JavaMethod {
    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = node.findChildByType("PARAMETER_LIST") ?: return emptyList()
            val parameters = parameterList.getChildrenByType("PARAMETER")
            return parameters.map { JavaValueParameterOverAst(it, source, containingClass) }
        }

    override val returnType: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE") ?: return JavaPrimitiveTypeOverAst(node, source)
            return createJavaType(
                typeNode, source,
                localScope = (containingClass as? JavaClassOverAst)?.localScope,
                imports = (containingClass as? JavaClassOverAst)?.imports ?: JavaImports.EMPTY
            )
        }
    override val annotationParameterDefaultValue: JavaAnnotationArgument? get() = null
    override val hasAnnotationParameterDefaultValue: Boolean get() = false
    override val isNative: Boolean get() = false
    override val typeParameters: List<JavaTypeParameter> get() = emptyList()
    override val isFromSource: Boolean get() = true
}

class JavaConstructorOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    containingClass: JavaClass
) : JavaMemberOverAst(node, source, containingClass), JavaConstructor {
    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = node.findChildByType("PARAMETER_LIST") ?: return emptyList()
            val parameters = parameterList.getChildrenByType("PARAMETER")
            return parameters.map { JavaValueParameterOverAst(it, source, containingClass) }
        }
    override val typeParameters: List<JavaTypeParameter> get() = emptyList()
    override val isFromSource: Boolean get() = true
}

class JavaValueParameterOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val containingClass: JavaClass
) : JavaElementOverAst(node, source), JavaValueParameter {
    override val name: Name?
        get() = node.findChildByType("IDENTIFIER")?.text?.let { Name.identifier(it) }

    override val type: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE") ?: node
            return createJavaType(
                typeNode, source,
                localScope = (containingClass as? JavaClassOverAst)?.localScope,
                imports = (containingClass as? JavaClassOverAst)?.imports ?: JavaImports.EMPTY
            )
        }

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType("MODIFIER_LIST")

    private fun hasModifier(modifier: String): Boolean {
        return modifierList?.children?.any { it.type.toString() == modifier } ?: false
    }

    override val isVararg: Boolean
        get() = node.findChildByType("ELLIPSIS") != null

    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: org.jetbrains.kotlin.name.FqName): JavaAnnotation? = null
    override val isFromSource: Boolean get() = true
}
