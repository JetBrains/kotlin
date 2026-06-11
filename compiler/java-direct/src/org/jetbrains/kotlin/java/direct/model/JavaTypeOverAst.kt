/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.element.SyntaxElementTypes
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isRaw
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.resolution.FirBackedJavaClassAdapter
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.classifierAdapterFor
import org.jetbrains.kotlin.java.direct.resolution.findClassInCurrentScope
import org.jetbrains.kotlin.java.direct.resolution.findInheritedTypeParameter
import org.jetbrains.kotlin.java.direct.resolution.findTypeParameter
import org.jetbrains.kotlin.java.direct.resolution.getSimpleImport
import org.jetbrains.kotlin.java.direct.resolution.isImportTargetAvailableAsJavaClass
import org.jetbrains.kotlin.java.direct.resolution.isTypeUseAnnotationClass
import org.jetbrains.kotlin.java.direct.resolution.recoverInheritedOuterTypeArguments
import org.jetbrains.kotlin.java.direct.resolution.resolve
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaTypeOverAst(
    val node: JavaLightNode,
    val tree: JavaLightTree,
    protected val resolutionContext: JavaResolutionContext,
    // Annotations from type positions (TYPE node → JAVA_CODE_REFERENCE pass-through).
    // These are TYPE_USE by syntactic position and returned unconditionally.
    private val extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    // Annotations from the containing member's modifier list (method/field/parameter).
    // Pre-filtered TYPE_USE-only via [isTypeUseAnnotationClass] on first
    // read of [annotations] — mirrors PSI/javac-wrapper's structure-build-time pre-filtering
    // (`TreeBasedAnnotationOwner` / `filterTypeAnnotations` in javac-wrapper). The legacy
    // FIR-side `JavaTypeWithExternalAnnotationFiltering` callback bridge is no longer needed.
    private val memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaType, JavaAnnotationOwner {
    // Callback-independent annotations: extra + MODIFIER_LIST children + direct ANNOTATION children.
    private val typePositionAnnotations: Collection<JavaAnnotation>
        get() = extraAnnotations + collectModifierListAndDirectAnnotations(node, tree, resolutionContext)

    /**
     * `memberAnnotations` filtered to only those whose annotation class declares
     * `@Target(ElementType.TYPE_USE)` (Java) or `@Target(AnnotationTarget.TYPE)` (Kotlin).
     * Lazy so the per-annotation symbol-provider lookup fires only when [annotations] is read,
     * which preserves the laziness boundary the FIR-side filter used to live behind.
     */
    private val filteredMemberAnnotations: Collection<JavaAnnotation> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (memberAnnotations.isEmpty()) emptyList()
        else memberAnnotations.filter { annotation ->
            val classId = annotation.classId ?: return@filter false
            with(resolutionContext) { isTypeUseAnnotationClass(classId) }
        }
    }

    override val annotations: Collection<JavaAnnotation>
        get() = filteredMemberAnnotations + typePositionAnnotations

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaClassifierTypeOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, tree, resolutionContext, extraAnnotations, memberAnnotations), JavaClassifierType {

    private val rawTypeNameParts: List<String>
        get() = extractTypeNameParts(node)

    private val rawTypeName: String
        get() {
            val parts = rawTypeNameParts
            return if (parts.size == 1) parts[0] else parts.joinToString(".")
        }

    /**
     * Extracts type name parts from a JAVA_CODE_REFERENCE node, ignoring annotations and type arguments.
     * Handles:
     * - Simple: "Object" → ["Object"]
     * - Qualified: "java.util.List" → ["java", "util", "List"]
     * - Annotated: "@NotNull Object" → ["Object"]
     * - Generic: "List<String>" → ["List"]
     * - Nested generic: "Outer<T>.Inner<U>" → ["Outer", "Inner"]
     */
    private fun extractTypeNameParts(node: JavaLightNode): List<String> {
        val parts = mutableListOf<String>()
        collectIdentifiers(node, parts)
        return parts
    }

    private fun collectIdentifiers(node: JavaLightNode, parts: MutableList<String>) {
        for (child in tree.getChildren(node)) {
            when (tree.getType(child)) {
                JavaSyntaxTokenType.IDENTIFIER -> parts.add(tree.getText(child).toString())
                JavaSyntaxElementType.JAVA_CODE_REFERENCE -> collectIdentifiers(child, parts)
                // Skip: ANNOTATION, REFERENCE_PARAMETER_LIST, WHITE_SPACE, DOT, etc.
            }
        }
    }

    override val classifier: JavaClassifier? by lazy(LazyThreadSafetyMode.PUBLICATION) { computeClassifier() }

    private fun computeClassifier(): JavaClassifier? {
        val parts = rawTypeNameParts

        with(resolutionContext) {
            if (parts.size == 1) {
                // Resolution order for simple names (matches Java scoping rules):
                // 1. OWN type parameters (method/class own — high priority, win over inner class names)
                findTypeParameter(parts[0])?.let { return it }
                // 2. Inner/local class names (shadow INHERITED outer type params)
                val localClass = findClassInCurrentScope(Name.identifier(parts[0]))
                if (localClass != null) return localClass
                // 3. INHERITED type parameters from outer class (low priority — shadowed by inner classes)
                findInheritedTypeParameter(parts[0])?.let { return it }
            }

            // Multi-part names: navigate from base class through inner classes
            var current: JavaClassifier? = findClassInCurrentScope(Name.identifier(parts[0]))

            if (current is JavaClass) {
                for (i in 1 until parts.size) {
                    current = (current as JavaClass).findInnerClass(Name.identifier(parts[i]))
                        ?: return null
                }
                return current
            }

            // Cross-file branch: resolve to a `ClassId` and wrap it in a `FirBackedJavaClassAdapter`.
            // The adapter's outer-class chain exposes [FirBackedJavaTypeParameter] wrappers consumed
            // by the qualified-form raw-detection walk in `computeIsRaw` (counts only). FIR's
            // own `is JavaTypeParameter ->` branch in `JavaTypeConversion` is never reached for
            // these wrappers under the model's resolver invariants; the stack-lookup fallback there
            // would not find them either. `classifierAdapterFor` returns null on sessions with no
            // symbol provider (parsing-level fixtures), so `classifier` stays null there.
            resolve(rawTypeName)?.let { return classifierAdapterFor(it) }
        }
        return null
    }

    override val classifierQualifiedName: String
        get() = computeClassifierQualifiedName()

    private fun computeClassifierQualifiedName(): String {
        val parts = rawTypeNameParts

        // Leverage the already-cached `classifier` to avoid redundant findTypeParameter/findLocalClass lookups.
        val resolvedClassifier = classifier
        if (resolvedClassifier is JavaTypeParameter) {
            return rawTypeName
        }
        if (resolvedClassifier is JavaClass) {
            return resolvedClassifier.fqName?.asString() ?: rawTypeName
        }

        // 3. Check explicit single-type imports
        // Only use import resolution if the target is a known Java class (source or binary).
        // This matches PSI behavior where classifierQualifiedName uses canonicalText, which
        // only returns the FQN when PSI can resolve the class through its indexes.
        // For non-Java classes (e.g., Kotlin builtins), PSI returns just the raw reference text.
        with(resolutionContext) {
            val qualified = getSimpleImport(parts[0])
            if (qualified != null && isImportTargetAvailableAsJavaClass(parts[0])) {
                var result = qualified.asString()
                for (i in 1 until parts.size) {
                    result += "." + parts[i]
                }
                return result
            }
        }

        // 4. Return as-is - FIR will resolve via callback (same package, star imports, java.lang types)
        return rawTypeName
    }

    override val presentableText: String get() = tree.getText(node).toString()

    override val isRaw: Boolean
        get() = computeIsRaw()

    private fun computeIsRaw(): Boolean {
        // Raw when:
        //  (a) own type params declared but fewer args provided — e.g. `List` for `List<E>`.
        //  (b) qualified `Outer.Inner` with no explicit `<>` on any non-static generic outer:
        //      JLS 4.6 raw semantics propagate down; the inner reference must surface as a
        //      `ConeRawType` so FIR's `getProjectionsForRawType` synthesises raw projections
        //      for the outer's type parameters. `Inner<U>` written inside the outer's body
        //      (implicit outer args in scope) is NOT raw — (b) only fires for multi-part
        //      references where the outer name is written explicitly.
        // REFERENCE_PARAMETER_LIST may exist but be empty (no TYPE children); use raw-text
        // part count for the qualified-form check rather than relying on a particular AST shape.
        val javaClass = classifier as? JavaClass ?: return false

        val parameterList = tree.findChildByType(node, JavaSyntaxElementType.REFERENCE_PARAMETER_LIST)
        val ownExplicit = parameterList?.let { pl ->
            tree.getChildren(pl).count { tree.getType(it) == JavaSyntaxElementType.TYPE }
        } ?: 0
        val ownParams = javaClass.typeParameters.size
        if (ownParams > 0 && ownExplicit < ownParams) return true

        if (!javaClass.isStatic && rawTypeNameParts.size > 1) {
            val allRefs = collectAllRefParamLists(node)
            val outerHasExplicitArgs = allRefs.size > 1 && allRefs.dropLast(1).any { pl ->
                tree.getChildren(pl).any { tree.getType(it) == JavaSyntaxElementType.TYPE }
            }
            if (!outerHasExplicitArgs) {
                // Walk the outer chain, one hop per qualifier in the source. NB: don't use
                // `outer.isStatic` to bound the walk. `FirBackedJavaClassAdapter.isStatic` reports
                // `true` for a top-level outer (no `FirOuterClassTypeParameterRef`s on its
                // `nonEnhancedTypeParameters`), which would stop the walk before checking the
                // top-level's own type parameters. For the qualified raw form
                // `Outer.Inner` where `Outer` is a top-level generic class, those own type
                // parameters are precisely what's missing.
                var outer: JavaClass? = javaClass.outerClass
                var levels = rawTypeNameParts.size - 1
                while (outer != null && levels > 0) {
                    if (outer.typeParameters.isNotEmpty()) return true
                    val parent = outer.outerClass
                    if (parent == null) break // Defensive: bound the walk to the top of the chain.
                    outer = parent
                    levels--
                }
            }
        }
        return false
    }

    override val typeArguments: List<JavaType>
        get() = computeTypeArguments()

    private fun computeTypeArguments(): List<JavaType> {
        // Collect all REFERENCE_PARAMETER_LISTs from this node and nested JAVA_CODE_REFERENCEs.
        // This handles both flat ("A<T>.B<U>" → [<T>, <U>] as direct children) and
        // nested ("A<T>.B<U>" → child JAVA_CODE_REF("A<T>") + sibling REFPARAMLIST(<U>)) structures.
        val allRefParamLists = collectAllRefParamLists(node)

        // The innermost class's explicit type arguments come from the LAST REFERENCE_PARAMETER_LIST.
        val explicitArgs = allRefParamLists.lastOrNull()?.let { pl ->
            tree.getChildren(pl)
                .filter { tree.getType(it) == JavaSyntaxElementType.TYPE }
                .map { typeNode -> createJavaType(typeNode, tree, resolutionContext) }
        } ?: emptyList()

        // For qualified generic types like "BaseOuter<H>.BaseInner<Double, String>", the earlier
        // REFERENCE_PARAMETER_LISTs contain explicit type arguments for the outer classes.
        // These are used directly instead of implicit outer type params — for cross-file types
        // (classifier == null) the source-level outer args are the only information available.
        if (allRefParamLists.size > 1) {
            val outerExplicitArgs = allRefParamLists.dropLast(1).reversed().flatMap { paramList ->
                tree.getChildren(paramList).filter { tree.getType(it) == JavaSyntaxElementType.TYPE }
                    .map { createJavaType(it, tree, resolutionContext) }
            }
            if (outerExplicitArgs.isNotEmpty()) {
                return explicitArgs + outerExplicitArgs
            }
        }

        // Simple (non-qualified) type: for non-static inner classes, add implicit outer type params.
        // This handles references like "Inner<U>" inside Outer<T> where the outer T is implicit.
        val javaClass = classifier as? JavaClass
        if (javaClass == null || javaClass.isStatic) {
            return explicitArgs
        }

        val outerTypeParams = mutableListOf<JavaTypeParameter>()
        var outer = javaClass.outerClass
        while (outer != null && !outer.isStatic) {
            outerTypeParams.addAll(outer.typeParameters)
            outer = outer.outerClass
        }

        if (outerTypeParams.isEmpty()) {
            // Inherited case: the inner class is non-static but its outer arguments are neither
            // written in source nor lexically in scope (the outer class is top-level / cross-file,
            // so the lexical walk above stops). Recover them from the containing class's supertype
            // hierarchy — the model-side replacement for the deleted FIR-side recovery. E.g.
            // `J1.NestedSubClass extends NestedInSuperClass` ⇒ `SuperClass<String>.NestedInSuperClass`.
            val classId = javaClass.classId
            if (classId != null) {
                val recovered = with(resolutionContext) { recoverInheritedOuterTypeArguments(classId) }
                if (recovered != null) return explicitArgs + recovered
            }
            return explicitArgs
        }

        // Resolve each outer type param through the current context so we get the caller's H
        // (e.g., Outer.H) rather than the abstract H from the outer class declaration.
        val implicitArgs = outerTypeParams.map { typeParam ->
            val resolved = with(resolutionContext) { findTypeParameter(typeParam.name.asString()) }
            if (resolved != null) JavaTypeParameterTypeOverAst(resolved)
            else JavaTypeParameterTypeOverAst(typeParam)
        }

        return explicitArgs + implicitArgs
    }

    /**
     * Recursively collects all REFERENCE_PARAMETER_LIST nodes in source order,
     * traversing into child JAVA_CODE_REFERENCE nodes (for nested qualified types).
     * For "A<T>.B<U>" → [paramList(<T>), paramList(<U>)] regardless of AST structure.
     */
    private fun collectAllRefParamLists(n: JavaLightNode): List<JavaLightNode> {
        val result = mutableListOf<JavaLightNode>()
        for (child in tree.getChildren(n)) {
            when (tree.getType(child)) {
                JavaSyntaxElementType.JAVA_CODE_REFERENCE -> result.addAll(collectAllRefParamLists(child))
                JavaSyntaxElementType.REFERENCE_PARAMETER_LIST -> result.add(child)
            }
        }
        return result
    }

}

