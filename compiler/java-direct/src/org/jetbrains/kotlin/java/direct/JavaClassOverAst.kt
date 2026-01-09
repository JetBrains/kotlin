/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Suppress("UNCHECKED_CAST")
class JavaClassOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    override val outerClass: JavaClass? = null
) : JavaElementOverAst(node, source), JavaClass {

    override val name: Name
        get() = Name.identifier(node.children.find { it.type.toString() == "IDENTIFIER" }?.text ?: "<error>")

    override val fqName: FqName?
        get() {
            var currentRoot: JavaSyntaxNode? = node
            while (currentRoot?.parent != null) {
                currentRoot = currentRoot.parent
            }
            val packageStmt = currentRoot?.findChildByType("PACKAGE_STATEMENT")
            val packageName = packageStmt?.findChildByType("JAVA_CODE_REFERENCE")?.text
            val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT
            
            val nestedName = mutableListOf<String>()
            var currentClass: JavaClass? = this
            while (currentClass != null) {
                nestedName.add(0, currentClass.name.asString())
                currentClass = currentClass.outerClass
            }
            
            var result = packageFqName
            for (name in nestedName) {
                result = result.child(Name.identifier(name))
            }
            return result
        }

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType("MODIFIER_LIST")

    private fun hasModifier(modifier: String): Boolean {
        return modifierList?.children?.any { it.type.toString() == modifier } ?: false
    }

    override val isAbstract: Boolean get() = hasModifier("ABSTRACT_KEYWORD") || isInterface
    override val isStatic: Boolean get() = hasModifier("STATIC_KEYWORD")
    override val isFinal: Boolean get() = hasModifier("FINAL_KEYWORD")

    override val visibility: Visibility
        get() = when {
            hasModifier("PUBLIC_KEYWORD") -> Visibilities.Public
            hasModifier("PROTECTED_KEYWORD") -> Visibilities.Protected
            hasModifier("PRIVATE_KEYWORD") -> Visibilities.Private
            else -> JavaDescriptorVisibilities.PACKAGE_VISIBILITY
        } as Visibility

    override val typeParameters: List<JavaTypeParameter>
        get() = node.findChildByType("TYPE_PARAMETER_LIST")?.getChildrenByType("TYPE_PARAMETER")?.map { JavaTypeParameterOverAst(it, source) } ?: emptyList()

    override val supertypes: Collection<JavaClassifierType>
        get() {
            val result = mutableListOf<JavaClassifierType>()
            node.findChildByType("EXTENDS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
                result.add(JavaClassifierTypeOverAst(it, source))
            }
            node.findChildByType("IMPLEMENTS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
                result.add(JavaClassifierTypeOverAst(it, source))
            }
            return result
        }
    override val innerClassNames: Collection<Name>
        get() = node.children.filter { it.type.toString() == "CLASS" }.map { 
            Name.identifier(it.findChildByType("IDENTIFIER")?.text ?: "<error>")
        }

    override fun findInnerClass(name: Name): JavaClass? {
        val innerClassNode = node.children.find { 
            it.type.toString() == "CLASS" && it.findChildByType("IDENTIFIER")?.text == name.asString()
        }
        return innerClassNode?.let { JavaClassOverAst(it, source, this) }
    }

    override val isInterface: Boolean get() = node.findChildByType("INTERFACE_KEYWORD") != null
    override val isAnnotationType: Boolean get() = node.findChildByType("AT") != null && isInterface
    override val isEnum: Boolean get() = node.findChildByType("ENUM_KEYWORD") != null
    override val isRecord: Boolean get() = node.findChildByType("RECORD_KEYWORD") != null
    override val isSealed: Boolean get() = false
    override val permittedTypes: Sequence<JavaClassifierType> get() = emptySequence()
    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod>
        get() = node.getChildrenByType("METHOD").filter { it.findChildByType("TYPE") != null }.map { JavaMethodOverAst(it, source, this) }

    override val fields: Collection<JavaField>
        get() = node.getChildrenByType("FIELD").map { JavaFieldOverAst(it, source, this) }

    override val constructors: Collection<JavaConstructor>
        get() = node.getChildrenByType("METHOD").filter { it.findChildByType("TYPE") == null }.map { JavaConstructorOverAst(it, source, this) }
    override val recordComponents: Collection<JavaRecordComponent> get() = emptyList()

    override fun hasDefaultConstructor(): Boolean = false

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.getChildrenByType("ANNOTATION")?.map { JavaAnnotationOverAst(it, source) } ?: emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isFromSource: Boolean get() = true
}
