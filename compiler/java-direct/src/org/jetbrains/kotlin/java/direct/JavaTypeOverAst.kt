/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.java.syntax.element.SyntaxElementTypes
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaTypeOverAst(
    val node: JavaSyntaxNode,
    protected val resolutionContext: JavaResolutionContext,
    // Annotations from type positions (TYPE node → JAVA_CODE_REFERENCE pass-through).
    // These are TYPE_USE by syntactic position and returned unconditionally.
    private val extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    // Annotations from the containing member's modifier list (method/field/parameter).
    // These need callback-based filtering since they may or may not be TYPE_USE.
    private val memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaType, JavaAnnotationOwner {
    override val annotations: Collection<JavaAnnotation>
        get() {
            val modifierListAnnotations =
                node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                    ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                    ?: emptyList()

            val directAnnotations = node.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, resolutionContext) }

            return memberAnnotations + extraAnnotations + modifierListAnnotations + directAnnotations
        }

    override fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
        // Type-position annotations (from the type node itself) are TYPE_USE by syntax.
        val modifierListAnnotations =
            node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
        val directAnnotations = node.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            .map { JavaAnnotationOverAst(it, resolutionContext) }
        val typePositionAnnotations = extraAnnotations + modifierListAnnotations + directAnnotations

        // Member modifier list annotations need callback filtering.
        val filteredMemberAnnotations = memberAnnotations.filter { annotation ->
            val fqName = if (annotation.isResolved) {
                annotation.classId?.asSingleFqName()?.asString()
            } else {
                annotation.resolveAnnotation { candidateClassId ->
                    isTypeUseAnnotation(candidateClassId.asSingleFqName().asString())
                }?.asSingleFqName()?.asString()
            } ?: return@filter false
            isTypeUseAnnotation(fqName)
        }

        return typePositionAnnotations + filteredMemberAnnotations
    }

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations, memberAnnotations), JavaClassifierType {

    private val rawTypeName: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Extract the type name from AST structure, excluding annotations.
        // For "java.util.List" → collect IDENTIFIERs: ["java", "util", "List"]
        // For "@NotNull Object" → skip ANNOTATION, get IDENTIFIER: "Object"
        // For "Outer<T>.Inner" → traverse JAVA_CODE_REFERENCEs recursively
        extractTypeName(node)
    }

    /**
     * Extracts type name from a JAVA_CODE_REFERENCE node, ignoring annotations and type arguments.
     * Handles:
     * - Simple: "Object" → "Object"
     * - Qualified: "java.util.List" → "java.util.List"
     * - Annotated: "@NotNull Object" → "Object"
     * - Generic: "List<String>" → "List"
     * - Nested generic: "Outer<T>.Inner<U>" → "Outer.Inner"
     */
    private fun extractTypeName(n: JavaSyntaxNode): String {
        val parts = mutableListOf<String>()
        collectIdentifiers(n, parts)
        return parts.joinToString(".")
    }

    private fun collectIdentifiers(n: JavaSyntaxNode, parts: MutableList<String>) {
        for (child in n.children) {
            when (child.type) {
                JavaSyntaxTokenType.IDENTIFIER -> parts.add(child.text)
                JavaSyntaxElementType.JAVA_CODE_REFERENCE -> collectIdentifiers(child, parts)
                // Skip: ANNOTATION, REFERENCE_PARAMETER_LIST, WHITE_SPACE, DOT, etc.
            }
        }
    }

    override val classifier: JavaClassifier? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val parts = rawTypeName.split('.')

        if (parts.size == 1) {
            // Resolution order for simple names (matches Java scoping rules):
            // 1. OWN type parameters (method/class own — high priority, win over inner class names)
            resolutionContext.findTypeParameter(parts[0])?.let { return@lazy it }
            // 2. Inner/local class names (shadow INHERITED outer type params)
            val localClass = resolutionContext.findLocalClass(Name.identifier(parts[0]))
            if (localClass != null) return@lazy localClass
            // 3. INHERITED type parameters from outer class (low priority — shadowed by inner classes)
            resolutionContext.findInheritedTypeParameter(parts[0])?.let { return@lazy it }
        }

        // Multi-part names: navigate from base class through inner classes
        var current: JavaClassifier? = resolutionContext.findLocalClass(Name.identifier(parts[0]))

        if (current is JavaClass) {
            for (i in 1 until parts.size) {
                current = (current as JavaClass).findInnerClass(Name.identifier(parts[i]))
                    ?: return@lazy null
            }
        }
        current
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
    override val isTriviallyFlexibleHint: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (classifier != null) return@lazy false // local lookup found it — handled by isTriviallyFlexible()
        val parts = rawTypeName.split('.')

        // Cross-file Java source class (same module, different file)
        if (parts.size == 1 && resolutionContext.isUnambiguouslyCrossFileClass(parts[0])) return@lazy true

        // For types resolved via explicit imports, check the Java FQN against the read-only set
        val qualifiedName = classifierQualifiedName
        if (qualifiedName != rawTypeName) {
            return@lazy FqName(qualifiedName) !in JAVA_READ_ONLY_FQ_NAMES
        }

        // Unresolved simple name (java.lang implicit import, star imports, same-package).
        // Conservatively check against simple names of read-only collection classes.
        if (parts.size == 1) {
            return@lazy parts[0] !in JAVA_READ_ONLY_SIMPLE_NAMES
        }

        false
    }

    override val classifierQualifiedName: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val parts = rawTypeName.split('.')

        // 1. Check type parameters - return name as-is (FIR handles type params specially)
        if (parts.size == 1 && resolutionContext.findTypeParameter(parts[0]) != null) {
            return@lazy rawTypeName
        }

        // 2. Check local scope (same compilation unit)
        val localBase = resolutionContext.findLocalClass(Name.identifier(parts[0]))
        if (localBase != null) {
            var current: JavaClass? = localBase
            for (i in 1 until parts.size) {
                current = current?.findInnerClass(Name.identifier(parts[i]))
            }
            return@lazy current?.fqName?.asString() ?: rawTypeName
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
            return@lazy result
        }

        // 4. Return as-is - FIR will resolve via callback (same package, star imports, java.lang types)
        rawTypeName
    }

    override val presentableText: String get() = node.text

    override val isRaw: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // A type is raw if it has no type arguments but the class has type parameters.
        // Also raw if fewer args than params (javac treats wrong-arity as error).
        // Note: REFERENCE_PARAMETER_LIST may exist but be empty (no TYPE children).
        val parameterList = node.findChildByType(JavaSyntaxElementType.REFERENCE_PARAMETER_LIST)
        val explicitArgCount = parameterList?.children?.count { it.type == JavaSyntaxElementType.TYPE } ?: 0
        val javaClass = classifier as? JavaClass ?: return@lazy false
        val typeParamCount = javaClass.typeParameters.size
        if (typeParamCount == 0) return@lazy false
        explicitArgCount == 0 || explicitArgCount < typeParamCount
    }

    override val typeArguments: List<JavaType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // Collect all REFERENCE_PARAMETER_LISTs from this node and nested JAVA_CODE_REFERENCEs.
        // This handles both flat ("A<T>.B<U>" → [<T>, <U>] as direct children) and
        // nested ("A<T>.B<U>" → child JAVA_CODE_REF("A<T>") + sibling REFPARAMLIST(<U>)) structures.
        val allRefParamLists = collectAllRefParamLists(node)

        // The innermost class's explicit type arguments come from the LAST REFERENCE_PARAMETER_LIST.
        val explicitArgs = allRefParamLists.lastOrNull()
            ?.children
            ?.filter { it.type == JavaSyntaxElementType.TYPE }
            ?.map { typeNode -> createJavaType(typeNode, resolutionContext) }
            ?: emptyList()

        // For qualified generic types like "BaseOuter<H>.BaseInner<Double, String>", the earlier
        // REFERENCE_PARAMETER_LISTs contain explicit type arguments for the outer classes.
        // These are used directly instead of implicit outer type params — for cross-file types
        // (classifier == null) the source-level outer args are the only information available.
        if (allRefParamLists.size > 1) {
            val outerExplicitArgs = allRefParamLists.dropLast(1).reversed().flatMap { paramList ->
                paramList.children.filter { it.type == JavaSyntaxElementType.TYPE }
                    .map { createJavaType(it, resolutionContext) }
            }
            if (outerExplicitArgs.isNotEmpty()) {
                return@lazy explicitArgs + outerExplicitArgs
            }
        }

        // Simple (non-qualified) type: for non-static inner classes, add implicit outer type params.
        // This handles references like "Inner<U>" inside Outer<T> where the outer T is implicit.
        val javaClass = classifier as? JavaClass
        if (javaClass == null || javaClass.isStatic) {
            return@lazy explicitArgs
        }

        val outerTypeParams = mutableListOf<JavaTypeParameter>()
        var outer = javaClass.outerClass
        while (outer != null && !outer.isStatic) {
            outerTypeParams.addAll(outer.typeParameters)
            outer = outer.outerClass
        }

        if (outerTypeParams.isEmpty()) {
            return@lazy explicitArgs
        }

        // Resolve each outer type param through the current context so we get the caller's H
        // (e.g., Outer.H) rather than the abstract H from the outer class declaration.
        val implicitArgs = outerTypeParams.map { typeParam ->
            val resolved = resolutionContext.findTypeParameter(typeParam.name.asString())
            if (resolved != null) JavaTypeParameterTypeOverAst(resolved)
            else JavaTypeParameterTypeOverAst(typeParam)
        }

        explicitArgs + implicitArgs
    }

    /**
     * Recursively collects all REFERENCE_PARAMETER_LIST nodes in source order,
     * traversing into child JAVA_CODE_REFERENCE nodes (for nested qualified types).
     * For "A<T>.B<U>" → [paramList(<T>), paramList(<U>)] regardless of AST structure.
     */
    private fun collectAllRefParamLists(n: JavaSyntaxNode): List<JavaSyntaxNode> {
        val result = mutableListOf<JavaSyntaxNode>()
        for (child in n.children) {
            when (child.type) {
                JavaSyntaxElementType.JAVA_CODE_REFERENCE -> result.addAll(collectAllRefParamLists(child))
                JavaSyntaxElementType.REFERENCE_PARAMETER_LIST -> result.add(child)
            }
        }
        return result
    }

    override val isResolved: Boolean
        get() {
            // Already resolved if we found a local classifier (including inner classes)
            if (classifier != null) return true

            val parts = rawTypeName.split('.')

            // Type parameter reference (single name only)
            if (parts.size == 1 && resolutionContext.findTypeParameter(rawTypeName) != null) return true

            // Explicit simple import for the first part resolves it
            if (resolutionContext.getSimpleImport(parts[0]) != null) return true

            // For unqualified names (no dots), we need resolution
            // For qualified names like "Map.Entry", we need to resolve the outer class
            // Only fully qualified names starting with a package are considered resolved
            // We can't reliably distinguish "java.util.Map" from "Outer.Inner" by case alone
            // So we consider anything that didn't match above as unresolved
            return false
        }

    override val containingClassIds: List<ClassId> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolutionContext.getContainingClassIds()
    }

    override fun resolve(
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)?,
    ): ClassId? {
        return resolutionContext.resolve(rawTypeName, tryResolve, getSupertypeClassIds)
    }

    private companion object {
        /** Java FQNs of Kotlin read-only collection classes (e.g., java.util.List, java.util.Map). */
        private val JAVA_READ_ONLY_FQ_NAMES: Set<FqName> = JavaToKotlinClassMap.getReadOnlyAsJava()

        /** Simple names of read-only collection classes for conservative matching of unresolved names. */
        private val JAVA_READ_ONLY_SIMPLE_NAMES: Set<String> =
            JAVA_READ_ONLY_FQ_NAMES.mapTo(mutableSetOf()) { it.shortName().asString() }
    }
}

