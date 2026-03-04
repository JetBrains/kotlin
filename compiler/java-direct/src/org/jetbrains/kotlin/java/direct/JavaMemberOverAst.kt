/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaMemberOverAst(
    node: JavaSyntaxNode,
    override val containingClass: JavaClassOverAst
) : JavaElementOverAst(node, containingClass.resolutionContext.source), JavaMember {

    protected val resolutionContext: JavaResolutionContext
        get() = containingClass.resolutionContext

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
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

class JavaFieldOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaField {
    override val isEnumEntry: Boolean get() = node.type.toString() == "ENUM_CONSTANT"
    override val type: JavaType
        get() = createJavaType(node, resolutionContext)
    override val initializerValue: Any? get() = null
    override val hasConstantNotNullInitializer: Boolean get() = false
    override val isFromSource: Boolean get() = true
}

class JavaMethodOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaMethod {
    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = node.findChildByType("PARAMETER_LIST") ?: return emptyList()
            return parameterList.getChildrenByType("PARAMETER")
                .map { JavaValueParameterOverAst(it, resolutionContext) }
        }

    override val returnType: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE")
                ?: return JavaPrimitiveTypeOverAst(node, resolutionContext.source)
            return createJavaType(typeNode, resolutionContext)
        }

    override val annotationParameterDefaultValue: JavaAnnotationArgument? get() = null
    override val hasAnnotationParameterDefaultValue: Boolean get() = false
    override val isNative: Boolean get() = false

    override val typeParameters: List<JavaTypeParameter> by lazy {
        node.findChildByType("TYPE_PARAMETER_LIST")
            ?.getChildrenByType("TYPE_PARAMETER")
            ?.map { JavaTypeParameterOverAst(it, resolutionContext) }
            ?: emptyList()
    }

    override val isFromSource: Boolean get() = true
}

class JavaConstructorOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaConstructor {
    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = node.findChildByType("PARAMETER_LIST") ?: return emptyList()
            return parameterList.getChildrenByType("PARAMETER")
                .map { JavaValueParameterOverAst(it, resolutionContext) }
        }

    override val typeParameters: List<JavaTypeParameter> by lazy {
        node.findChildByType("TYPE_PARAMETER_LIST")
            ?.getChildrenByType("TYPE_PARAMETER")
            ?.map { JavaTypeParameterOverAst(it, resolutionContext) }
            ?: emptyList()
    }

    override val isFromSource: Boolean get() = true
}

class JavaValueParameterOverAst(
    node: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext
) : JavaElementOverAst(node, resolutionContext.source), JavaValueParameter {
    override val name: Name?
        get() = node.findChildByType("IDENTIFIER")?.text?.let { Name.identifier(it) }

    override val type: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE") ?: node
            return createJavaType(typeNode, resolutionContext)
        }

    override val isVararg: Boolean
        get() {
            if (node.findChildByType("ELLIPSIS") != null) return true
            val typeNode = node.findChildByType("TYPE")
            return typeNode?.findChildByType("ELLIPSIS") != null
        }

    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val isFromSource: Boolean get() = true
}