/** [JavaClassifierType] for enum entry fields: the constant's type is the containing enum class. */
class JavaClassifierTypeForEnumEntry(
    private val enumClass: JavaClass,
) : JavaClassifierType {
    override val classifier: JavaClassifier get() = enumClass
    override val classifierQualifiedName: String get() = enumClass.fqName?.asString() ?: enumClass.name.asString()
    override val presentableText: String get() = classifierQualifiedName
    override val isRaw: Boolean get() = false
    override val typeArguments: List<JavaType> get() = emptyList()
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

/**
 * [JavaClassifierType] backed by an already-resolved [JavaClass]. Used by
 * [JavaClassOverAst.deriveImplicitPermittedTypes] so the resolved nested
 * inner class is surfaced directly without going through AST-based
 * classifier resolution; this keeps the FIR-side
 * `setSealedClassInheritors` consumer on the non-null `classifier` branch.
 */
class ResolvedJavaClassifierType(
    private val resolvedClass: JavaClass,
) : JavaClassifierType {
    override val classifier: JavaClassifier get() = resolvedClass
    override val classifierQualifiedName: String get() = resolvedClass.fqName?.asString() ?: resolvedClass.name.asString()
    override val presentableText: String get() = classifierQualifiedName
    override val isRaw: Boolean get() = false
    override val typeArguments: List<JavaType> get() = emptyList()
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

class JavaPrimitiveTypeOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, tree, resolutionContext, extraAnnotations, memberAnnotations), JavaPrimitiveType {
    override val type: org.jetbrains.kotlin.builtins.PrimitiveType?
        get() {
            val text = tree.getText(node)
            return when {
                text.contentEquals("void") -> null
                text.contentEquals("boolean") -> org.jetbrains.kotlin.builtins.PrimitiveType.BOOLEAN
                text.contentEquals("char") -> org.jetbrains.kotlin.builtins.PrimitiveType.CHAR
                text.contentEquals("byte") -> org.jetbrains.kotlin.builtins.PrimitiveType.BYTE
                text.contentEquals("short") -> org.jetbrains.kotlin.builtins.PrimitiveType.SHORT
                text.contentEquals("int") -> org.jetbrains.kotlin.builtins.PrimitiveType.INT
                text.contentEquals("float") -> org.jetbrains.kotlin.builtins.PrimitiveType.FLOAT
                text.contentEquals("long") -> org.jetbrains.kotlin.builtins.PrimitiveType.LONG
                text.contentEquals("double") -> org.jetbrains.kotlin.builtins.PrimitiveType.DOUBLE
                else -> null
            }
        }
}

class JavaArrayTypeOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    override val componentType: JavaType,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, tree, resolutionContext, extraAnnotations, memberAnnotations), JavaArrayType

class JavaWildcardTypeOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    override val bound: JavaType?,
    override val isExtends: Boolean,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, tree, resolutionContext, extraAnnotations, memberAnnotations), JavaWildcardType

/**
 * A JavaClassifierType that represents a type parameter reference.
 * Used for implicit type arguments from outer classes of inner class types.
 * This matches TreeBasedTypeParameterType in javac-wrapper.
 */
class JavaTypeParameterTypeOverAst(
    override val classifier: JavaTypeParameter,
) : JavaClassifierType {
    override val typeArguments: List<JavaType> get() = emptyList()
    override val isRaw: Boolean get() = false
    override val classifierQualifiedName: String get() = classifier.name.asString()
    override val presentableText: String get() = classifierQualifiedName
    override val annotations: Collection<JavaAnnotation> get() = classifier.annotations
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

fun createJavaType(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
): JavaType {
    // If input node is a TYPE with array brackets, vararg ellipsis, or '?' wildcard, handle it
    // directly. Don't look for a nested TYPE first — that would skip the outer array dimension
    // or mistake a wildcard-bound TYPE child for the wildcard itself.
    if (tree.getType(node) == JavaSyntaxElementType.TYPE) {
        val arrayOrVararg = tryCreateArrayOrVarargFromTypeNode(node, tree, resolutionContext, memberAnnotations)
        if (arrayOrVararg != null) return arrayOrVararg

        if (tree.findChildByType(node, JavaSyntaxTokenType.QUEST) != null) {
            return createWildcardType(node, tree, resolutionContext, memberAnnotations)
        }
    }

    val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE) ?: node

    if (tree.findChildByType(typeNode, JavaSyntaxTokenType.QUEST) != null) {
        return createWildcardType(typeNode, tree, resolutionContext, memberAnnotations)
    }

    val arrayOrVararg = tryCreateArrayOrVarargFromTypeNode(typeNode, tree, resolutionContext, memberAnnotations)
    if (arrayOrVararg != null) return arrayOrVararg

    return createClassifierOrPrimitive(typeNode, tree, resolutionContext, memberAnnotations)
}

