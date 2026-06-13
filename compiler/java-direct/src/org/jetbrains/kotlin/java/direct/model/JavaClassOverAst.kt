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
        val innerClassNode = tree.getChildren(node).find { child ->
            tree.getType(child) == JavaSyntaxElementType.CLASS &&
                    tree.findChildByType(child, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.textEquals(it, nameString) } == true
        }

        if (innerClassNode != null) {
            // Check if the inner class is effectively static:
            // - Explicitly marked with 'static' keyword
            // - Is an interface (interfaces are implicitly static in Java; JLS 8.5.1)
            // - Is an enum (enums are implicitly static in Java; JLS 8.5.1)
            // - Is a record (records are implicitly static; JLS 8.10.3)
            val hasStaticKeyword = tree.findChildByType(innerClassNode, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
                tree.hasChildOfType(ml, JavaSyntaxTokenType.STATIC_KEYWORD)
            } ?: false
            val innerIsInterface = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.INTERFACE_KEYWORD) != null
            val innerIsEnum = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.ENUM_KEYWORD) != null
            val innerIsRecord = tree.findChildByType(innerClassNode, JavaSyntaxTokenType.RECORD_KEYWORD) != null
            val innerIsEffectivelyStatic = hasStaticKeyword || innerIsInterface || innerIsEnum || innerIsRecord

            // Non-static inner classes get outer type params as OWN (high priority, can't be shadowed
            // by inner class names) via memberResolutionContext.
            // Static nested types get them as INHERITED (low priority, shadowable by inner class names).
            val contextForInner = if (innerIsEffectivelyStatic)
                resolutionContext.withContainingClass(this).withInheritedTypeParameters(typeParameters)
            else
                memberResolutionContext
            return JavaClassOverAst(innerClassNode, tree, contextForInner, outerClass = this)
        }

        // Inner class is not directly declared. Like the PSI (`JavaClassImpl.findInnerClassByName(name, false)`)
        // and binary (`BinaryJavaClass.ownInnerClassNameToAccess`) implementations, `findInnerClass` returns
        // only directly declared member types. Inherited member types (JLS 8.5) are resolved by the resolution
        // layer (see [org.jetbrains.kotlin.java.direct.resolution.findInnerClassInSameFileSupertypes] and
        // [org.jetbrains.kotlin.java.direct.resolution.JavaInheritedMemberResolver]).
        return null
    }

    /**
     * Direct supertype reference names exactly as written in the source — the raw `EXTENDS_LIST` and
     * `IMPLEMENTS_LIST` `JAVA_CODE_REFERENCE` text, with any generic arguments stripped.
     *
     * This is purely **syntactic**: it does NOT resolve the references, so it is safe to read during
     * resolution without re-entering type construction (touching `supertypes` would call
     * `classifier → findLocalClass → findInnerClass` and recurse). The resolution layer uses it for the
     * recursion-safe same-file supertype walk that resolves inherited member types.
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
            // No explicit permits clause — sealed class: infer permitted types from the subtypes
            // declared anywhere in the same compilation unit (JLS 8.1.6 / 9.1.4). See
            // deriveImplicitPermittedTypes for the rationale.
            if (!isSealed) return emptySequence()
            return deriveImplicitPermittedTypes()
        }

    /**
     * Implicit-`permits` fallback: when a sealed type has no `permits` clause, Java infers its
     * permitted direct subtypes as *each top-level or nested class declared in the same compilation
     * unit (the whole `.java` file) whose direct superclass is this type* (JLS 8.1.6 / 9.1.4). We
     * therefore scan **every** CLASS node in the file — top-level siblings and member types at any
     * nesting depth — not just this type's own directly-nested members. This mirrors the PSI
     * reference (`JavaClassImpl.lazilyComputePermittedTypesInSameFile`), which walks the entire
     * `containingFile` with `SyntaxTraverser.psiTraverser`.
     *
     * The match is **resolution-based**, exactly like PSI's `isInheritor(this, checkDeep = false)`:
     * a candidate is permitted iff one of its *directly declared* `extends`/`implements` supertypes
     * **resolves** to this sealed type (compared by [fqName]). Resolving — rather than matching the
     * raw reference text — is what makes this honour imports, packages and scoping, so it has
     * neither of the false-positive/false-negative gaps of a textual match:
     * - a candidate extending a *different* type that merely shares this type's simple name (e.g.
     *   imported from another package) resolves to that other classifier and is excluded;
     * - a candidate naming this type through a differently-spelled qualified path (fully-qualified
     *   or via a single-type import) resolves to this type and is included.
     *
     * Resolution here re-enters type construction (`classifier -> findLocalClass -> findInnerClass`)
     * and can recurse while `permittedTypes` is itself being computed during this type's resolution.
     * To stay recursion-safe we mirror PSI's second design choice and defer it: only the candidate
     * **enumeration** (walking the file for CLASS nodes) is eager; both the node→[JavaClass]
     * resolution and the per-candidate supertype resolution happen **lazily** inside the returned
     * `Sequence`, so they run only when the caller iterates it — by which point this type's own
     * resolution is no longer on the stack (FIR consumes `permittedTypes` from a deferred
     * `setSealedClassInheritors { ... }` provider). PSI defers the same `isInheritor` filter behind
     * its `Sequence` "because that resolution can cause contract violations".
     *
     * Only *direct* declared supertypes are compared (no transitive walk), matching JLS's "direct
     * superclass" wording and PSI's `checkDeep = false`.
     */
    private fun deriveImplicitPermittedTypes(): Sequence<JavaClassifierType> {
        val myFqName = fqName
        // Eagerly enumerate every CLASS node in the compilation unit (top-level siblings and member
        // types at any depth). Enumeration is purely structural (by node type) and recursion-safe.
        val candidateNodes = mutableListOf<JavaLightNode>()
        collectClassNodes(tree.getRoot(), candidateNodes)
        // Lazily resolve each candidate and keep those whose direct superclass resolves to this type.
        // Resolution is deferred behind the Sequence so it never runs while this type's own
        // permittedTypes is being computed during resolution.
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

    /**
     * Recursively collects every CLASS node under [container] (top-level siblings and member types
     * at any depth) into [out]. Enumeration is purely structural — no references are resolved here.
     */
    private fun collectClassNodes(container: JavaLightNode, out: MutableList<JavaLightNode>) {
        for (child in tree.getChildren(container)) {
            if (tree.getType(child) != JavaSyntaxElementType.CLASS) continue
            out.add(child)
            collectClassNodes(child, out)
        }
    }

    /**
     * Resolves an arbitrary same-file CLASS node to its [JavaClass] without triggering supertype
     * resolution: the top-level enclosing class is materialised through the file's same-file
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
        get() = tree.getChildrenByType(node, JavaSyntaxElementType.METHOD)
            .filter {
                tree.findChildByType(it, JavaSyntaxElementType.TYPE) == null &&
                        tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER) != null
            }
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
