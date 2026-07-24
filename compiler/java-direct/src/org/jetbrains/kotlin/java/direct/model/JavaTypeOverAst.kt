/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.element.SyntaxElementTypes
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.resolution.*
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaTypeOverAst(
    val node: JavaLightNode,
    val tree: JavaLightTree,
    protected val resolutionContext: JavaResolutionContext,
    // Annotations written in the type position itself (e.g. `@NotNull` in `List<@NotNull Integer>`,
    // where ANNOTATION is a direct child of the TYPE node) — TYPE_USE by syntactic position,
    // returned unconditionally.
    private val extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    // Annotations from the containing member's modifier list (method/field/parameter).
    // Kept TYPE_USE-only: filtered via [isTypeUseAnnotationClass] lazily on first read of
    // [annotations] — see [filteredMemberAnnotations].
    private val memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaType, JavaAnnotationOwner {
    // Callback-independent annotations: extra + MODIFIER_LIST children + direct ANNOTATION children.
    private val typePositionAnnotations: Collection<JavaAnnotation>
        get() = extraAnnotations + collectModifierListAndDirectAnnotations(node, tree, resolutionContext)

    /**
     * `memberAnnotations` filtered to only those whose annotation class declares
     * `@Target(ElementType.TYPE_USE)` (Java) or `@Target(AnnotationTarget.TYPE)` (Kotlin).
     * Lazy so the per-annotation symbol-provider lookup fires only when [annotations] is read.
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
                // 1. OWN type parameters (high priority). Known javac divergence: javac prefers a
                // same-named nested class over the own type parameter in one narrow JLS-scoping
                // edge case; this order mirrors PSI, and PSI parity is the target. This priority is
                // pinned by the compiler-wide test
                // `diagnostics/tests/javac/typeParameters/OwnNestedClassAndTypeParameterWithSameNames.kt`
                // (and its inherited-nested-class sibling `InheritedInnerAndTypeParameterWithSameNames.kt`),
                // both of which also run in this module's phased suite and would fail if this step
                // were reordered after the nested-class lookup below.
                findTypeParameter(parts[0])?.let { return it }
                // 2. Inner/local class names (shadow INHERITED outer type params)
                findClassInCurrentScope(parts[0])?.let { return it }
                // 3. INHERITED type parameters from outer class (low priority — shadowed by inner classes).
                // TODO: (KT-87797) PSI-parity-only; to remove (see `JavaScopeContext.inheritedTypeParametersInScope`)
                findInheritedTypeParameter(parts[0])?.let { return it }
            }

            // In-scope (AST/model) navigation, kept as a distinct pass *before* the [resolve]
            // fallback below:
            //  - it needs no `FirSession` symbol provider, unlike [resolve]'s class-existence probe
            //    (so it also serves parser-only tests);
            //  - even with a session it avoids a symbol-provider round-trip per segment.
            var current: JavaClassifier? = findClassInCurrentScope(parts[0])

            if (current is JavaClass) {
                for (i in 1 until parts.size) {
                    val part = Name.identifier(parts[i])
                    current = declaredOrFullyInherited(current as JavaClass, part)
                        ?: return null
                }
                return current
            }

            // Cross-file branch: resolve the whole reference to a `ClassId` and materialize it via
            // [classifierAdapterFor] — the canonical, identity-preserving `JavaClassOverAst` for a
            // source-backed `ClassId`, a `FirBackedJavaClassAdapter` otherwise; `null` on sessions
            // without a symbol provider.
            resolve(rawTypeName)?.let { return classifierAdapterFor(it) }
        }
        return null
    }

    override val classifierQualifiedName: String
        get() = computeClassifierQualifiedName()

    private fun computeClassifierQualifiedName(): String =
        when (val resolvedClassifier = classifier) {
            is JavaClass -> {
                resolvedClassifier.fqName?.asString() ?: rawTypeName
            }
            else -> rawTypeName
        }

    override val presentableText: String get() = tree.getText(node).toString()

    override val isRaw: Boolean
        get() = computeIsRaw()

    private fun computeIsRaw(): Boolean {
        // Raw when (JLS 4.6):
        //  (a) own type params declared but fewer args provided — e.g. `List` for `List<E>`;
        //  (b) qualified `Outer.Inner` with no explicit `<>` on any non-static generic outer —
        //      raw semantics propagate down, so FIR must see a `ConeRawType`. `Inner<U>` written
        //      inside the outer's body (implicit outer args in scope) is NOT raw.
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
                // Walk the outer chain, one hop per qualifier in the source. NB: don't bound the
                // walk with `outer.isStatic` — `FirBackedJavaClassAdapter.isStatic` reports `true`
                // for a top-level outer, which would skip exactly the top-level generic outer
                // whose type parameters make the qualified form raw.
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

/**
 * [JavaClassifierType] backed by an already-resolved [JavaClass], surfaced directly without
 * going through AST-based classifier resolution. Used for:
 *  - enum entry fields, where the constant's type is its containing enum class
 *    ([JavaMemberOverAst.computeType]);
 *  - implicit permitted types ([JavaClassOverAst.deriveImplicitPermittedTypes]), where it keeps
 *    the FIR-side `setSealedClassInheritors` consumer on the non-null `classifier` branch.
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
    // [node] is always a primitive keyword (SyntaxElementTypes.PRIMITIVE_TYPE_BIT_SET) or
    // VOID_KEYWORD — see [createClassifierOrPrimitive] — so the token type maps directly to a
    // PrimitiveType. `void` (and any unexpected token) is absent from the map and yields null.
    override val type: PrimitiveType?
        get() = PRIMITIVE_TYPE_BY_TOKEN[tree.getType(node)]

    private companion object {
        private val PRIMITIVE_TYPE_BY_TOKEN: Map<SyntaxElementType, PrimitiveType> = mapOf(
            JavaSyntaxTokenType.BOOLEAN_KEYWORD to PrimitiveType.BOOLEAN,
            JavaSyntaxTokenType.CHAR_KEYWORD to PrimitiveType.CHAR,
            JavaSyntaxTokenType.BYTE_KEYWORD to PrimitiveType.BYTE,
            JavaSyntaxTokenType.SHORT_KEYWORD to PrimitiveType.SHORT,
            JavaSyntaxTokenType.INT_KEYWORD to PrimitiveType.INT,
            JavaSyntaxTokenType.FLOAT_KEYWORD to PrimitiveType.FLOAT,
            JavaSyntaxTokenType.LONG_KEYWORD to PrimitiveType.LONG,
            JavaSyntaxTokenType.DOUBLE_KEYWORD to PrimitiveType.DOUBLE,
        )
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
 * returns the wrapped [JavaArrayTypeOverAst] chain; `null` when it is neither. The KMP parser
 * places all `[]` pairs as siblings under the same TYPE node, so the inner type is wrapped in N
 * dimensions, innermost first.
 *
 * [memberAnnotations] placement matches PSI:
 * - varargs: on the component type (TYPE_USE annotations enhance the component's nullability);
 * - non-vararg arrays: nowhere — the member's own `annotations` already deliver them to FIR as
 *   container annotations, and FIR's array-head TYPE_USE filter (KT-24392) drops them from the
 *   array head; attaching them here as *type* annotations would double-apply them
 *   (`@NotNull Foo[] f()` must give `Array<Foo!>!`, not `Array<Foo!>`).
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
        // No annotations on the array wrapper — see this function's KDoc.
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
 * Member annotations are passed separately from type-position annotations so that the TYPE_USE
 * filtering (see [JavaTypeOverAst]) is applied only to member annotations, while type-position
 * annotations are returned unconditionally.
 */
fun createJavaTypeWithAnnotations(
    typeNode: JavaLightNode,
    modifierList: JavaLightNode?,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): JavaType {
    val memberAnnotations = parseAnnotationsFromModifierList(modifierList, tree, resolutionContext)
    return createJavaType(typeNode, tree, resolutionContext, memberAnnotations = memberAnnotations)
}

/**
 * Maps the ANNOTATION children of a MODIFIER_LIST node to [JavaAnnotationOverAst], or returns an
 * empty list when [modifierList] is `null`.
 */
internal fun parseAnnotationsFromModifierList(
    modifierList: JavaLightNode?,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): List<JavaAnnotation> =
    modifierList?.let { ml ->
        tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
            .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
    } ?: emptyList()

/**
 * Collects annotations attached syntactically to [node]: those nested inside its MODIFIER_LIST,
 * then direct ANNOTATION children (the KMP parser places them in either position, depending on
 * the construct).
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
 * AST-backed [JavaTypeParameter], built in two phases because upper bounds may forward-reference
 * sibling type parameters (`<S extends JsStubElement<E>, E>`):
 *  1. each parameter is created with the bare [initialResolutionContext];
 *  2. once all siblings exist, [updateResolutionContext] is called with a context enriched by
 *     the full sibling list (see `computeTypeParameters` in `utils.kt`).
 * [upperBounds] is lazy and must not be read between the phases; by convention only
 * `computeTypeParameters` constructs instances and always completes phase 2 first.
 */
class JavaTypeParameterOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    initialResolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node, tree), JavaTypeParameter {

    private var resolutionContext: JavaResolutionContext = initialResolutionContext

    /** Phase 2 of the two-phase construction (see class KDoc). */
    internal fun updateResolutionContext(newContext: JavaResolutionContext) {
        resolutionContext = newContext
    }

    override val name: Name
        get() = Name.identifier(identifierText() ?: "<error>")

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
    // See [collectModifierListAndDirectAnnotations] for the parser-shape handling.
    override val annotations: Collection<JavaAnnotation>
        get() = collectModifierListAndDirectAnnotations(node, tree, resolutionContext)

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }
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
 * [JavaClassifierType] backed by a resolved FIR [ConeClassLikeType]. Exposes
 * [FirBackedJavaClassAdapter.supertypes] (and, recursively, their cone type arguments) back
 * through the public Java-model interface so FIR's `JavaTypeConversion` can re-convert them.
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
 * original projection when re-converting a [FirBackedJavaClassifierType]'s type arguments.
 * Non-class-like invariant projections (type-parameter / flexible / error) fall back to an
 * unbounded wildcard — the recovery substitutes type parameters to concrete arguments at the
 * cone level before wrapping, so the fallback is only reached for unsubstituted residuals.
 */
internal fun firBackedJavaType(projection: ConeTypeProjection, session: FirSession): JavaType {
    return when (projection) {
        is ConeStarProjection -> FirBackedJavaWildcardType(bound = null, isExtends = true)
        is ConeKotlinTypeProjectionIn ->
            FirBackedJavaWildcardType(bound = firBackedClassifierOrNull(projection.type, session), isExtends = false)
        is ConeKotlinTypeProjectionOut ->
            FirBackedJavaWildcardType(bound = firBackedClassifierOrNull(projection.type, session), isExtends = true)
        is ConeClassLikeType -> FirBackedJavaClassifierType(projection, session)
        else -> FirBackedJavaWildcardType(bound = null, isExtends = true)
    }
}

private fun firBackedClassifierOrNull(type: ConeKotlinType, session: FirSession): JavaType? =
    (type as? ConeClassLikeType)?.let { FirBackedJavaClassifierType(it, session) }