/**
 * If [typeNode] encodes an array (one or more `[]`) or vararg (`...`) wrapping another TYPE,
 * returns the wrapped [JavaArrayTypeOverAst] chain. Returns `null` when [typeNode] is neither.
 *
 * The KMP parser places all `[]` pairs as siblings under the same TYPE node
 * (e.g. `List<Double>[][]` → `TYPE[TYPE[List<Double>], [], []]`), so we wrap the inner type in
 * N array dimensions, innermost first.
 *
 * For varargs (`@NonNull String... args`), member annotations (from the parameter's
 * MODIFIER_LIST) apply to the component type, not the array wrapper — matching
 * PSI/javac-wrapper behaviour where TYPE_USE annotations like `@NonNull` enhance the component
 * type's nullability, not the array's.
 *
 * Non-vararg arrays never receive [memberAnnotations] on the outer wrapper or component:
 * a method/parameter MODIFIER_LIST annotation is delivered to FIR via the member's own
 * `annotations` (containerAnnotations in [AbstractSignatureParts]), and FIR's array-head
 * TYPE_USE filter (KT-24392) intentionally drops TYPE_USE container annotations on the array
 * head to avoid double-application — `@NotNull Foo[] f()` should give `Array<Foo!>!` (flexible
 * array, non-null component), not `Array<Foo!>` (non-null array). PSI achieves this by leaving
 * `PsiArrayType.getAnnotations()` empty for method-level annotations; we match that by never
 * attaching [memberAnnotations] to the outer [JavaArrayTypeOverAst] for non-vararg arrays.
 */
