/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.element.SyntaxElementTypes
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
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
    // These need callback-based filtering since they may or may not be TYPE_USE.
    private val memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaType, JavaAnnotationOwner {
    // Callback-independent annotations: extra + MODIFIER_LIST children + direct ANNOTATION children.
    private val typePositionAnnotations: Collection<JavaAnnotation>
        get() = extraAnnotations + collectModifierListAndDirectAnnotations(node, tree, resolutionContext)

    override val annotations: Collection<JavaAnnotation>
        get() = memberAnnotations + typePositionAnnotations

    override val needsTypeUseAnnotationFiltering: Boolean get() = true

    override fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
        // Step 4.5a: post-injection, `JavaAnnotation.classId` is reliable for every annotation
        // reference (no callback fallback needed) per
        // `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §3.
        val filteredMemberAnnotations = memberAnnotations.filter { annotation ->
            val fqName = annotation.classId?.asSingleFqName()?.asString() ?: return@filter false
            isTypeUseAnnotation(fqName)
        }

        return typePositionAnnotations + filteredMemberAnnotations
    }

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

    override val classifier: JavaClassifier?
        get() = computeClassifier()

    private fun computeClassifier(): JavaClassifier? {
        val parts = rawTypeNameParts

        if (parts.size == 1) {
            // Resolution order for simple names (matches Java scoping rules):
            // 1. OWN type parameters (method/class own — high priority, win over inner class names)
            resolutionContext.findTypeParameter(parts[0])?.let { return it }
            // 2. Inner/local class names (shadow INHERITED outer type params)
            val localClass = resolutionContext.findLocalClass(Name.identifier(parts[0]))
            if (localClass != null) return localClass
            // 3. INHERITED type parameters from outer class (low priority — shadowed by inner classes)
            resolutionContext.findInheritedTypeParameter(parts[0])?.let { return it }
        }

        // Multi-part names: navigate from base class through inner classes
        var current: JavaClassifier? = resolutionContext.findLocalClass(Name.identifier(parts[0]))

        if (current is JavaClass) {
            for (i in 1 until parts.size) {
                current = (current as JavaClass).findInnerClass(Name.identifier(parts[i]))
                    ?: return null
            }
            return current
        }

        // Cross-file references stay `classifier == null` (the pre-`java-direct` shape)
        // so the FIR side's `JavaClassifierType is JavaClass` branch in `JavaTypeConversion`
        // does not fire on a structurally incomplete adapter. The resolved `ClassId` is
        // exposed via [resolvedCrossFileClassId] (a hint that `JavaTypeConversion.resolveTypeName`
        // consults under post-Step-4.5a injection per
        // `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §3).
        return null
    }

    /**
     * Cross-file [ClassId] resolved via the model's own resolver (Step 4.5a per
     * [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §3 / §5 / §11), or `null`
     * when no [LazySessionAccess] is wired (parsing-level unit-test fixtures) or no
     * cross-file resolution succeeds.
     *
     * Subsumes the work the deleted `JavaClassifierType.resolve(...)` callback API used
     * to do for cross-file references, without exposing a structurally-empty `JavaClass`
     * adapter through the [classifier] interface field. `JavaTypeConversion.resolveTypeName`
     * reads this hint as a primary source of truth, falling back to `findClassIdByFqNameString`
     * when it is `null` (the pre-`java-direct` shape).
     */
    override val resolvedClassId: ClassId? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (!resolutionContext.hasLazySessionAccess) return@lazy null
        if (classifier != null) return@lazy null
        resolutionContext.resolve(rawTypeName)
    }

    /**
     * Returns true when this type references a class that should produce a trivially flexible
     * ConeFlexibleType (isTrivial=true), rendering as `T!` instead of `ft<T, T?>`.
     *
     * With PSI, the `classifier` property is non-null for all resolved classes, and
     * `isTriviallyFlexible()` is checked directly. With java-direct, `classifier` is null
     * for external classes (JDK, libraries, cross-file), so this hint provides the equivalent
     * check.
     *
     * A class is trivially flexible unless it's a Kotlin read-only collection mapped class
     * (e.g., java.util.List → kotlin.collections.List), which needs mutable/readonly distinction.
     */
    override val isTriviallyFlexibleHint: Boolean
        get() = computeIsTriviallyFlexibleHint()

    private fun computeIsTriviallyFlexibleHint(): Boolean {
        if (classifier != null) return false // local lookup found it — handled by isTriviallyFlexible()
        val parts = rawTypeNameParts

        // Cross-file Java source class (same module, different file)
        if (parts.size == 1 && resolutionContext.isUnambiguouslyCrossFileClass(parts[0])) return true

        // For types resolved via explicit imports, check the Java FQN against the read-only set
        val qualifiedName = classifierQualifiedName
        if (qualifiedName != rawTypeName) {
            return FqName(qualifiedName) !in JAVA_READ_ONLY_FQ_NAMES
        }

        // Unresolved simple name (java.lang implicit import, star imports, same-package).
        // Conservatively check against simple names of read-only collection classes.
        if (parts.size == 1) {
            return parts[0] !in JAVA_READ_ONLY_SIMPLE_NAMES
        }

        return false
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
        val qualified = resolutionContext.getSimpleImport(parts[0])
        if (qualified != null && resolutionContext.isImportTargetAvailableAsJavaClass(parts[0])) {
            var result = qualified.asString()
            for (i in 1 until parts.size) {
                result += "." + parts[i]
            }
            return result
        }

        // 4. Return as-is - FIR will resolve via callback (same package, star imports, java.lang types)
        return rawTypeName
    }

    override val presentableText: String get() = tree.getText(node).toString()

    override val isRaw: Boolean
        get() = computeIsRaw()

    private fun computeIsRaw(): Boolean {
        // A type is raw if it has no type arguments but the class has type parameters.
        // Also raw if fewer args than params (javac treats wrong-arity as error).
        // Note: REFERENCE_PARAMETER_LIST may exist but be empty (no TYPE children).
        val parameterList = tree.findChildByType(node, JavaSyntaxElementType.REFERENCE_PARAMETER_LIST)
        val explicitArgCount = parameterList?.let { pl ->
            tree.getChildren(pl).count { tree.getType(it) == JavaSyntaxElementType.TYPE }
        } ?: 0
        val javaClass = classifier as? JavaClass ?: return false
        val typeParamCount = javaClass.typeParameters.size
        if (typeParamCount == 0) return false
        return explicitArgCount == 0 || explicitArgCount < typeParamCount
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
            return explicitArgs
        }

        // Resolve each outer type param through the current context so we get the caller's H
        // (e.g., Outer.H) rather than the abstract H from the outer class declaration.
        val implicitArgs = outerTypeParams.map { typeParam ->
            val resolved = resolutionContext.findTypeParameter(typeParam.name.asString())
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

    // `classifier` already consults `findTypeParameter` + `findLocalClass`, so a positive
    // classifier result already implies the type-parameter / local-class paths. The only
    // additional resolution step this property needs is the explicit-import check.
    override val isResolved: Boolean
        get() = classifier != null || resolutionContext.getSimpleImport(rawTypeNameParts[0]) != null

    override val containingClassIds: List<ClassId>
        get() = resolutionContext.getContainingClassIds()

    private companion object {
        /** Java FQNs of Kotlin read-only collection classes (e.g., java.util.List, java.util.Map). */
        private val JAVA_READ_ONLY_FQ_NAMES: Set<FqName> = JavaToKotlinClassMap.getReadOnlyAsJava()

        /** Simple names of read-only collection classes for conservative matching of unresolved names. */
        private val JAVA_READ_ONLY_SIMPLE_NAMES: Set<String> =
            JAVA_READ_ONLY_FQ_NAMES.mapTo(mutableSetOf()) { it.shortName().asString() }
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

    override val isResolved: Boolean get() = true
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

    override val isResolved: Boolean get() = true
}

fun createJavaType(
    node: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
): JavaType {
    // If input node is a TYPE with array brackets, vararg ellipsis, or '?' wildcard, handle it
    // directly. Don't look for a nested TYPE first — that would skip the outer array dimension
    // or mistake a wildcard-bound TYPE child for the wildcard itself.
    if (tree.getType(node) == JavaSyntaxElementType.TYPE) {
        val arrayOrVararg = tryCreateArrayOrVarargFromTypeNode(node, tree, resolutionContext, extraAnnotations, memberAnnotations)
        if (arrayOrVararg != null) return arrayOrVararg

        if (tree.findChildByType(node, JavaSyntaxTokenType.QUEST) != null) {
            return createWildcardType(node, tree, resolutionContext, extraAnnotations, memberAnnotations)
        }
    }

    val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE) ?: node

    if (tree.findChildByType(typeNode, JavaSyntaxTokenType.QUEST) != null) {
        return createWildcardType(typeNode, tree, resolutionContext, extraAnnotations, memberAnnotations)
    }

    val arrayOrVararg = tryCreateArrayOrVarargFromTypeNode(typeNode, tree, resolutionContext, extraAnnotations, memberAnnotations)
    if (arrayOrVararg != null) return arrayOrVararg

    return createClassifierOrPrimitive(typeNode, tree, resolutionContext, extraAnnotations, memberAnnotations)
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
 * type's nullability, not the array's. For non-vararg arrays the split is a no-op
 * (`hasVarargEllipsis = false` leaves member annotations on the outer wrapper), so the same
 * helper is safe for both the TYPE-input and derived-`typeNode` paths of [createJavaType].
 */
private fun tryCreateArrayOrVarargFromTypeNode(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation>,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaType? {
    val arrayDimensions = tree.getChildren(typeNode).count { tree.getType(it) == JavaSyntaxTokenType.LBRACKET }
    val hasVarargEllipsis = tree.findChildByType(typeNode, JavaSyntaxTokenType.ELLIPSIS) != null
    if (arrayDimensions == 0 && !hasVarargEllipsis) return null
    val componentTypeNode = tree.findChildByType(typeNode, JavaSyntaxElementType.TYPE) ?: return null

    val dims = if (hasVarargEllipsis) 1 else arrayDimensions
    val componentMemberAnnotations = if (hasVarargEllipsis) memberAnnotations else emptyList()
    val arrayMemberAnnotations = if (hasVarargEllipsis) emptyList() else memberAnnotations
    var result: JavaType = createJavaType(componentTypeNode, tree, resolutionContext, memberAnnotations = componentMemberAnnotations)
    repeat(dims) { i ->
        result = JavaArrayTypeOverAst(
            typeNode, tree, resolutionContext, result,
            if (i == dims - 1) extraAnnotations else emptyList(),
            if (i == dims - 1) arrayMemberAnnotations else emptyList(),
        )
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
    extraAnnotations: Collection<JavaAnnotation>,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaWildcardTypeOverAst {
    val hasSuper = tree.findChildByType(typeNode, JavaSyntaxTokenType.SUPER_KEYWORD) != null
    val boundTypeNode = tree.findChildByType(typeNode, JavaSyntaxElementType.TYPE)
    val bound = boundTypeNode?.let { createJavaType(it, tree, resolutionContext) }
    val isExtends = !hasSuper
    return JavaWildcardTypeOverAst(typeNode, tree, resolutionContext, bound, isExtends, extraAnnotations, memberAnnotations)
}

/**
 * Falls through to a primitive ([JavaPrimitiveTypeOverAst]) or classifier
 * ([JavaClassifierTypeOverAst]) type depending on which child [typeNode] has.
 */
private fun createClassifierOrPrimitive(
    typeNode: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation>,
    memberAnnotations: Collection<JavaAnnotation>,
): JavaType {
    val primitiveNode = tree.getChildren(typeNode).find {
        val t = tree.getType(it)
        t in SyntaxElementTypes.PRIMITIVE_TYPE_BIT_SET || t == JavaSyntaxTokenType.VOID_KEYWORD
    }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, tree, resolutionContext, extraAnnotations, memberAnnotations)
    }

    val referenceNode = tree.findChildByType(typeNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
    if (referenceNode != null) {
        // TYPE_USE annotations on type arguments appear directly under the TYPE node (not in MODIFIER_LIST).
        // Pass them as extraAnnotations since we're using JAVA_CODE_REFERENCE as the node.
        val typeNodeAnnotations = tree.getChildrenByType(typeNode, JavaSyntaxElementType.ANNOTATION)
            .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        return JavaClassifierTypeOverAst(referenceNode, tree, resolutionContext, extraAnnotations + typeNodeAnnotations, memberAnnotations)
    }
    return JavaClassifierTypeOverAst(typeNode, tree, resolutionContext, extraAnnotations, memberAnnotations)
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
) : JavaClassifierType {
    override val classifier: JavaClassifier? get() = null // External class, will be resolved by FIR
    override val classifierQualifiedName: String get() = "java.lang.Enum"
    override val typeArguments: List<JavaType> get() = listOf(EnumSelfTypeArgument())
    override val isRaw: Boolean get() = false
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val presentableText: String get() = "java.lang.Enum<${enumClass.fqName}>"
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isResolved: Boolean get() = true

    private inner class EnumSelfTypeArgument : JavaClassifierType {
        override val classifier: JavaClassifier get() = enumClass
        override val classifierQualifiedName: String get() = enumClass.fqName?.asString() ?: ""
        override val typeArguments: List<JavaType> get() = emptyList()
        override val isRaw: Boolean get() = false
        override val annotations: Collection<JavaAnnotation> get() = emptyList()
        override val presentableText: String get() = classifierQualifiedName
        override val isDeprecatedInJavaDoc: Boolean get() = false
        override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

        override val isResolved: Boolean get() = true
    }
}

/** Classifier type for well-known external classes (e.g. `java.lang.Object`), resolved by FIR. */
class SimpleClassifierType(
    override val classifierQualifiedName: String,
) : JavaClassifierType {
    override val classifier: JavaClassifier? get() = null // External class, will be resolved by FIR
    override val typeArguments: List<JavaType> get() = emptyList()
    override val isRaw: Boolean get() = false
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val presentableText: String get() = classifierQualifiedName
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isResolved: Boolean get() = true
}
