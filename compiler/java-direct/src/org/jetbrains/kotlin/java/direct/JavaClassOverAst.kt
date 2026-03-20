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
) : JavaElementOverAst(node), JavaClass {

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

    // Interfaces and annotation types are implicitly abstract; enums/annotations with abstract
    // methods (each constant overrides) are also abstract per JLS 8.1.1.1 / 9.6.1
    override val isAbstract: Boolean
        get() = hasModifier("ABSTRACT_KEYWORD") || isInterface ||
                ((isAnnotationType || isEnum) && methods.any { it.isAbstract })

    // Java nested interfaces and enums are implicitly static even without the keyword
    // Classes nested inside interfaces are also implicitly static (JLS 9.5)
    override val isStatic: Boolean get() = hasModifier("STATIC_KEYWORD") || (outerClass != null && (isInterface || isEnum)) || (outerClass?.isInterface == true)

    // Enums are implicitly final (JLS 8.9) unless they have abstract methods (subclass per constant)
    override val isFinal: Boolean get() = (isEnum && !methods.any { it.isAbstract }) || hasModifier("FINAL_KEYWORD")

    override val visibility: Visibility
        get() = when {
            // Nested type declarations in interfaces are implicitly public (JLS 9.5)
            outerClass?.isInterface == true -> Visibilities.Public
            hasModifier("PUBLIC_KEYWORD") -> Visibilities.Public
            // Protected nested classes are visible in same package + subclasses (like members)
            hasModifier("PROTECTED_KEYWORD") -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
            hasModifier("PRIVATE_KEYWORD") -> Visibilities.Private
            else -> JavaVisibilities.PackageVisibility
        }

    override val typeParameters: List<JavaTypeParameter> by lazy {
        computeTypeParameters(node, resolutionContext)
    }

    override val supertypes: Collection<JavaClassifierType>
        get() {
            val result = mutableListOf<JavaClassifierType>()

            // Add implicit supertypes for special class kinds
            if (isEnum) {
                // Enums implicitly extend java.lang.Enum<E>
                result.add(EnumSupertypeForJavaDirect(this))
            } else if (isAnnotationType) {
                // Annotation types implicitly implement java.lang.annotation.Annotation
                result.add(SimpleClassifierType("java.lang.annotation.Annotation"))
            }

            // Explicit supertypes can reference class type parameters (e.g., class Foo<T> extends Bar<T>)
            node.findChildByType("EXTENDS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
                result.add(JavaClassifierTypeOverAst(it, memberResolutionContext))
            }

            // Add implicit java.lang.Object for classes without explicit extends (not interfaces)
            if (result.isEmpty() && !isInterface) {
                result.add(SimpleClassifierType("java.lang.Object"))
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
        } ?: return null

        // Check if the inner class is effectively static:
        // - Explicitly marked with 'static' keyword
        // - Is an interface (interfaces are implicitly static in Java)
        // - Is an enum (enums are implicitly static in Java)
        val hasStaticKeyword = innerClassNode.findChildByType("MODIFIER_LIST")
            ?.children?.any { it.type.toString() == "STATIC_KEYWORD" } ?: false
        val isInterface = innerClassNode.findChildByType("INTERFACE_KEYWORD") != null
        val isEnum = innerClassNode.findChildByType("ENUM_KEYWORD") != null
        val innerIsEffectivelyStatic = hasStaticKeyword || isInterface || isEnum

        // Non-static inner classes get outer type params as OWN (high priority, can't be shadowed
        // by inner class names) via memberResolutionContext.
        // Static nested types (interfaces/enums/static classes) get outer type params as INHERITED
        // (low priority, can be shadowed by inner class names of the static nested type itself).
        // This matches Java's scoping rules where static nested types see outer type params but
        // inner class names of the nested type shadow them.
        val contextForInner = if (innerIsEffectivelyStatic)
            resolutionContext.withContainingClass(this).withInheritedTypeParameters(typeParameters)
        else
            memberResolutionContext
        return JavaClassOverAst(innerClassNode, contextForInner, outerClass = this)
    }

    override val isInterface: Boolean get() = node.findChildByType("INTERFACE_KEYWORD") != null
    override val isAnnotationType: Boolean get() = node.findChildByType("AT") != null && isInterface
    override val isEnum: Boolean get() = node.findChildByType("ENUM_KEYWORD") != null
    // Note: Parser produces "RECORD" token, not "RECORD_KEYWORD" (SyntaxElementType("RECORD"))
    override val isRecord: Boolean get() = node.findChildByType("RECORD") != null
    // Note: Parser produces "SEALED" token, not "SEALED_KEYWORD" (SyntaxElementType("SEALED"))
    override val isSealed: Boolean get() = hasModifier("SEALED")

    override val permittedTypes: Sequence<JavaClassifierType>
        get() {
            val permitsList = node.findChildByType("PERMITS_LIST")
            if (permitsList != null) {
                return permitsList.children
                    .filter { it.type.toString() == "JAVA_CODE_REFERENCE" }
                    .map { JavaClassifierTypeOverAst(it, memberResolutionContext) }
                    .asSequence()
            }
            // No explicit permits clause — sealed class: derive permitted types from direct
            // subtypes in the same compilation unit (JLS 13.4.27).
            if (!isSealed) return emptySequence()
            return deriveImplicitPermittedTypes()
        }

    private fun deriveImplicitPermittedTypes(): Sequence<JavaClassifierType> {
        val myName = name.asString()
        val myFqName = fqName?.asString() ?: myName
        return node.children
            .filter { it.type.toString() == "CLASS" }
            .filter { innerNode ->
                // Check if the inner class directly extends/implements this sealed type
                val extendsRefs = innerNode.findChildByType("EXTENDS_LIST")
                    ?.getChildrenByType("JAVA_CODE_REFERENCE")
                    ?.map { it.text.substringBefore('<').trim() }
                    ?: emptyList()
                val implementsRefs = innerNode.findChildByType("IMPLEMENTS_LIST")
                    ?.getChildrenByType("JAVA_CODE_REFERENCE")
                    ?.map { it.text.substringBefore('<').trim() }
                    ?: emptyList()
                (extendsRefs + implementsRefs).any { ref -> ref == myName || ref == myFqName }
            }
            .mapNotNull { innerNode ->
                val innerName = innerNode.findChildByType("IDENTIFIER")?.text ?: return@mapNotNull null
                SimpleClassifierType("$myFqName.$innerName")
            }
            .asSequence()
    }
    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod>
        get() {
            // Both regular methods and annotation interface methods need to be included
            val methodNodes = node.getChildrenByType("METHOD") + node.getChildrenByType("ANNOTATION_METHOD")
            return methodNodes
                .filter { it.findChildByType("TYPE") != null }
                .map { JavaMethodOverAst(it, this) }
        }

    override val fields: Collection<JavaField>
        get() {
            // Include both regular fields and enum constants
            val fieldNodes = node.getChildrenByType("FIELD") + node.getChildrenByType("ENUM_CONSTANT")
            return fieldNodes.map { JavaFieldOverAst(it, this) }
        }

    override val constructors: Collection<JavaConstructor>
        get() = node.getChildrenByType("METHOD")
            .filter { it.findChildByType("TYPE") == null && it.findChildByType("IDENTIFIER") != null }
            .map { JavaConstructorOverAst(it, this) }

    override val recordComponents: Collection<JavaRecordComponent>
        get() {
            val header = node.findChildByType("RECORD_HEADER") ?: return emptyList()
            return header.getChildrenByType("RECORD_COMPONENT")
                .map { JavaRecordComponentOverAst(it, this) }
        }

    override fun hasDefaultConstructor(): Boolean = !isInterface && constructors.isEmpty()

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.getChildrenByType("ANNOTATION")
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isFromSource: Boolean get() = true
}