private fun tryCreateArrayOrVarargFromTypeNode(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaType? {
    val arrayDimensions = tree.getChildren(typeNode).count { tree.getType(it) == JavaSyntaxTokenType.LBRACKET }
    val hasVarargEllipsis = tree.findChildByType(typeNode, JavaSyntaxTokenType.ELLIPSIS) != null
    if (arrayDimensions == 0 && !hasVarargEllipsis) return null
    val componentTypeNode = tree.findChildByType(typeNode, JavaSyntaxElementType.TYPE) ?: return null

    val dims = if (hasVarargEllipsis) 1 else arrayDimensions
    val componentMemberAnnotations = if (hasVarargEllipsis) memberAnnotations else emptyList()
    var result: JavaType = createJavaType(componentTypeNode, tree, resolutionContext, memberAnnotations = componentMemberAnnotations)
    repeat(dims) {
        // memberAnnotations always empty for non-vararg arrays (see this function's KDoc and
        // FIR's array-head TYPE_USE filter). extraAnnotations omitted: no caller ever supplies
        // TYPE-position annotations on the outer TYPE node for an array — those live inside
        // the wrapped TYPE child and are picked up by the recursive createJavaType call above.
        result = JavaArrayTypeOverAst(typeNode, tree, resolutionContext, result)
    }
    return result
}

/**
 * Builds a [JavaWildcardTypeOverAst] from [typeNode], which must contain a `?` child (QUEST).
 * AST structure: `TYPE -> [QUEST, (EXTENDS_KEYWORD|SUPER_KEYWORD)?, TYPE?]`.
 */
private fun createWildcardType(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaWildcardTypeOverAst {
    val hasSuper = tree.findChildByType(typeNode, JavaSyntaxTokenType.SUPER_KEYWORD) != null
    val boundTypeNode = tree.findChildByType(typeNode, JavaSyntaxElementType.TYPE)
    val bound = boundTypeNode?.let { createJavaType(it, tree, resolutionContext) }
    val isExtends = !hasSuper
    return JavaWildcardTypeOverAst(typeNode, tree, resolutionContext, bound, isExtends, memberAnnotations = memberAnnotations)
}

/**
 * Falls through to a primitive ([JavaPrimitiveTypeOverAst]) or classifier
 * ([JavaClassifierTypeOverAst]) type depending on which child [typeNode] has.
 */
private fun createClassifierOrPrimitive(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaType {
    val primitiveNode = tree.getChildren(typeNode).find {
        val t = tree.getType(it)
        t in SyntaxElementTypes.PRIMITIVE_TYPE_BIT_SET || t == JavaSyntaxTokenType.VOID_KEYWORD
    }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, tree, resolutionContext, memberAnnotations = memberAnnotations)
    }

    val referenceNode = tree.findChildByType(typeNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
    if (referenceNode != null) {
        // TYPE_USE annotations on type arguments appear directly under the TYPE node (not in MODIFIER_LIST).
        // Pass them as extraAnnotations since we're using JAVA_CODE_REFERENCE as the node.
        val typeNodeAnnotations = tree.getChildrenByType(typeNode, JavaSyntaxElementType.ANNOTATION)
            .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        return JavaClassifierTypeOverAst(referenceNode, tree, resolutionContext, typeNodeAnnotations, memberAnnotations)
    }
    return JavaClassifierTypeOverAst(typeNode, tree, resolutionContext, memberAnnotations = memberAnnotations)
}

/**
 * Creates a JavaType with annotations from a member's modifier list.
 * Member annotations are passed separately from type-position annotations so that
 * filterTypeUseAnnotations can apply callback-based filtering only to member annotations
 * while returning type-position annotations unconditionally.
 */
fun createJavaTypeWithAnnotations(
    typeNode: JavaLightNode,
    modifierList: JavaLightNode?,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): JavaType {
    val memberAnnotations = modifierList?.let { ml ->
        tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
            .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
    } ?: emptyList()
    return createJavaType(typeNode, tree, resolutionContext, memberAnnotations = memberAnnotations)
}

/**
 * Collects annotations attached syntactically to [node], in source order:
 *  1. annotations nested inside the node's MODIFIER_LIST (if any), then
 *  2. annotations that are direct children of the node.
 *
 * Shared by [JavaTypeOverAst.typePositionAnnotations] and [JavaTypeParameterOverAst.annotations],
 * which both need exactly this walk (the KMP parser may place annotations either inside a
 * MODIFIER_LIST or directly under the enclosing node, depending on the construct).
 */
private fun collectModifierListAndDirectAnnotations(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): List<JavaAnnotation> {
    val modifierListAnnotations =
        tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
            tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        } ?: emptyList()
    val directAnnotations = tree.getChildrenByType(node, JavaSyntaxElementType.ANNOTATION)
        .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
    return modifierListAnnotations + directAnnotations
}

/**
 * AST-backed [JavaTypeParameter].
 *
 * Two-phase construction invariant
 * --------------------------------
 * A type parameter's upper bounds may forward-reference its sibling type parameters
 * (e.g. `<S extends JsStubElement<E>, E>`). At construction time, however, only the
 * containing-class context is known — the sibling list is not yet available. We therefore
 * build a [JavaTypeParameterOverAst] in two phases:
 *
 *  1. Create each parameter with the bare [initialResolutionContext] (containing class only).
 *  2. Once **all** sibling parameters in the same declaration list have been created,
 *     [updateResolutionContext] is called exactly once with a context enriched by the full
 *     sibling list (see `computeTypeParameters` in `utils.kt`).
 *
 * The [upperBounds] property is `lazy(PUBLICATION)`, so it must not be touched between
 * phases 1 and 2 — otherwise it would cache a context that cannot resolve sibling references.
 * This contract is enforced by convention: only `computeTypeParameters` constructs
 * [JavaTypeParameterOverAst], and it always invokes [updateResolutionContext] before exposing
 * the parameters to any caller.
 */
class JavaTypeParameterOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    initialResolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node, tree), JavaTypeParameter {

