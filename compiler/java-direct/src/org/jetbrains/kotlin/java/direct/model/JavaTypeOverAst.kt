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
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTagImpl
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

/**
 * Extracts identifier segments from a JAVA_CODE_REFERENCE node via the AST,
 * so type-argument lists cannot be confused with package/class separators. Handles:
 * - Simple: "Object" → ["Object"]
 * - Qualified: "java.util.List" → ["java", "util", "List"]
 * - Annotated: "@NotNull Object" → ["Object"]
 * - Generic: "List<String>" → ["List"]
 * - Nested generic: "Outer<T>.Inner<U>" → ["Outer", "Inner"]
 */
private fun JavaLightTree.extractReferenceNameParts(node: JavaLightNode): List<String> {
    val parts = mutableListOf<String>()

    fun collectIdentifiers(current: JavaLightNode) {
        for (child in getChildren(current)) {
            when (getType(child)) {
                JavaSyntaxTokenType.IDENTIFIER -> parts.add(getText(child).toString())
                JavaSyntaxElementType.JAVA_CODE_REFERENCE -> collectIdentifiers(child)
                // Skip: ANNOTATION, REFERENCE_PARAMETER_LIST, WHITE_SPACE, DOT, etc.
            }
        }
    }

    collectIdentifiers(node)
    return parts
}

class JavaClassifierTypeOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, tree, resolutionContext, extraAnnotations, memberAnnotations), JavaClassifierType {

