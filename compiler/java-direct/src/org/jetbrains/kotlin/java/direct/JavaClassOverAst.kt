/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
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
    val memberResolutionContext: JavaResolutionContext by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolutionContext
            .withContainingClass(this)
            .withTypeParameters(typeParameters)
    }

    override val name: Name
        get() = Name.identifier(node.children.find { it.type == JavaSyntaxTokenType.IDENTIFIER }?.text ?: "<error>")

    override val fqName: FqName? by lazy(LazyThreadSafetyMode.PUBLICATION) {
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
        result
    }

    private val modifierList: JavaSyntaxNode? by lazy(LazyThreadSafetyMode.PUBLICATION) { node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST) }

    private fun hasModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.children?.any { it.type == modifier } ?: false
    }

    // Interfaces and annotation types are implicitly abstract; enums/annotations with abstract
    // methods (each constant overrides) are also abstract per JLS 8.1.1.1 / 9.6.1
    override val isAbstract: Boolean
        get() = hasModifier(JavaSyntaxTokenType.ABSTRACT_KEYWORD) || isInterface ||
                ((isAnnotationType || isEnum) && methods.any { it.isAbstract })

    // Java nested interfaces and enums are implicitly static even without the keyword
    // Classes nested inside interfaces are also implicitly static (JLS 9.5)
    override val isStatic: Boolean get() = hasModifier(JavaSyntaxTokenType.STATIC_KEYWORD) || (outerClass != null && (isInterface || isEnum)) || (outerClass?.isInterface == true)

    // Enums are implicitly final (JLS 8.9) unless they have abstract methods (subclass per constant)
    override val isFinal: Boolean get() = (isEnum && !methods.any { it.isAbstract }) || hasModifier(JavaSyntaxTokenType.FINAL_KEYWORD)

    override val visibility: Visibility
        get() = when {
            // Nested type declarations in interfaces are implicitly public (JLS 9.5)
            outerClass?.isInterface == true -> Visibilities.Public
            hasModifier(JavaSyntaxTokenType.PUBLIC_KEYWORD) -> Visibilities.Public
            // Protected nested classes are visible in same package + subclasses (like members)
            hasModifier(JavaSyntaxTokenType.PROTECTED_KEYWORD) -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
            hasModifier(JavaSyntaxTokenType.PRIVATE_KEYWORD) -> Visibilities.Private
            else -> JavaVisibilities.PackageVisibility
        }

    override val typeParameters: List<JavaTypeParameter> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computeTypeParameters(node, resolutionContext)
    }

    override val supertypes: Collection<JavaClassifierType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
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
        node.findChildByType(JavaSyntaxElementType.EXTENDS_LIST)?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.forEach {
            result.add(JavaClassifierTypeOverAst(it, memberResolutionContext))
        }

        // Add implicit java.lang.Object for classes without explicit extends (not interfaces)
        if (result.isEmpty() && !isInterface) {
            result.add(SimpleClassifierType("java.lang.Object"))
        }

        node.findChildByType(JavaSyntaxElementType.IMPLEMENTS_LIST)?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.forEach {
            result.add(JavaClassifierTypeOverAst(it, memberResolutionContext))
        }
        result
    }

    override val innerClassNames: Collection<Name> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        node.children.filter { it.type == JavaSyntaxElementType.CLASS }.map {
            Name.identifier(it.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text ?: "<error>")
        }
    }

    private val innerClassCache = HashMap<Name, JavaClass?>()

    override fun findInnerClass(name: Name): JavaClass? {
        innerClassCache[name]?.let { return it }
        if (innerClassCache.containsKey(name)) return null // cached null

        val result = findInnerClassUncached(name)
        innerClassCache[name] = result
        return result
    }

    private fun findInnerClassUncached(name: Name): JavaClass? {
        val nameString = name.asString()
        val innerClassNode = node.children.find {
            it.type == JavaSyntaxElementType.CLASS &&
                    it.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.textEquals(nameString) == true
        }

        if (innerClassNode != null) {
            // Check if the inner class is effectively static:
            // - Explicitly marked with 'static' keyword
            // - Is an interface (interfaces are implicitly static in Java)
            // - Is an enum (enums are implicitly static in Java)
            val hasStaticKeyword = innerClassNode.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
                ?.children?.any { it.type == JavaSyntaxTokenType.STATIC_KEYWORD } ?: false
            val isInterface = innerClassNode.findChildByType(JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
            val isEnum = innerClassNode.findChildByType(JavaSyntaxTokenType.ENUM_KEYWORD) != null
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

        // Inner class not directly declared — search supertypes (JLS 8.5: inherited member types).
        // This handles cases like SimpleFunctionDescriptor.CopyBuilder where CopyBuilder is
        // declared in FunctionDescriptor (superinterface) but referenced via SimpleFunctionDescriptor.
        return findInnerClassInSupertypes(name, mutableSetOf())
    }

    /**
     * Searches for an inner class in the supertypes of this class, working **purely on raw AST text**.
     *
     * This is intentionally distinct from [JavaInheritedMemberResolver.findInnerClassFromSupertypes]:
     *
     * | Aspect            | This method (`JavaClassOverAst`)                       | `JavaInheritedMemberResolver`                          |
     * |-------------------|--------------------------------------------------------|--------------------------------------------------------|
     * | Input             | Raw `EXTENDS_LIST` / `IMPLEMENTS_LIST` AST text        | Resolved `javaClass.supertypes` (full [JavaClassifierType]) |
     * | Resolution depth  | Simple-name lookup via [JavaResolutionContext.findLocalClass] | Full classifier resolution + cross-file ambiguity check |
     * | Caller context    | Inside [findInnerClass] — recursion sentinel for the model layer | Top-level resolver entry point used by [JavaScopeResolver] |
     * | Recursion guard   | `visited: MutableSet<String>` of FQN strings           | `visited: MutableSet<JavaClass>` of model instances    |
     *
     * The two paths cannot be unified because **this method must avoid triggering full type
     * resolution** — calling `javaClass.supertypes` here would re-enter type construction, which
     * itself calls `classifier → findLocalClass → findInnerClass`, producing infinite recursion.
     * Conversely, the inherited-member resolver requires resolved supertypes to detect cross-file
     * ambiguities that simple-name AST scanning cannot see.
     */
    private fun findInnerClassInSupertypes(name: Name, visited: MutableSet<String>): JavaClass? {
        val myId = fqName?.asString() ?: return null
        if (myId in visited) return null
        visited.add(myId)

        val supertypeRefNames = mutableListOf<String>()
        node.findChildByType(JavaSyntaxElementType.EXTENDS_LIST)?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
            ?.forEach { ref ->
                supertypeRefNames.add(ref.text.substringBefore('<').trim())
            }
        node.findChildByType(JavaSyntaxElementType.IMPLEMENTS_LIST)?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
            ?.forEach { ref ->
                supertypeRefNames.add(ref.text.substringBefore('<').trim())
            }

        val nameString = name.asString()
        for (supertypeRef in supertypeRefNames) {
            // Use only the first part (simple name) to find the supertype class locally
            val simpleName = supertypeRef.substringBefore('.')
            val supertypeClass = resolutionContext.findLocalClass(Name.identifier(simpleName))
                    as? JavaClassOverAst ?: continue

            // Check if the supertype directly declares this inner class
            val directInner = supertypeClass.node.children.find {
                it.type == JavaSyntaxElementType.CLASS &&
                        it.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.textEquals(nameString) == true
            }
            if (directInner != null) {
                // Found the inner class — return it properly constructed
                return supertypeClass.findInnerClass(name)
            }

            // Recursively check the supertype's supertypes
            supertypeClass.findInnerClassInSupertypes(name, visited)?.let { return it }
        }
        return null
    }

    override val isInterface: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { node.findChildByType(JavaSyntaxTokenType.INTERFACE_KEYWORD) != null }
    override val isAnnotationType: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { node.findChildByType(JavaSyntaxTokenType.AT) != null && isInterface }
    override val isEnum: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { node.findChildByType(JavaSyntaxTokenType.ENUM_KEYWORD) != null }
    override val isRecord: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { node.findChildByType(JavaSyntaxTokenType.RECORD_KEYWORD) != null }
    override val isSealed: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { hasModifier(JavaSyntaxTokenType.SEALED_KEYWORD) }

    override val permittedTypes: Sequence<JavaClassifierType>
        get() {
            val permitsList = node.findChildByType(JavaSyntaxElementType.PERMITS_LIST)
            if (permitsList != null) {
                return permitsList.children
                    .filter { it.type == JavaSyntaxElementType.JAVA_CODE_REFERENCE }
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
            .filter { it.type == JavaSyntaxElementType.CLASS }
            .filter { innerNode ->
                // Check if the inner class directly extends/implements this sealed type
                val extendsRefs = innerNode.findChildByType(JavaSyntaxElementType.EXTENDS_LIST)
                    ?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
                    ?.map { it.text.substringBefore('<').trim() }
                    ?: emptyList()
                val implementsRefs = innerNode.findChildByType(JavaSyntaxElementType.IMPLEMENTS_LIST)
                    ?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
                    ?.map { it.text.substringBefore('<').trim() }
                    ?: emptyList()
                (extendsRefs + implementsRefs).any { ref -> ref == myName || ref == myFqName }
            }
            .mapNotNull { innerNode ->
                val innerName = innerNode.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text ?: return@mapNotNull null
                SimpleClassifierType("$myFqName.$innerName")
            }
            .asSequence()
    }

    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Both regular methods and annotation interface methods need to be included
        val methodNodes =
            node.getChildrenByType(JavaSyntaxElementType.METHOD) + node.getChildrenByType(JavaSyntaxElementType.ANNOTATION_METHOD)
        methodNodes
            .filter { it.findChildByType(JavaSyntaxElementType.TYPE) != null }
            .map { JavaMethodOverAst(it, this) }
    }

    override val fields: Collection<JavaField> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Include both regular fields and enum constants
        val fieldNodes = node.getChildrenByType(JavaSyntaxElementType.FIELD) + node.getChildrenByType(JavaSyntaxElementType.ENUM_CONSTANT)
        fieldNodes.map { JavaFieldOverAst(it, this) }
    }

    override val constructors: Collection<JavaConstructor> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        node.getChildrenByType(JavaSyntaxElementType.METHOD)
            .filter { it.findChildByType(JavaSyntaxElementType.TYPE) == null && it.findChildByType(JavaSyntaxTokenType.IDENTIFIER) != null }
            .map { JavaConstructorOverAst(it, this) }
    }

    override val recordComponents: Collection<JavaRecordComponent> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val header = node.findChildByType(JavaSyntaxElementType.RECORD_HEADER) ?: return@lazy emptyList()
        header.getChildrenByType(JavaSyntaxElementType.RECORD_COMPONENT)
            .map { JavaRecordComponentOverAst(it, this) }
    }

    override fun hasDefaultConstructor(): Boolean = !isInterface && constructors.isEmpty()

    override val annotations: Collection<JavaAnnotation> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        modifierList?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()
    }

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isFromSource: Boolean get() = true
}