    private var resolutionContext: JavaResolutionContext = initialResolutionContext

    /**
     * Phase 2 of the two-phase construction (see class KDoc).
     * Replaces the resolution context with one that knows about all sibling type parameters,
     * so that forward references in upper bounds can be resolved.
     *
     * Must be called exactly once, before [upperBounds] is first accessed.
     * Marked `internal` because only the `utils.kt` `computeTypeParameters` factory is allowed
     * to invoke it; external callers must not break the construction invariant.
     */
    internal fun updateResolutionContext(newContext: JavaResolutionContext) {
        resolutionContext = newContext
    }

    override val name: Name
        get() = Name.identifier(
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() } ?: "<error>"
        )

    override val upperBounds: Collection<JavaClassifierType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val extendsList = tree.findChildByType(node, JavaSyntaxElementType.EXTENDS_BOUND_LIST) ?: return@lazy emptyList()
        tree.getChildren(extendsList)
            .filter { tree.getType(it) != JavaSyntaxTokenType.AND }
            .mapNotNull { child ->
                when (tree.getType(child)) {
                    JavaSyntaxElementType.TYPE -> createJavaType(child, tree, resolutionContext) as? JavaClassifierType
                    JavaSyntaxElementType.JAVA_CODE_REFERENCE -> JavaClassifierTypeOverAst(child, tree, resolutionContext)
                    else -> null
                }
            }
    }

    // Annotations on the type parameter declaration itself (e.g., <@NonNull T>).
    // Matches TreeBasedTypeParameter which reads tree.annotations().
    // See [collectModifierListAndDirectAnnotations] for the parser-shape handling.
    override val annotations: Collection<JavaAnnotation>
        get() = collectModifierListAndDirectAnnotations(node, tree, resolutionContext)

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isFromSource: Boolean get() = true
}