    private val rawTypeNameParts: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        tree.extractReferenceNameParts(node)
    }

    private val rawTypeName: String
        get() {
            val parts = rawTypeNameParts
            return if (parts.size == 1) parts[0] else parts.joinToString(".")
        }

    override val classifier: JavaClassifier? by lazy(LazyThreadSafetyMode.PUBLICATION) { computeClassifier() }

    private fun computeClassifier(): JavaClassifier? {
        val parts = rawTypeNameParts

        with(resolutionContext) {
            if (parts.size == 1) {
                // Matches PSI implementation's order; javac prefers a same-named nested class over an own type parameter.
                // Reflected in `diagnostics/tests/javac/typeParameters/OwnNestedClassAndTypeParameterWithSameNames.kt` and
                // `InheritedInnerAndTypeParameterWithSameNames.kt`.
                // TODO: consider switching to javac behavior (KT-88935)
                findTypeParameter(parts[0])?.let { return it }
                findClassInCurrentScope(parts[0])?.let { return it }
                findInheritedTypeParameter(parts[0])?.let { return it }
            }

            // Unlike [resolve] below, needs no `FirSession` symbol provider: serves parser-only tests, and saves
            // a symbol-provider round-trip per segment.
            var current: JavaClassifier? = findClassInCurrentScope(parts[0])

            if (current is JavaClass) {
                for (i in 1 until parts.size) {
                    val part = Name.identifier(parts[i])
                    current = declaredOrFullyInherited(current as JavaClass, part)
                        ?: return null
                }
                return current
            }

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

    /** Raw (JLS 4.6) when an expected type argument is neither explicit nor recoverable */
    override val isRaw: Boolean
        get() = typeArguments.any { it == null }

    override val typeArguments: List<JavaType?> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computeTypeArguments()
    }

    /**
     * The classifier's own first, then the enclosing instances' ones; `static` class stops the traversal.
     */
    private fun computeTypeArguments(): List<JavaType?> {
        val allRefParamLists = collectAllRefParamLists(node)

        // The innermost class's explicit type arguments come from the LAST REFERENCE_PARAMETER_LIST.
        val explicitArgs = allRefParamLists.lastOrNull()?.let { pl ->
            tree.getChildren(pl)
                .filter { tree.getType(it) == JavaSyntaxElementType.TYPE }
                .map { typeNode -> createJavaType(typeNode, tree, resolutionContext) }
        } ?: emptyList()

        val javaClass = classifier as? JavaClass

        val ownParameterCount = javaClass?.typeParameters?.size ?: 0
        val ownArgs: List<JavaType?> =
            if (javaClass != null && explicitArgs.size < ownParameterCount) List(ownParameterCount) { null }
            else explicitArgs

        if (allRefParamLists.size > 1) {
            val outerExplicitArgs = allRefParamLists.dropLast(1).reversed().flatMap { paramList ->
                tree.getChildren(paramList).filter { tree.getType(it) == JavaSyntaxElementType.TYPE }
                    .map { createJavaType(it, tree, resolutionContext) }
            }
            if (outerExplicitArgs.isNotEmpty()) {
                return ownArgs + outerExplicitArgs
            }
        }

        if (javaClass == null || javaClass.isStatic) return ownArgs

        val outerTypeParams = mutableListOf<JavaTypeParameter>()
        var anyOutOfScope = false
        var outer = javaClass.outerClass
        while (outer != null) {
            if (outer.typeParameters.isNotEmpty()) {
                if (!isInScopeOfDeclaringClass(outer)) anyOutOfScope = true
                outerTypeParams.addAll(outer.typeParameters)
            }
            outer = if (outer.isStatic) null else outer.outerClass
        }
        if (outerTypeParams.isEmpty()) return ownArgs

        // Naming the enclosing class opts out of its implicit arguments: `Outer.Inner` is raw even inside
        // `Outer`'s own body. A qualifier which only inherits the inner class (`Sub.Inner` with
        // `class Sub extends Outer<String>`) takes them from that subclass's supertypes instead.
        val isQualified = rawTypeNameParts.size > 1
        val qualifiedByDeclaringOuter =
            isQualified && rawTypeNameParts[rawTypeNameParts.size - 2] == javaClass.outerClass?.name?.asString()

        // Out of scope: the reference sits in a class which merely inherits the inner class
        // (`class Outer<E1, E2> extends BaseOuter<Integer, E1>` referencing `BaseInner`), and the declaring
        // class's parameters denote nothing there.
        if (anyOutOfScope || isQualified) {
            val classId = javaClass.classId
            if (!qualifiedByDeclaringOuter && classId != null) {
                val recovered = with(resolutionContext) { recoverInheritedOuterTypeArguments(classId) }
                if (recovered != null) return ownArgs + recovered
            }
            // Nothing binds the enclosing instance's parameters at this reference.
            return ownArgs + List(outerTypeParams.size) { null }
        }

        // The declaring class's own instances: FIR matches a `JavaTypeParameter` to its `FirTypeParameterSymbol`
        // by identity in the per-class `JavaTypeParameterStack`.
        return ownArgs + outerTypeParams.map { JavaTypeParameterTypeOverAst(it) }
    }

    /**
     * Whether this type reference is written inside [declaringClass], i.e. whether
     * [declaringClass]'s own type parameters denote the enclosing instance's ones here.
     *
     * By identity, not by name: a same-named parameter of a nested class or of an enclosing generic
     * method shadows the outer one for name resolution but is not the one the implicit argument denotes
     * (`class A<T> { class Inner<T> { Inner<String> foo(); } }` means `A<A.T>.Inner<String>`).
     */
    private fun isInScopeOfDeclaringClass(declaringClass: JavaClass): Boolean {
        val declaringClassId = declaringClass.classId
        var enclosing: JavaClass? = resolutionContext.scopeContext.containingClass
        while (enclosing != null) {
            if (enclosing === declaringClass) return true
            // Defensive: `FirBackedJavaClassAdapter` is built fresh per call, so a class visible both as
            // source and through the symbol provider — e.g. a previous build's `.class` file on the
            // classpath of an incremental run — can be seen through two non-identical instances.
            if (declaringClassId != null && enclosing.classId == declaringClassId) return true
            if (enclosing.isStatic) return false
            enclosing = enclosing.outerClass
        }
        return false
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
    // Annotations of *this* array level only, already bound by [arrayLevelAnnotations].
    // Not delegated to [JavaTypeOverAst]: all levels of a multi-dimensional array share the one
    // TYPE node the parser produces, so its node scan would report every level's annotations on
    // every level.
    override val annotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, tree, resolutionContext), JavaArrayType

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
 * Annotations are bound per level (JLS 9.7.4), exactly like the class-file peer binds them by JVM
 * type path (`BinaryJavaAnnotation.computeTargetType` in `impl/classFiles/Annotations.kt`):
 * - each `[]` pair keeps only the annotations written in front of it, the leftmost pair being the
 *   outermost array — see [arrayLevelAnnotations];
 * - [memberAnnotations] (i.e. what stands in front of the *type name*, which for a field/method/
 *   parameter the parser puts in the member's MODIFIER_LIST) annotate the element type, so they are
 *   handed to the component and to nothing else. `@NotNull Foo[] f()` is therefore `Array<Foo>`,
 *   not `Array<Foo!>` — while the array head stays unannotated, the member's own `annotations`
 *   being what reaches FIR as the container annotations of the declaration.
 */
private fun tryCreateArrayOrVarargFromTypeNode(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaType? {
    val levels = arrayLevelAnnotations(typeNode, tree, resolutionContext)
    if (levels.isEmpty()) return null
    val componentTypeNode = tree.findChildByType(typeNode, JavaSyntaxElementType.TYPE) ?: return null

    var result: JavaType = createJavaType(componentTypeNode, tree, resolutionContext, memberAnnotations = memberAnnotations)
    // Innermost first, so the levels are consumed from the rightmost dimension leftwards.
    for (levelAnnotations in levels.asReversed()) {
        result = JavaArrayTypeOverAst(typeNode, tree, resolutionContext, result, levelAnnotations)
    }
    return result
}

/**
 * Splits the direct ANNOTATION children of an array/vararg [typeNode] into one group per array
 * level, outermost level first, or returns an empty list when [typeNode] is no array at all.
 *
 * The parser emits the whole `ANNOTATION* ('[' ']' | '...')` sequence flat, after the component
 * TYPE, so a level is closed by its own `[` (or by the vararg `...`) and owns the annotations
 * accumulated since the previous one. JLS 9.7.4: the leftmost pair is the outermost array — `String
 * @Outer [] @Inner []` is an `@Outer` array of `@Inner` arrays of `String`.
 *
 * A vararg `...` is just the rightmost dimension of the declared type (JLS 8.4.1: `T... x` has type
 * `T[]`; JLS 10.2 treats the ellipsis as a bracket pair, so `int @A [] @B [] x` and
 * `int @A [] @B ... y` have the same array type), so it is counted and annotated like a `[]` pair:
 * `String [] @Nullable ... x` is an array of `@Nullable` arrays of `String`.
 */
private fun arrayLevelAnnotations(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): List<List<JavaAnnotation>> {
    val levels = mutableListOf<List<JavaAnnotation>>()
    var pending = mutableListOf<JavaAnnotation>()
    for (child in tree.getChildren(typeNode)) {
        when (tree.getType(child)) {
            JavaSyntaxElementType.ANNOTATION -> pending.add(JavaAnnotationOverAst(child, tree, resolutionContext))
            JavaSyntaxTokenType.LBRACKET, JavaSyntaxTokenType.ELLIPSIS -> {
                levels.add(pending)
                pending = mutableListOf()
            }
        }
    }
    return levels
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
        //
        // Only for a *simple* name though: JLS 9.7.4 binds an annotation written in front of a
        // qualified name to its leftmost segment, so `@NotNull A.B` annotates `A` and says nothing
        // about the denoted type — which is written `A.@NotNull B` and picked up from the reference
        // node itself. In the one case where such an annotation is admissible at all (`@Foo C.D`
        // with `D` an inner class of `C`) it is dropped, because the model has no node for the
        // outer type: `C.D` is a single classifier type whose arguments are `D`'s followed by `C`'s.
        // Same at the PSI boundary — `PsiClassReferenceType` keeps such annotations in a separate
        // qualifier channel that `getAnnotations()` never reports — and, differently, on the
        // class-file side, where `BinaryJavaAnnotation.translatePath` skips the `INNER_TYPE` steps
        // of a JVM type path, so `@A Map.Entry` and `Map.@A Entry` collapse onto that one type.
        val referenceIsQualified = tree.findChildByType(referenceNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE) != null
        val typeNodeAnnotations = when {
            referenceIsQualified -> emptyList()
            else -> tree.getChildrenByType(typeNode, JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        }
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
    /** Declaration chain for a type parameter among the nested arguments (`List<E>`), see [firBackedJavaType]. */
    private val declarationChainRoot: JavaClass? = null,
) : JavaClassifierType {
    override val classifier: JavaClassifier = FirBackedJavaClassAdapter(coneType.lookupTag.classId, session)
    override val classifierQualifiedName: String get() = coneType.lookupTag.classId.asSingleFqName().asString()
    override val presentableText: String get() = classifierQualifiedName
    override val isRaw: Boolean get() = coneType.isRaw()

    override val typeArguments: List<JavaType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        coneType.typeArguments.map { firBackedJavaType(it, session, declarationChainRoot) }
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
 *
 * [declarationChainRoot] is the class whose supertype the projection was taken from. `JavaTypeConversion`
 * resolves a [JavaTypeParameter] by identity in that class's `MutableJavaTypeParameterStack`, so a cone type
 * parameter has to be handed back as the model's own instance from that chain.
 */
internal fun firBackedJavaType(
    projection: ConeTypeProjection,
    session: FirSession,
    declarationChainRoot: JavaClass? = null,
): JavaType {
    // Arguments read from a resolved FIR supertype are flexible; the Java model is nullability-agnostic and
    // FIR re-derives flexibility when converting back.
    return when (val type = (projection as? ConeKotlinType)?.lowerBoundIfFlexible() ?: projection) {
        is ConeStarProjection -> FirBackedJavaWildcardType(bound = null, isExtends = true)
        is ConeKotlinTypeProjectionIn ->
            FirBackedJavaWildcardType(bound = firBackedClassifierOrNull(type.type, session, declarationChainRoot), isExtends = false)
        is ConeKotlinTypeProjectionOut ->
            FirBackedJavaWildcardType(bound = firBackedClassifierOrNull(type.type, session, declarationChainRoot), isExtends = true)
        is ConeClassLikeType -> FirBackedJavaClassifierType(type, session, declarationChainRoot)
        is ConeTypeParameterType -> {
            val lookupTag = type.lookupTag
            val parameter = if (declarationChainRoot != null && lookupTag is ConeTypeParameterLookupTagImpl) {
                javaTypeParameterInDeclarationChain(declarationChainRoot, lookupTag.typeParameterSymbol)
            } else {
                null
            }
            parameter?.let { JavaTypeParameterTypeOverAst(it) } ?: FirBackedJavaWildcardType(bound = null, isExtends = true)
        }
        // Error and captured types have no Java-model representation; an unbounded wildcard keeps them resolvable.
        else -> FirBackedJavaWildcardType(bound = null, isExtends = true)
    }
}

private fun firBackedClassifierOrNull(type: ConeKotlinType, session: FirSession, declarationChainRoot: JavaClass?): JavaType? =
    (type.lowerBoundIfFlexible() as? ConeClassLikeType)?.let { FirBackedJavaClassifierType(it, session, declarationChainRoot) }