/**
 * JavaClassifierType for enum entry fields.
 * The type of enum constant is the containing enum class itself.
 */
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

    // Already resolved - we have direct reference to the class
    override val isResolved: Boolean get() = true
    override fun resolve(tryResolve: (ClassId) -> Boolean, getSupertypeClassIds: ((ClassId) -> List<ClassId>)?): ClassId? {
        val fqName = enumClass.fqName ?: return null
        val classId = ClassId.topLevel(fqName)
        return if (tryResolve(classId)) classId else null
    }
}

class JavaPrimitiveTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations, memberAnnotations), JavaPrimitiveType {
    override val type: org.jetbrains.kotlin.builtins.PrimitiveType?
        get() = when (node.text) {
            "void" -> null
            "boolean" -> org.jetbrains.kotlin.builtins.PrimitiveType.BOOLEAN
            "char" -> org.jetbrains.kotlin.builtins.PrimitiveType.CHAR
            "byte" -> org.jetbrains.kotlin.builtins.PrimitiveType.BYTE
            "short" -> org.jetbrains.kotlin.builtins.PrimitiveType.SHORT
            "int" -> org.jetbrains.kotlin.builtins.PrimitiveType.INT
            "float" -> org.jetbrains.kotlin.builtins.PrimitiveType.FLOAT
            "long" -> org.jetbrains.kotlin.builtins.PrimitiveType.LONG
            "double" -> org.jetbrains.kotlin.builtins.PrimitiveType.DOUBLE
            else -> null
        }
}

class JavaArrayTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    override val componentType: JavaType,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations, memberAnnotations), JavaArrayType

class JavaWildcardTypeOverAst(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    override val bound: JavaType?,
    override val isExtends: Boolean,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
) : JavaTypeOverAst(node, resolutionContext, extraAnnotations, memberAnnotations), JavaWildcardType

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

    // Type parameter references are always resolved
    override val isResolved: Boolean get() = true
    // Type parameters don't resolve to ClassId - they're handled specially by FIR
}

fun createJavaType(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
    extraAnnotations: Collection<JavaAnnotation> = emptyList(),
    memberAnnotations: Collection<JavaAnnotation> = emptyList(),
): JavaType {
    // If input node is a TYPE with array brackets or vararg ellipsis, handle it directly
    // (don't look for nested TYPE first, as that would skip the array dimension)
    if (node.type == JavaSyntaxElementType.TYPE) {
        val arrayDimensions = node.children.count { it.type == JavaSyntaxTokenType.LBRACKET }
        val hasVarargEllipsis = node.findChildByType(JavaSyntaxTokenType.ELLIPSIS) != null
        if (arrayDimensions > 0 || hasVarargEllipsis) {
            val componentTypeNode = node.findChildByType(JavaSyntaxElementType.TYPE)
            if (componentTypeNode != null) {
                // The KMP parser places all [] pairs as siblings under the same TYPE node
                // (e.g., List<Double>[][] → TYPE[TYPE[List<Double>], [], []]).
                // We need to wrap the inner type in N array dimensions, innermost first.
                //
                // For varargs (@NonNull String... args), member annotations (from the parameter's
                // MODIFIER_LIST) apply to the component type, not the array wrapper. This matches
                // PSI/javac-wrapper behavior where TYPE_USE annotations like @NonNull enhance the
                // component type's nullability, not the array's.
                val dims = if (hasVarargEllipsis) 1 else arrayDimensions
                val componentMemberAnnotations = if (hasVarargEllipsis) memberAnnotations else emptyList()
                val arrayMemberAnnotations = if (hasVarargEllipsis) emptyList() else memberAnnotations
                var result: JavaType = createJavaType(componentTypeNode, resolutionContext, memberAnnotations = componentMemberAnnotations)
                repeat(dims) { i ->
                    result = JavaArrayTypeOverAst(
                        node, resolutionContext, result,
                        if (i == dims - 1) extraAnnotations else emptyList(),
                        if (i == dims - 1) arrayMemberAnnotations else emptyList(),
                    )
                }
                return result
            }
        }

        // Wildcard type: TYPE contains QUEST (the '?'), optionally with EXTENDS_KEYWORD or SUPER_KEYWORD
        // AST structure: TYPE -> [QUEST, (EXTENDS_KEYWORD|SUPER_KEYWORD)?, TYPE?]
        // Must check on the input TYPE node BEFORE looking for nested TYPE (which would be the bound type)
        if (node.findChildByType(JavaSyntaxTokenType.QUEST) != null) {
            val hasSuper = node.findChildByType(JavaSyntaxTokenType.SUPER_KEYWORD) != null
            val boundTypeNode = node.findChildByType(JavaSyntaxElementType.TYPE)
            val bound = boundTypeNode?.let { createJavaType(it, resolutionContext) }
            val isExtends = !hasSuper
            return JavaWildcardTypeOverAst(node, resolutionContext, bound, isExtends, extraAnnotations, memberAnnotations)
        }
    }

    val typeNode = node.findChildByType(JavaSyntaxElementType.TYPE) ?: node

    // Also check for wildcard on the derived typeNode (for non-TYPE input nodes)
    if (typeNode.findChildByType(JavaSyntaxTokenType.QUEST) != null) {
        val hasSuper = typeNode.findChildByType(JavaSyntaxTokenType.SUPER_KEYWORD) != null
        val boundTypeNode = typeNode.findChildByType(JavaSyntaxElementType.TYPE)
        val bound = boundTypeNode?.let { createJavaType(it, resolutionContext) }
        val isExtends = !hasSuper
        return JavaWildcardTypeOverAst(typeNode, resolutionContext, bound, isExtends, extraAnnotations, memberAnnotations)
    }

    // Array type or vararg: TYPE contains nested TYPE + LBRACKET/RBRACKET or ELLIPSIS
    val arrayDims = typeNode.children.count { it.type == JavaSyntaxTokenType.LBRACKET }
    val hasVarargEllipsis = typeNode.findChildByType(JavaSyntaxTokenType.ELLIPSIS) != null
    if (arrayDims > 0 || hasVarargEllipsis) {
        val componentTypeNode = typeNode.findChildByType(JavaSyntaxElementType.TYPE)
        if (componentTypeNode != null) {
            val dims = if (hasVarargEllipsis) 1 else arrayDims
            var result: JavaType = createJavaType(componentTypeNode, resolutionContext)
            repeat(dims) { i ->
                result = JavaArrayTypeOverAst(
                    typeNode, resolutionContext, result,
                    if (i == dims - 1) extraAnnotations else emptyList(),
                    if (i == dims - 1) memberAnnotations else emptyList(),
                )
            }
            return result
        }
    }

    val primitiveNode =
        typeNode.children.find { it.type in SyntaxElementTypes.PRIMITIVE_TYPE_BIT_SET || it.type == JavaSyntaxTokenType.VOID_KEYWORD }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, resolutionContext, extraAnnotations, memberAnnotations)
    }

    val referenceNode = typeNode.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
    if (referenceNode != null) {
        // TYPE_USE annotations on type arguments appear directly under the TYPE node (not in MODIFIER_LIST)
        // Extract them here and pass as extraAnnotations since we're using JAVA_CODE_REFERENCE as the node
        val typeNodeAnnotations = typeNode.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            .map { JavaAnnotationOverAst(it, resolutionContext) }
        val allAnnotations = extraAnnotations + typeNodeAnnotations
        return JavaClassifierTypeOverAst(referenceNode, resolutionContext, allAnnotations, memberAnnotations)
    }
    return JavaClassifierTypeOverAst(typeNode, resolutionContext, extraAnnotations, memberAnnotations)
}