/** Implicit supertype `java.lang.Enum<E>` for enum classes. */
class EnumSupertypeForJavaDirect(
    private val enumClass: JavaClass,
    private val resolutionContext: JavaResolutionContext,
) : JavaClassifierType {
    override val classifier: JavaClassifier? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        with(resolutionContext) { resolve(classifierQualifiedName)?.let { classifierAdapterFor(it) } }
    }
    override val classifierQualifiedName: String get() = "java.lang.Enum"
    override val typeArguments: List<JavaType> get() = listOf(EnumSelfTypeArgument())
    override val isRaw: Boolean get() = false
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val presentableText: String get() = "java.lang.Enum<${enumClass.fqName}>"
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    private inner class EnumSelfTypeArgument : JavaClassifierType {
        override val classifier: JavaClassifier get() = enumClass
        override val classifierQualifiedName: String get() = enumClass.fqName?.asString() ?: ""
        override val typeArguments: List<JavaType> get() = emptyList()
        override val isRaw: Boolean get() = false
        override val annotations: Collection<JavaAnnotation> get() = emptyList()
        override val presentableText: String get() = classifierQualifiedName
        override val isDeprecatedInJavaDoc: Boolean get() = false
        override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    }
}

/**
 * [JavaClassifierType] for well-known external classes (e.g. `java.lang.Object`).
 * Lazily resolves [classifier] through the [JavaResolutionContext]'s session so the
 * FIR-side `null ->` branch in `JavaTypeConversion` doesn't have to handle this case.
 */
