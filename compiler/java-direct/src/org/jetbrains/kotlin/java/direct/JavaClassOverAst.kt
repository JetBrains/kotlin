/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

class JavaClassOverAst(
    node: JavaSyntaxNode,
    val resolutionContext: JavaResolutionContext,
    override val outerClass: JavaClass? = null,
) : JavaElementOverAst(node), JavaClass {

    // Performance: manual @Volatile fields replace `by lazy(PUBLICATION)` delegates.
    // JavaClassOverAst has 16 lazy properties; at ~32 bytes each that's 512 bytes per instance.
    // With 5,000 classes in a large project, this saves ~2.5 MB of delegate overhead.
    // Same safe-publication JMM semantics as the lazy(PUBLICATION) pattern.

    /**
     * Resolution context for members of this class, includes the class's own type parameters
     * and allows resolution of inner classes by simple name.
     * Used by fields, methods, constructors, and inner classes to resolve type references.
     */
    @Volatile private var _memberResolutionContext: JavaResolutionContext? = null
    val memberResolutionContext: JavaResolutionContext
        get() {
            _memberResolutionContext?.let { return it }
            val computed = resolutionContext.withContainingClass(this).withTypeParameters(typeParameters)
            _memberResolutionContext = computed
            return computed
        }

    override val name: Name
        get() = Name.identifier(node.children.find { it.type == JavaSyntaxTokenType.IDENTIFIER }?.text ?: "<error>")

    @Volatile private var _fqName: Any? = NOT_COMPUTED
    override val fqName: FqName?
        get() {
            val cached = _fqName
            if (cached !== NOT_COMPUTED) return cached as FqName?
            val computed = computeFqName()
            _fqName = computed
            return computed
        }

    private fun computeFqName(): FqName {
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

    @Volatile private var _modifierList: Any? = NOT_COMPUTED
    private val modifierList: JavaSyntaxNode?
        get() {
            val cached = _modifierList
            if (cached !== NOT_COMPUTED) return cached as JavaSyntaxNode?
            val computed = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
            _modifierList = computed
            return computed
        }

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

    @Volatile private var _typeParameters: List<JavaTypeParameter>? = null
    override val typeParameters: List<JavaTypeParameter>
        get() {
            _typeParameters?.let { return it }
            val computed = computeTypeParameters(node, resolutionContext)
            _typeParameters = computed
            return computed
        }

    @Volatile private var _supertypes: Collection<JavaClassifierType>? = null
    override val supertypes: Collection<JavaClassifierType>
        get() {
            _supertypes?.let { return it }
            val computed = computeSupertypes()
            _supertypes = computed
            return computed
        }

    private fun computeSupertypes(): List<JavaClassifierType> {
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
        return result
    }

    @Volatile private var _innerClassNames: Collection<Name>? = null
    override val innerClassNames: Collection<Name>
        get() {
            _innerClassNames?.let { return it }
            val computed = node.children.filter { it.type == JavaSyntaxElementType.CLASS }.map {
                Name.identifier(it.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text ?: "<error>")
            }
            _innerClassNames = computed
            return computed
        }

    // Consistent with the other caches in this module (JavaClassFinderOverAstImpl, JavaSupertypeGraph):
    // FIR lazy resolution queries Java model concurrently, so caches that straddle request boundaries
    // must be thread-safe. ConcurrentHashMap disallows null values, so negative results are represented
    // by [NULL_INNER_CLASS] (see Step 2.5 / 2.6 / 3.5 of REFACTORING_PLAN.md).
    private val innerClassCache: ConcurrentHashMap<Name, Any> = ConcurrentHashMap()

    override fun findInnerClass(name: Name): JavaClass? {
        innerClassCache[name]?.let { return if (it === NULL_INNER_CLASS) null else it as JavaClass }

        val result = findInnerClassUncached(name)
        innerClassCache[name] = result ?: NULL_INNER_CLASS
        return result
    }

    private companion object {
        private val NULL_INNER_CLASS: Any = Any()
        /** Sentinel for @Volatile nullable properties: distinguishes "not yet computed" from "computed as null". */
        private val NOT_COMPUTED: Any = Any()
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

    // Boolean flags: tri-state @Volatile Int (-1 = not computed, 0 = false, 1 = true)
    @Volatile private var _isInterface: Int = -1
    override val isInterface: Boolean
        get() {
            val cached = _isInterface
            if (cached >= 0) return cached != 0
            val computed = node.findChildByType(JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
            _isInterface = if (computed) 1 else 0
            return computed
        }

    /**
     * A Java `@interface` (annotation declaration) is represented by the KMP parser as a CLASS
     * node whose direct children contain an `AT` token followed immediately by `INTERFACE_KEYWORD`.
     *
     * A plain interface (`interface Foo`) contains `INTERFACE_KEYWORD` but no `AT` token at the
     * class-header level (any `@Annotation` on the class lives inside the `MODIFIER_LIST` child,
     * not as a direct CLASS child). So a loose `AT present && INTERFACE_KEYWORD present` check
     * is normally sufficient, but we tighten it further to guarantee adjacency — this protects
     * against future parser changes that might surface an annotation token at the CLASS level
     * for a non-annotation interface, and makes the invariant explicit.
     */
    @Volatile private var _isAnnotationType: Int = -1
    override val isAnnotationType: Boolean
        get() {
            val cached = _isAnnotationType
            if (cached >= 0) return cached != 0
            val computed = computeIsAnnotationType()
            _isAnnotationType = if (computed) 1 else 0
            return computed
        }

    private fun computeIsAnnotationType(): Boolean {
        val children = node.children
        var i = 0
        while (i < children.size - 1) {
            if (children[i].type == JavaSyntaxTokenType.AT) {
                // Skip whitespace/comments between AT and the next significant token.
                var j = i + 1
                while (j < children.size && children[j].type == SyntaxTokenTypes.WHITE_SPACE) j++
                if (j < children.size && children[j].type == JavaSyntaxTokenType.INTERFACE_KEYWORD) return true
            }
            i++
        }
        return false
    }

    @Volatile private var _isEnum: Int = -1
    override val isEnum: Boolean
        get() {
            val cached = _isEnum
            if (cached >= 0) return cached != 0
            val computed = node.findChildByType(JavaSyntaxTokenType.ENUM_KEYWORD) != null
            _isEnum = if (computed) 1 else 0
            return computed
        }

    @Volatile private var _isRecord: Int = -1
    override val isRecord: Boolean
        get() {
            val cached = _isRecord
            if (cached >= 0) return cached != 0
            val computed = node.findChildByType(JavaSyntaxTokenType.RECORD_KEYWORD) != null
            _isRecord = if (computed) 1 else 0
            return computed
        }

    @Volatile private var _isSealed: Int = -1
    override val isSealed: Boolean
        get() {
            val cached = _isSealed
            if (cached >= 0) return cached != 0
            val computed = hasModifier(JavaSyntaxTokenType.SEALED_KEYWORD)
            _isSealed = if (computed) 1 else 0
            return computed
        }

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

    @Volatile private var _methods: Collection<JavaMethod>? = null
    override val methods: Collection<JavaMethod>
        get() {
            _methods?.let { return it }
            // Both regular methods and annotation interface methods need to be included
            val methodNodes =
                node.getChildrenByType(JavaSyntaxElementType.METHOD) + node.getChildrenByType(JavaSyntaxElementType.ANNOTATION_METHOD)
            val computed = methodNodes
                .filter { it.findChildByType(JavaSyntaxElementType.TYPE) != null }
                .map { JavaMethodOverAst(it, this) }
            _methods = computed
            return computed
        }

    @Volatile private var _fields: Collection<JavaField>? = null
    override val fields: Collection<JavaField>
        get() {
            _fields?.let { return it }
            // Include both regular fields and enum constants
            val fieldNodes = node.getChildrenByType(JavaSyntaxElementType.FIELD) + node.getChildrenByType(JavaSyntaxElementType.ENUM_CONSTANT)
            val computed = fieldNodes.map { JavaFieldOverAst(it, this) }
            _fields = computed
            return computed
        }

    @Volatile private var _constructors: Collection<JavaConstructor>? = null
    override val constructors: Collection<JavaConstructor>
        get() {
            _constructors?.let { return it }
            val computed = node.getChildrenByType(JavaSyntaxElementType.METHOD)
                .filter { it.findChildByType(JavaSyntaxElementType.TYPE) == null && it.findChildByType(JavaSyntaxTokenType.IDENTIFIER) != null }
                .map { JavaConstructorOverAst(it, this) }
            _constructors = computed
            return computed
        }

    @Volatile private var _recordComponents: Collection<JavaRecordComponent>? = null
    override val recordComponents: Collection<JavaRecordComponent>
        get() {
            _recordComponents?.let { return it }
            val header = node.findChildByType(JavaSyntaxElementType.RECORD_HEADER)
            val computed = if (header != null) {
                header.getChildrenByType(JavaSyntaxElementType.RECORD_COMPONENT)
                    .map { JavaRecordComponentOverAst(it, this) }
            } else emptyList()
            _recordComponents = computed
            return computed
        }

    override fun hasDefaultConstructor(): Boolean = !isInterface && constructors.isEmpty()

    @Volatile private var _annotations: Collection<JavaAnnotation>? = null
    override val annotations: Collection<JavaAnnotation>
        get() {
            _annotations?.let { return it }
            val computed = modifierList?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
            _annotations = computed
            return computed
        }

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isFromSource: Boolean get() = true
}