/**
 * Creates a JavaType with annotations from a member's modifier list.
 * Member annotations are passed separately from type-position annotations so that
 * filterTypeUseAnnotations can apply callback-based filtering only to member annotations
 * while returning type-position annotations unconditionally.
 */
fun createJavaTypeWithAnnotations(
    typeNode: JavaSyntaxNode,
    modifierList: JavaSyntaxNode?,
    resolutionContext: JavaResolutionContext,
): JavaType {
    val memberAnnotations = modifierList?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
        ?.map { JavaAnnotationOverAst(it, resolutionContext) }
        ?: emptyList()
    return createJavaType(typeNode, resolutionContext, memberAnnotations = memberAnnotations)
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
    node: JavaSyntaxNode,
    initialResolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaTypeParameter {

    // Resolution context can be updated after construction to include all sibling type parameters.
    // This is needed for resolving bounds like `S extends JsStubElement<E>` where E is another type param.
    // See the class-level KDoc for the two-phase construction invariant.
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
        get() = Name.identifier(node.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text ?: "<error>")

    override val upperBounds: Collection<JavaClassifierType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val extendsList = node.findChildByType(JavaSyntaxElementType.EXTENDS_BOUND_LIST) ?: return@lazy emptyList()
        // Bounds may be TYPE nodes (with annotations like "T extends @NotNull Object") or bare
        // JAVA_CODE_REFERENCE nodes. Matches TreeBasedTypeParameter.upperBounds behavior.
        extendsList.children
            .filter { it.type != SyntaxTokenTypes.WHITE_SPACE && it.type != JavaSyntaxTokenType.AND }
            .mapNotNull { child ->
                when (child.type) {
                    JavaSyntaxElementType.TYPE -> createJavaType(child, resolutionContext) as? JavaClassifierType
                    JavaSyntaxElementType.JAVA_CODE_REFERENCE -> JavaClassifierTypeOverAst(child, resolutionContext)
                    else -> null
                }
            }
    }

    // Annotations on the type parameter declaration itself (e.g., <@NonNull T>).
    // Matches TreeBasedTypeParameter which reads tree.annotations().
    // Annotations on the type parameter declaration (e.g., <@NonNull T>).
    // The KMP parser may place annotations in a MODIFIER_LIST or directly under the
    // TYPE_PARAMETER node (no MODIFIER_LIST wrapper).
    override val annotations: Collection<JavaAnnotation>
        get() {
            val modListAnns = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
                ?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
            val directAnns = node.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, resolutionContext) }
            return modListAnns + directAnns
        }

    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val isFromSource: Boolean get() = true
}

