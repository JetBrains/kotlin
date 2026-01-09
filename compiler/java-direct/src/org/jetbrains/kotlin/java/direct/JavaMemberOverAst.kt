/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
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
            hasModifier("PUBLIC_KEYWORD") -> org.jetbrains.kotlin.descriptors.Visibilities.Public
            hasModifier("PROTECTED_KEYWORD") -> org.jetbrains.kotlin.descriptors.Visibilities.Protected
            hasModifier("PRIVATE_KEYWORD") -> org.jetbrains.kotlin.descriptors.Visibilities.Private
            else -> JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        } as Visibility

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
    override val type: JavaType get() = createJavaType(node, source)
    override val initializerValue: Any? get() = null
    override val hasConstantNotNullInitializer: Boolean get() = false
    override val isFromSource: Boolean get() = true
}

class JavaMethodOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    containingClass: JavaClass
) : JavaMemberOverAst(node, source, containingClass), JavaMethod {
    override val valueParameters: List<JavaValueParameter> get() = emptyList()
    override val returnType: JavaType get() = createJavaType(node, source)
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
    override val valueParameters: List<JavaValueParameter> get() = emptyList()
    override val typeParameters: List<JavaTypeParameter> get() = emptyList()
    override val isFromSource: Boolean get() = true
}
