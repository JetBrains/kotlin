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

class JavaClassOverAst(
    node: JavaSyntaxNode,
    val resolutionContext: JavaResolutionContext,
    override val outerClass: JavaClass? = null,
) : JavaElementOverAst(node, resolutionContext.source), JavaClass {

    /**
     * Resolution context for members of this class, includes the class's own type parameters
     * and allows resolution of inner classes by simple name.
     * Used by fields, methods, constructors, and inner classes to resolve type references.
     */
    val memberResolutionContext: JavaResolutionContext by lazy {
        resolutionContext
            .withContainingClass(this)
            .withTypeParameters(typeParameters)
    }

    override val name: Name
        get() = Name.identifier(node.children.find { it.type.toString() == "IDENTIFIER" }?.text ?: "<error>")

    override val fqName: FqName?
        get() {
            val nestedName = mutableListOf<String>()
            var currentClass: JavaClass? = this
            while (currentClass != null) {
                nestedName.add(0, currentClass.name.asString())
                currentClass = currentClass.outerClass
            }

            var result = resolutionContext.packageFqName
            for (n in nestedName) {
                result = result.child(Name.identifier(n))
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
            else -> JavaVisibilities.PackageVisibility
        }

    override val typeParameters: List<JavaTypeParameter>
        get() = node.findChildByType("TYPE_PARAMETER_LIST")
            ?.getChildrenByType("TYPE_PARAMETER")
            ?.map { JavaTypeParameterOverAst(it, resolutionContext) }
            ?: emptyList()

    override val supertypes: Collection<JavaClassifierType>
        get() {
            val result = mutableListOf<JavaClassifierType>()
            // Supertypes can reference class type parameters (e.g., class Foo<T> extends Bar<T>)
            node.findChildByType("EXTENDS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
                result.add(JavaClassifierTypeOverAst(it, memberResolutionContext))
            }
            node.findChildByType("IMPLEMENTS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
                result.add(JavaClassifierTypeOverAst(it, memberResolutionContext))
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
        // Non-static inner classes see outer class type parameters; static nested classes don't
        val innerIsStatic = innerClassNode?.findChildByType("MODIFIER_LIST")
            ?.children?.any { it.type.toString() == "STATIC_KEYWORD" } ?: false
        val contextForInner = if (innerIsStatic) resolutionContext else memberResolutionContext
        return innerClassNode?.let { JavaClassOverAst(it, contextForInner, outerClass = this) }
    }

    override val isInterface: Boolean get() = node.findChildByType("INTERFACE_KEYWORD") != null
    override val isAnnotationType: Boolean get() = node.findChildByType("AT") != null && isInterface
    override val isEnum: Boolean get() = node.findChildByType("ENUM_KEYWORD") != null
    override val isRecord: Boolean get() = node.findChildByType("RECORD_KEYWORD") != null
    override val isSealed: Boolean get() = false
    override val permittedTypes: Sequence<JavaClassifierType> get() = emptySequence()
    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod>
        get() = node.getChildrenByType("METHOD")
            .filter { it.findChildByType("TYPE") != null }
            .map { JavaMethodOverAst(it, this) }

    override val fields: Collection<JavaField>
        get() = node.getChildrenByType("FIELD").map { JavaFieldOverAst(it, this) }

    override val constructors: Collection<JavaConstructor>
        get() = node.getChildrenByType("METHOD")
            .filter { it.findChildByType("TYPE") == null }
            .map { JavaConstructorOverAst(it, this) }

    override val recordComponents: Collection<JavaRecordComponent> get() = emptyList()

    override fun hasDefaultConstructor(): Boolean = !isInterface && constructors.isEmpty()

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.getChildrenByType("ANNOTATION")
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isFromSource: Boolean get() = true
}