/**
 * Represents the implicit supertype java.lang.Enum<E> for enum classes.
 * In Java, all enums implicitly extend java.lang.Enum parameterized with the enum type itself.
 */
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

    // java.lang.Enum is always resolved (it's a well-known class)
    override val isResolved: Boolean get() = true
    override fun resolve(tryResolve: (ClassId) -> Boolean, getSupertypeClassIds: ((ClassId) -> List<ClassId>)?): ClassId? {
        val classId = ClassId.topLevel(FqName(classifierQualifiedName))
        return if (tryResolve(classId)) classId else null
    }

    /**
     * The type argument for Enum<E> - represents the enum class itself.
     */
    private inner class EnumSelfTypeArgument : JavaClassifierType {
        override val classifier: JavaClassifier? get() = enumClass
        override val classifierQualifiedName: String get() = enumClass.fqName?.asString() ?: ""
        override val typeArguments: List<JavaType> get() = emptyList()
        override val isRaw: Boolean get() = false
        override val annotations: Collection<JavaAnnotation> get() = emptyList()
        override val presentableText: String get() = classifierQualifiedName
        override val isDeprecatedInJavaDoc: Boolean get() = false
        override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

        override val isResolved: Boolean get() = true
        override fun resolve(tryResolve: (ClassId) -> Boolean, getSupertypeClassIds: ((ClassId) -> List<ClassId>)?): ClassId? {
            val fqName = enumClass.fqName ?: return null
            val classId = ClassId.topLevel(fqName)
            return if (tryResolve(classId)) classId else null
        }
    }
}

/**
 * A simple classifier type for well-known external classes like java.lang.Object
 * and java.lang.annotation.Annotation. These don't have type arguments.
 */
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

    // Well-known classes are always resolved
    override val isResolved: Boolean get() = true
    override fun resolve(tryResolve: (ClassId) -> Boolean, getSupertypeClassIds: ((ClassId) -> List<ClassId>)?): ClassId? {
        val classId = ClassId.topLevel(FqName(classifierQualifiedName))
        return if (tryResolve(classId)) classId else null
    }
}