class SimpleClassifierType(
    override val classifierQualifiedName: String,
    private val resolutionContext: JavaResolutionContext,
) : JavaClassifierType {
    override val classifier: JavaClassifier? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        with(resolutionContext) { resolve(classifierQualifiedName)?.let { classifierAdapterFor(it) } }
    }
    override val typeArguments: List<JavaType> get() = emptyList()
    override val isRaw: Boolean get() = false
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val presentableText: String get() = classifierQualifiedName
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

/**
 * [JavaClassifierType] backed by a resolved FIR [ConeClassLikeType]. Used to expose
 * [FirBackedJavaClassAdapter.supertypes] (and, recursively, their cone type arguments) back
 * through the public Java-model interface so FIR's `JavaTypeConversion` can re-convert them.
 *
 * The model-side inherited-outer-argument recovery in [JavaClassifierTypeOverAst.computeTypeArguments]
 * reads [coneType] directly (it is `internal`) to walk the supertype hierarchy and substitute type
 * arguments at the cone level.
 */
internal class FirBackedJavaClassifierType(
    val coneType: ConeClassLikeType,
    private val session: FirSession,
) : JavaClassifierType {
    override val classifier: JavaClassifier = FirBackedJavaClassAdapter(coneType.lookupTag.classId, session)
    override val classifierQualifiedName: String get() = coneType.lookupTag.classId.asSingleFqName().asString()
    override val presentableText: String get() = classifierQualifiedName
    override val isRaw: Boolean get() = coneType.isRaw()

    override val typeArguments: List<JavaType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        coneType.typeArguments.map { firBackedJavaType(it, session) }
    }

    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override fun toString(): String = "FirBackedJavaClassifierType($coneType)"
}

/**
 * [JavaWildcardType] backed by a cone projection's bound. Reproduces FIR's own
 * `JavaWildcardType -> ConeKotlinTypeProjectionIn/Out/Star` mapping when the cone arguments of a
 * [FirBackedJavaClassifierType] are re-converted by `JavaTypeConversion`.
 */
internal class FirBackedJavaWildcardType(
    override val bound: JavaType?,
    override val isExtends: Boolean,
) : JavaWildcardType {
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
}

/**
 * Wraps a cone [ConeTypeProjection] as a [JavaType] so FIR's `JavaTypeConversion` reproduces the
 * original projection when re-converting a [FirBackedJavaClassifierType]'s type arguments:
 *  - star projection → unbounded wildcard (`? `) → `ConeStarProjection`
 *  - `in`/`out` projection → bounded wildcard → `ConeKotlinTypeProjection{In, Out}`
 *  - invariant class type  → [FirBackedJavaClassifierType]
 *
 * Type-parameter (and other non-class-like) invariant projections fall back to an unbounded
 * wildcard: the recovery substitutes them to concrete arguments at the cone level before any
 * wrapper is produced, so this fallback is only reached for unsubstituted residual projections.
 */
internal fun firBackedJavaType(projection: ConeTypeProjection, session: FirSession): JavaType {
    return when (projection) {
        is ConeStarProjection -> FirBackedJavaWildcardType(bound = null, isExtends = true)
        is ConeKotlinTypeProjectionIn ->
            FirBackedJavaWildcardType(bound = firBackedClassifierOrNull(projection.type, session), isExtends = false)
        is ConeKotlinTypeProjectionOut ->
            FirBackedJavaWildcardType(bound = firBackedClassifierOrNull(projection.type, session), isExtends = true)
        is ConeClassLikeType -> FirBackedJavaClassifierType(projection, session)
        // Type-parameter / flexible / conflicting / error projections: fall back to an unbounded
        // wildcard. The recovery substitutes type parameters to concrete arguments at the cone
        // level before wrapping, so this fallback is only reached for unsubstituted residuals.
        else -> FirBackedJavaWildcardType(bound = null, isExtends = true)
    }
}

private fun firBackedClassifierOrNull(type: ConeKotlinType, session: FirSession): JavaType? =
    (type as? ConeClassLikeType)?.let { FirBackedJavaClassifierType(it, session) }
