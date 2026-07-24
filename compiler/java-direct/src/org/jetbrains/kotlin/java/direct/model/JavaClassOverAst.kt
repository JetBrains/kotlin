/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import org.jetbrains.kotlin.java.direct.util.computeTypeParameters
import org.jetbrains.kotlin.java.direct.util.isDeprecatedInJavaDoc
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

class JavaClassOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    internal val resolutionContext: JavaResolutionContext,
    override val outerClass: JavaClass? = null,
) : JavaElementOverAst(node, tree), JavaClass {

    val memberResolutionContext: JavaResolutionContext by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolutionContext.withContainingClass(this).withTypeParameters(typeParameters)
    }

    override val name: Name by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Name.identifier(identifierText() ?: "<error>")
    }

    override val fqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        outerClass?.fqName?.child(name) ?: resolutionContext.packageFqName.child(name)
    }

    override val modifierList: JavaLightNode? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
    }

    private fun hasModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
    }

    override val isAbstract: Boolean
        get() = hasModifier(JavaSyntaxTokenType.ABSTRACT_KEYWORD) || isInterface ||
                ((isAnnotationType || isEnum) && methods.any { it.isAbstract })

    // Nested interfaces/enums/records are implicitly static (JLS 8.5.1, 8.10.3); any member type
    // of an interface is implicitly static (JLS 9.5). Everything else requires an explicit
    // `static` modifier.
    override val isStatic: Boolean
        get() = hasModifier(JavaSyntaxTokenType.STATIC_KEYWORD) ||
                (outerClass != null && (isInterface || isEnum || isRecord)) ||
                (outerClass?.isInterface == true)

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

    // FIR matches Java type parameters by object identity (see JavaClassCache.kt KDoc): repeated
    // accesses through the same JavaClassOverAst must return the same JavaTypeParameter instances.
    override val typeParameters: List<JavaTypeParameter> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        computeTypeParameters(node, tree, resolutionContext)
    }

    override val supertypes: Collection<JavaClassifierType>
        get() {
            val result = mutableListOf<JavaClassifierType>()

            if (isEnum) {
                result.add(EnumSupertypeForJavaDirect(this, memberResolutionContext))
            } else if (isAnnotationType) {
                result.add(SimpleClassifierType("java.lang.annotation.Annotation", memberResolutionContext))
            }

            tree.findChildByType(node, JavaSyntaxElementType.EXTENDS_LIST)?.let { extList ->
                tree.getChildrenByType(extList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach {
                    result.add(JavaClassifierTypeOverAst(it, tree, memberResolutionContext))
                }
            }

            if (result.isEmpty() && !isInterface) {
                result.add(SimpleClassifierType("java.lang.Object", memberResolutionContext))
            }

            tree.findChildByType(node, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { implList ->
                tree.getChildrenByType(implList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach {
                    result.add(JavaClassifierTypeOverAst(it, tree, memberResolutionContext))
                }
            }
            return result
        }

    override val innerClassNames: Collection<Name>
        get() = tree.getChildren(node).filter { tree.getType(it) == JavaSyntaxElementType.CLASS }.map {
            Name.identifier(classNodeSimpleName(it) ?: "<error>")
        }

    // Positive-only cache: same name → same JavaClass instance. Required so that the
    // JavaTypeParameter instances of inner classes also satisfy FIR's identity contract
    // (see JavaClassCache.kt KDoc). Negative results are intentionally not cached — the
    // perf cost of re-resolving misses is acceptable; identity for nulls is meaningless.
    private val innerClassCache = ConcurrentHashMap<Name, JavaClass>()

    override fun findInnerClass(name: Name): JavaClass? {
        innerClassCache[name]?.let { return it }
        val resolved = findInnerClassImpl(name) ?: return null
        return innerClassCache.putIfAbsent(name, resolved) ?: resolved
    }

    private fun findInnerClassImpl(name: Name): JavaClass? {
        val nameString = name.asString()
        // Like the PSI and binary implementations, `findInnerClass` returns only directly
        // declared member types; inherited ones (JLS 8.5) are resolved by the resolution layer.
        val innerClassNode = tree.getChildren(node).find { child ->
            tree.getType(child) == JavaSyntaxElementType.CLASS &&
                    tree.findChildByType(child, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.textEquals(it, nameString) } == true
        } ?: return null

        // Member interfaces/enums/records are implicitly static (JLS 8.5.1, 8.10.3).
        val hasStaticKeyword = tree.findChildByType(innerClassNode, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
            tree.hasChildOfType(ml, JavaSyntaxTokenType.STATIC_KEYWORD)
        } ?: false
        val innerIsInterface = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
        val innerIsEnum = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.ENUM_KEYWORD) != null
        val innerIsRecord = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.RECORD_KEYWORD) != null
        val innerIsEffectivelyStatic = hasStaticKeyword || innerIsInterface || innerIsEnum || innerIsRecord

        // Non-static inner classes see outer type params as OWN (high priority); static
        // nested types see them as INHERITED (low priority, shadowable by inner class names).
        // Per JLS 6.5.5/8.1.3 outer type params are not in scope in static nested types at
        // all, but PSI resolves them anyway (`JavaClassifierTypeImpl.computeResolveResult`)
        // and java-direct matches it — see `staticNestedTypeParamShadowsImportedClass.kt`.
        // (see also comments to `JavaScopeContext.inheritedTypeParametersInScope`).
        // TODO: remove (KT-87797)
        val contextForInner = if (innerIsEffectivelyStatic)
            resolutionContext.withContainingClass(this).withInheritedTypeParameters(typeParameters)
        else
            memberResolutionContext
        return JavaClassOverAst(innerClassNode, tree, contextForInner, outerClass = this)
    }

    /**
     * Direct supertype reference names exactly as written in the source, with generic arguments
     * stripped. Purely syntactic — does NOT resolve the references, so it is safe to read during
     * resolution without re-entering type construction (unlike `supertypes`).
     */
    internal val directSupertypeRefNames: List<String>
        get() {
            val result = mutableListOf<String>()
            tree.findChildByType(node, JavaSyntaxElementType.EXTENDS_LIST)?.let { extList ->
                tree.getChildrenByType(extList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                    result.add(tree.getText(ref).toString().substringBefore('<').trim())
                }
            }
            tree.findChildByType(node, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { implList ->
                tree.getChildrenByType(implList, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                    result.add(tree.getText(ref).toString().substringBefore('<').trim())
                }
            }
            return result
        }

    override val isInterface: Boolean
        get() = tree.findChildByType(node, JavaSyntaxTokenType.INTERFACE_KEYWORD) != null

    /**
     * A Java `@interface` (annotation declaration) is parsed by the KMP parser as a CLASS node
     * whose direct children contain an `AT` token (immediately before `INTERFACE_KEYWORD`).
     *
     * Checking for a direct-child `AT` is sufficient:
     * - `findChildByType` only scans *direct* children, and the sole `AT` that appears as a direct
     *   child of a CLASS node is the one of `@interface`.
     * - A class-/interface-level `@Annotation` lives nested under the `MODIFIER_LIST` child
     *   (`MODIFIER_LIST → ANNOTATION → AT`), so its `AT` is never a direct CLASS child.
     */
    override val isAnnotationType: Boolean
        get() = tree.findChildByType(node, JavaSyntaxTokenType.AT) != null

    override val isEnum: Boolean
        get() = tree.findChildByType(node, JavaSyntaxTokenType.ENUM_KEYWORD) != null

    override val isRecord: Boolean
        get() = tree.findChildByType(node, JavaSyntaxTokenType.RECORD_KEYWORD) != null

    override val isSealed: Boolean
        get() = hasModifier(JavaSyntaxTokenType.SEALED_KEYWORD)

    override val permittedTypes: Sequence<JavaClassifierType>
        get() {
            val permitsList = tree.findChildByType(node, JavaSyntaxElementType.PERMITS_LIST)
            if (permitsList != null) {
                return tree.getChildren(permitsList)
                    .filter { tree.getType(it) == JavaSyntaxElementType.JAVA_CODE_REFERENCE }
                    .map { JavaClassifierTypeOverAst(it, tree, memberResolutionContext) }
                    .asSequence()
            }
            if (!isSealed) return emptySequence()
            return deriveImplicitPermittedTypes()
        }

    /**
     * Implicit-`permits` fallback: a sealed type without a `permits` clause permits each class
     * declared anywhere in the same compilation unit whose *direct* declared supertype resolves to
     * this type (JLS 8.1.6 / 9.1.4). Mirrors PSI's `JavaClassImpl.lazilyComputePermittedTypesInSameFile`:
     * - the match is resolution-based, like PSI's `isInheritor(this, checkDeep = false)` — a raw
     *   textual match would both miss differently-spelled references to this type and wrongly
     *   accept unrelated types sharing its simple name;
     * - only the candidate enumeration (walking the file for CLASS nodes) is eager; resolution is
     *   deferred behind the returned `Sequence` because it re-enters type construction and could
     *   recurse while this type's own `permittedTypes` is being computed (FIR consumes it from a
     *   deferred `setSealedClassInheritors { ... }` provider).
     */
    private fun deriveImplicitPermittedTypes(): Sequence<JavaClassifierType> {
        val myFqName = fqName
        val candidateNodes = mutableListOf<JavaLightNode>()
        collectClassNodes(tree.getRoot(), candidateNodes)
        return candidateNodes.asSequence().mapNotNull { classNode ->
            val candidate = resolveSameFileClassNode(classNode) ?: return@mapNotNull null
            // A type is never its own subtype; skip it without forcing its supertype resolution.
            if (candidate.fqName == myFqName) return@mapNotNull null
            val isDirectSubtype = candidate.supertypes.any { supertype ->
                (supertype.classifier as? JavaClass)?.fqName == myFqName
            }
            if (isDirectSubtype) ResolvedJavaClassifierType(candidate) else null
        }
    }

    /** Recursively collects every CLASS node under [container]; purely structural. */
    private fun collectClassNodes(container: JavaLightNode, out: MutableList<JavaLightNode>) {
        for (child in tree.getChildren(container)) {
            if (tree.getType(child) != JavaSyntaxElementType.CLASS) continue
            out.add(child)
            collectClassNodes(child, out)
        }
    }

    /**
     * Resolves an arbitrary same-file CLASS node to its [JavaClass] without triggering supertype
     * resolution: the top-level enclosing class is materialized through the file's same-file
     * top-level provider (which builds it against the file-level context) and each nested level is
     * reached with the declared-only [JavaClass.findInnerClass]. Returns `null` if any segment of
     * the enclosing chain cannot be resolved (e.g. a malformed/anonymous node without a name).
     */
    private fun resolveSameFileClassNode(classNode: JavaLightNode): JavaClass? {
        // Build the enclosing CLASS chain (top-level first). Climb while the parent is itself a
        // CLASS, stopping at the compilation-unit root so the synthetic root is never included.
        val rootNode = tree.getRoot()
        val chain = ArrayList<JavaLightNode>()
        var current = classNode
        while (true) {
            chain.add(current)
            val parent = tree.getParent(current) ?: break
            if (parent == rootNode || tree.getType(parent) != JavaSyntaxElementType.CLASS) break
            current = parent
        }
        chain.reverse()
        val topName = classNodeSimpleName(chain.first()) ?: return null
        var resolved: JavaClass =
            resolutionContext.scopeContext.sameFileTopLevelClassProvider(Name.identifier(topName)) ?: return null
        for (i in 1 until chain.size) {
            val nestedName = classNodeSimpleName(chain[i]) ?: return null
            resolved = resolved.findInnerClass(Name.identifier(nestedName)) ?: return null
        }
        return resolved
    }

    private fun classNodeSimpleName(classNode: JavaLightNode): String? =
        tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() }

    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod>
        get() {
            val methodNodes =
                tree.getChildrenByType(node, JavaSyntaxElementType.METHOD) + tree.getChildrenByType(
                    node,
                    JavaSyntaxElementType.ANNOTATION_METHOD
                )
            return methodNodes
                .filter { tree.findChildByType(it, JavaSyntaxElementType.TYPE) != null }
                .map { JavaMethodOverAst(it, tree, this) }
        }

    override val fields: Collection<JavaField>
        get() {
            val fieldNodes = tree.getChildrenByType(node, JavaSyntaxElementType.FIELD) +
                    tree.getChildrenByType(node, JavaSyntaxElementType.ENUM_CONSTANT)
            return fieldNodes.map { JavaFieldOverAst(it, tree, this) }
        }

    override val constructors: Collection<JavaConstructor>
        // A constructor is a METHOD node with no return TYPE (mirrors PSI's
        // `getReturnTypeElement() == null`); the name is irrelevant. A malformed nameless
        // declaration like `void () {}` — whose `void` is an error element, not a return type —
        // is therefore a (package-private) constructor, matching PSI and suppressing the
        // synthesized default constructor.
        get() = tree.getChildrenByType(node, JavaSyntaxElementType.METHOD)
            .filter { tree.findChildByType(it, JavaSyntaxElementType.TYPE) == null }
            .map { JavaConstructorOverAst(it, tree, this) }

    override val recordComponents: Collection<JavaRecordComponent>
        get() {
            val header = tree.findChildByType(node, JavaSyntaxElementType.RECORD_HEADER)
            return if (header != null) {
                tree.getChildrenByType(header, JavaSyntaxElementType.RECORD_COMPONENT)
                    .map { JavaRecordComponentOverAst(it, tree, this) }
            } else emptyList()
        }

    override fun hasDefaultConstructor(): Boolean = !isInterface && constructors.isEmpty()

    override val annotations: Collection<JavaAnnotation>
        get() = parseAnnotationsFromModifierList(modifierList, tree, resolutionContext)

    override val isDeprecatedInJavaDoc: Boolean
        get() = isDeprecatedInJavaDoc(tree, node)

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }
}
