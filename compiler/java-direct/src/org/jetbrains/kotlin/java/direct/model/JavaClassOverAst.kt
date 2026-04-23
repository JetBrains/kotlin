/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.util.NOT_COMPUTED
import org.jetbrains.kotlin.java.direct.util.cachedBoolean
import org.jetbrains.kotlin.java.direct.util.cachedNonNull
import org.jetbrains.kotlin.java.direct.util.cachedNullable
import org.jetbrains.kotlin.java.direct.util.computeTypeParameters
import org.jetbrains.kotlin.java.direct.util.isDeprecatedInJavaDoc
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

class JavaClassOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    val resolutionContext: JavaResolutionContext,
    override val outerClass: JavaClass? = null,
) : JavaElementOverAst(node, tree), JavaClass {

    @Volatile private var _memberResolutionContext: JavaResolutionContext? = null
    val memberResolutionContext: JavaResolutionContext
        get() = cachedNonNull(
            { _memberResolutionContext },
            { _memberResolutionContext = it }) {
            resolutionContext.withContainingClass(this).withTypeParameters(typeParameters)
        }

    override val name: Name
        get() = Name.identifier(
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() } ?: "<error>"
        )

    @Volatile private var _fqName: Any? = NOT_COMPUTED
    override val fqName: FqName?
        get() = cachedNullable({ _fqName }, { _fqName = it }) { computeFqName() }

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
    private val modifierList: JavaLightNode?
        get() = cachedNullable({ _modifierList }, { _modifierList = it }) {
            tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
        }

    private fun hasModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
    }

    override val isAbstract: Boolean
        get() = hasModifier(JavaSyntaxTokenType.ABSTRACT_KEYWORD) || isInterface ||
                ((isAnnotationType || isEnum) && methods.any { it.isAbstract })

    override val isStatic: Boolean
        get() = hasModifier(JavaSyntaxTokenType.STATIC_KEYWORD) || (outerClass != null && (isInterface || isEnum)) || (outerClass?.isInterface == true)

    override val isFinal: Boolean
        get() = (isEnum && !methods.any { it.isAbstract }) || hasModifier(JavaSyntaxTokenType.FINAL_KEYWORD)

    override val visibility: Visibility
        get() = when {
            outerClass?.isInterface == true -> Visibilities.Public
            hasModifier(JavaSyntaxTokenType.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(JavaSyntaxTokenType.PROTECTED_KEYWORD) -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
            hasModifier(JavaSyntaxTokenType.PRIVATE_KEYWORD) -> Visibilities.Private
            else -> JavaVisibilities.PackageVisibility
        }

    @Volatile private var _typeParameters: List<JavaTypeParameter>? = null
    override val typeParameters: List<JavaTypeParameter>
        get() = cachedNonNull({ _typeParameters }, { _typeParameters = it }) {
            computeTypeParameters(node, tree, resolutionContext)
        }

    @Volatile private var _supertypes: Collection<JavaClassifierType>? = null
    override val supertypes: Collection<JavaClassifierType>
        get() = cachedNonNull(
            { _supertypes },
            { _supertypes = it }) { computeSupertypes() }

    private fun computeSupertypes(): List<JavaClassifierType> {
        val result = mutableListOf<JavaClassifierType>()

        if (isEnum) {
            result.add(EnumSupertypeForJavaDirect(this))
        } else if (isAnnotationType) {
            result.add(SimpleClassifierType("java.lang.annotation.Annotation"))
        }

        tree.findChildByType(node, JavaSyntaxElementType.EXTENDS_LIST)?.let { extList ->
            tree.getChildrenByType(extList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach {
                result.add(JavaClassifierTypeOverAst(it, tree, memberResolutionContext))
            }
        }

        if (result.isEmpty() && !isInterface) {
            result.add(SimpleClassifierType("java.lang.Object"))
        }

        tree.findChildByType(node, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { implList ->
            tree.getChildrenByType(implList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach {
                result.add(JavaClassifierTypeOverAst(it, tree, memberResolutionContext))
            }
        }
        return result
    }

    @Volatile private var _innerClassNames: Collection<Name>? = null
    override val innerClassNames: Collection<Name>
        get() = cachedNonNull({ _innerClassNames }, { _innerClassNames = it }) {
            tree.getChildren(node).filter { tree.getType(it) == JavaSyntaxElementType.CLASS }.map {
                Name.identifier(tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() }
                                    ?: "<error>")
            }
        }

    private val innerClassCache: ConcurrentHashMap<Name, Any> = ConcurrentHashMap()

    override fun findInnerClass(name: Name): JavaClass? {
        innerClassCache[name]?.let { return if (it === NULL_INNER_CLASS) null else it as JavaClass }
        val cached = innerClassCache.computeIfAbsent(name) { findInnerClassUncached(it) ?: NULL_INNER_CLASS }
        return if (cached === NULL_INNER_CLASS) null else cached as JavaClass
    }

    private companion object {
        private val NULL_INNER_CLASS: Any = Any()
    }

    private fun findInnerClassUncached(name: Name): JavaClass? {
        val nameString = name.asString()
        val innerClassNode = tree.getChildren(node).find { child ->
            tree.getType(child) == JavaSyntaxElementType.CLASS &&
                    tree.findChildByType(child, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.textEquals(it, nameString) } == true
        }

        if (innerClassNode != null) {
            // Check if the inner class is effectively static:
            // - Explicitly marked with 'static' keyword
            // - Is an interface (interfaces are implicitly static in Java)
            // - Is an enum (enums are implicitly static in Java)
            val hasStaticKeyword = tree.findChildByType(innerClassNode, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
                tree.hasChildOfType(ml, JavaSyntaxTokenType.STATIC_KEYWORD)
            } ?: false
            val innerIsInterface = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
            val innerIsEnum = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.ENUM_KEYWORD) != null
            val innerIsEffectivelyStatic = hasStaticKeyword || innerIsInterface || innerIsEnum

            // Non-static inner classes get outer type params as OWN (high priority, can't be shadowed
            // by inner class names) via memberResolutionContext.
            // Static nested types get them as INHERITED (low priority, shadowable by inner class names).
            val contextForInner = if (innerIsEffectivelyStatic)
                resolutionContext.withContainingClass(this).withInheritedTypeParameters(typeParameters)
            else
                memberResolutionContext
            return JavaClassOverAst(innerClassNode, tree, contextForInner, outerClass = this)
        }

        // Inner class not directly declared — search supertypes (JLS 8.5: inherited member types).
        // This handles cases like SimpleFunctionDescriptor.CopyBuilder where CopyBuilder is
        // declared in FunctionDescriptor (superinterface) but referenced via SimpleFunctionDescriptor.
        return findInnerClassInSupertypes(name, mutableSetOf())
    }

    /**
     * Searches for an inner class in the supertypes of this class, working purely on raw AST text.
     *
     * This is intentionally distinct from [org.jetbrains.kotlin.java.direct.resolution.JavaInheritedMemberResolver.findInnerClassFromSupertypes]:
     *
     * | Aspect            | This method (`JavaClassOverAst`)                       | `JavaInheritedMemberResolver`                          |
     * |-------------------|--------------------------------------------------------|--------------------------------------------------------|
     * | Input             | Raw `EXTENDS_LIST` / `IMPLEMENTS_LIST` AST text        | Resolved `javaClass.supertypes` (full [JavaClassifierType]) |
     * | Resolution depth  | Simple-name lookup via [JavaResolutionContext.findLocalClass] | Full classifier resolution + cross-file ambiguity check |
     * | Caller context    | Inside [findInnerClass] — recursion sentinel for the model layer | Top-level resolver entry point used by [org.jetbrains.kotlin.java.direct.resolution.JavaScopeResolver] |
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
        tree.findChildByType(node, JavaSyntaxElementType.EXTENDS_LIST)?.let { extList ->
            tree.getChildrenByType(extList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                supertypeRefNames.add(tree.getText(ref).toString().substringBefore('<').trim())
            }
        }
        tree.findChildByType(node, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { implList ->
            tree.getChildrenByType(implList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                supertypeRefNames.add(tree.getText(ref).toString().substringBefore('<').trim())
            }
        }

        val nameString = name.asString()
        for (supertypeRef in supertypeRefNames) {
            val simpleName = supertypeRef.substringBefore('.')
            val supertypeClass = resolutionContext.findLocalClass(Name.identifier(simpleName))
                    as? JavaClassOverAst ?: continue

            val directInner = supertypeClass.tree.getChildren(supertypeClass.node).find { child ->
                supertypeClass.tree.getType(child) == JavaSyntaxElementType.CLASS &&
                        supertypeClass.tree.findChildByType(child, JavaSyntaxTokenType.IDENTIFIER)?.let {
                            supertypeClass.tree.textEquals(it, nameString)
                        } == true
            }
            if (directInner != null) {
                return supertypeClass.findInnerClass(name)
            }

            supertypeClass.findInnerClassInSupertypes(name, visited)?.let { return it }
        }
        return null
    }

    @Volatile private var _isInterface: Int = -1
    override val isInterface: Boolean
        get() = cachedBoolean({ _isInterface }, { _isInterface = it }) {
            tree.findChildByType(node, JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
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
        get() = cachedBoolean(
            { _isAnnotationType },
            { _isAnnotationType = it }) { computeIsAnnotationType() }

    private fun computeIsAnnotationType(): Boolean {
        // Whitespace is excluded from children by JavaLightTree, so AT is directly
        // followed by INTERFACE_KEYWORD for `@interface` declarations.
        val children = tree.getChildren(node)
        for (i in 0 until children.size - 1) {
            if (tree.getType(children[i]) == JavaSyntaxTokenType.AT &&
                tree.getType(children[i + 1]) == JavaSyntaxTokenType.INTERFACE_KEYWORD
            ) return true
        }
        return false
    }

    @Volatile private var _isEnum: Int = -1
    override val isEnum: Boolean
        get() = cachedBoolean({ _isEnum }, { _isEnum = it }) {
            tree.findChildByType(node, JavaSyntaxTokenType.ENUM_KEYWORD) != null
        }

    @Volatile private var _isRecord: Int = -1
    override val isRecord: Boolean
        get() = cachedBoolean({ _isRecord }, { _isRecord = it }) {
            tree.findChildByType(node, JavaSyntaxTokenType.RECORD_KEYWORD) != null
        }

    @Volatile private var _isSealed: Int = -1
    override val isSealed: Boolean
        get() = cachedBoolean({ _isSealed }, { _isSealed = it }) {
            hasModifier(
                JavaSyntaxTokenType.SEALED_KEYWORD
            )
        }

    override val permittedTypes: Sequence<JavaClassifierType>
        get() {
            val permitsList = tree.findChildByType(node, JavaSyntaxElementType.PERMITS_LIST)
            if (permitsList != null) {
                return tree.getChildren(permitsList)
                    .filter { tree.getType(it) == JavaSyntaxElementType.JAVA_CODE_REFERENCE }
                    .map { JavaClassifierTypeOverAst(it, tree, memberResolutionContext) }
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
        return tree.getChildren(node)
            .filter { tree.getType(it) == JavaSyntaxElementType.CLASS }
            .filter { innerNode ->
                // Check if the inner class directly extends/implements this sealed type
                val extendsRefs = tree.findChildByType(innerNode, JavaSyntaxElementType.EXTENDS_LIST)?.let { el ->
                    tree.getChildrenByType(el, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
                        .map { tree.getText(it).toString().substringBefore('<').trim() }
                } ?: emptyList()
                val implementsRefs = tree.findChildByType(innerNode, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { il ->
                    tree.getChildrenByType(il, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
                        .map { tree.getText(it).toString().substringBefore('<').trim() }
                } ?: emptyList()
                (extendsRefs + implementsRefs).any { ref -> ref == myName || ref == myFqName }
            }
            .mapNotNull { innerNode ->
                val innerName = tree.findChildByType(innerNode, JavaSyntaxTokenType.IDENTIFIER)?.let {
                    tree.getText(it).toString()
                } ?: return@mapNotNull null
                SimpleClassifierType("$myFqName.$innerName")
            }
            .asSequence()
    }

    override val lightClassOriginKind: LightClassOriginKind? get() = null

    @Volatile private var _methods: Collection<JavaMethod>? = null
    override val methods: Collection<JavaMethod>
        get() = cachedNonNull({ _methods }, { _methods = it }) {
            val methodNodes =
                tree.getChildrenByType(node, JavaSyntaxElementType.METHOD) + tree.getChildrenByType(
                    node,
                    JavaSyntaxElementType.ANNOTATION_METHOD
                )
            methodNodes
                .filter { tree.findChildByType(it, JavaSyntaxElementType.TYPE) != null }
                .map { JavaMethodOverAst(it, tree, this) }
        }

    @Volatile private var _fields: Collection<JavaField>? = null
    override val fields: Collection<JavaField>
        get() = cachedNonNull({ _fields }, { _fields = it }) {
            val fieldNodes = tree.getChildrenByType(node, JavaSyntaxElementType.FIELD) +
                    tree.getChildrenByType(node, JavaSyntaxElementType.ENUM_CONSTANT)
            fieldNodes.map { JavaFieldOverAst(it, tree, this) }
        }

    @Volatile private var _constructors: Collection<JavaConstructor>? = null
    override val constructors: Collection<JavaConstructor>
        get() = cachedNonNull({ _constructors }, { _constructors = it }) {
            tree.getChildrenByType(node, JavaSyntaxElementType.METHOD)
                .filter {
                    tree.findChildByType(it, JavaSyntaxElementType.TYPE) == null &&
                            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER) != null
                }
                .map { JavaConstructorOverAst(it, tree, this) }
        }

    @Volatile private var _recordComponents: Collection<JavaRecordComponent>? = null
    override val recordComponents: Collection<JavaRecordComponent>
        get() = cachedNonNull({ _recordComponents }, { _recordComponents = it }) {
            val header = tree.findChildByType(node, JavaSyntaxElementType.RECORD_HEADER)
            if (header != null) {
                tree.getChildrenByType(header, JavaSyntaxElementType.RECORD_COMPONENT)
                    .map { JavaRecordComponentOverAst(it, tree, this) }
            } else emptyList()
        }

    override fun hasDefaultConstructor(): Boolean = !isInterface && constructors.isEmpty()

    @Volatile private var _annotations: Collection<JavaAnnotation>? = null
    override val annotations: Collection<JavaAnnotation>
        get() = cachedNonNull({ _annotations }, { _annotations = it }) {
            modifierList?.let { ml ->
                tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                    .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
            } ?: emptyList()
        }

    override val isDeprecatedInJavaDoc: Boolean
        get() = isDeprecatedInJavaDoc(tree, node)

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isFromSource: Boolean get() = true
}
