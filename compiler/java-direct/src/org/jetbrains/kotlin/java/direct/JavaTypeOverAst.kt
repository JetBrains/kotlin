/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

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
            val modifierListAnnotations = node.findChildByType("MODIFIER_LIST")?.getChildrenByType("ANNOTATION")
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()

            val directAnnotations = node.getChildrenByType("ANNOTATION")
                .map { JavaAnnotationOverAst(it, resolutionContext) }

            return memberAnnotations + extraAnnotations + modifierListAnnotations + directAnnotations
        }

    override fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
        // Type-position annotations (from the type node itself) are TYPE_USE by syntax.
        val modifierListAnnotations = node.findChildByType("MODIFIER_LIST")?.getChildrenByType("ANNOTATION")
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()
        val directAnnotations = node.getChildrenByType("ANNOTATION")
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

    private val rawTypeName: String by lazy {
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
            when (child.type.toString()) {
                "IDENTIFIER" -> parts.add(child.text)
                "JAVA_CODE_REFERENCE" -> collectIdentifiers(child, parts)
                // Skip: ANNOTATION, REFERENCE_PARAMETER_LIST, WHITE_SPACE, DOT, etc.
            }
        }
    }

    override val classifier: JavaClassifier? by lazy {
        val parts = rawTypeName.split('.')

        // 1. Check type parameters in scope FIRST (e.g., T, E, K, V)
        if (parts.size == 1) {
            resolutionContext.findTypeParameter(parts[0])?.let { return@lazy it }
        }

        // 2. Check local classes (same compilation unit)
        var current: JavaClassifier? = resolutionContext.findLocalClass(Name.identifier(parts[0]))

        if (current is JavaClass) {
            for (i in 1 until parts.size) {
                current = (current as JavaClass).findInnerClass(Name.identifier(parts[i]))
                    ?: return@lazy null
            }
        }
        current
    }

    override val classifierQualifiedName: String
        get() {
            val parts = rawTypeName.split('.')

            // 1. Check type parameters - return name as-is (FIR handles type params specially)
            if (parts.size == 1 && resolutionContext.findTypeParameter(parts[0]) != null) {
                return rawTypeName
            }

            // 2. Check local scope (same compilation unit)
            val localBase = resolutionContext.findLocalClass(Name.identifier(parts[0]))
            if (localBase != null) {
                var current: JavaClass? = localBase
                for (i in 1 until parts.size) {
                    current = current?.findInnerClass(Name.identifier(parts[i]))
                }
                return current?.fqName?.asString() ?: rawTypeName
            }

            // 3. Check explicit single-type imports
            val qualified = resolutionContext.getSimpleImport(parts[0])
            if (qualified != null) {
                var result = qualified.asString()
                for (i in 1 until parts.size) {
                    result += "." + parts[i]
                }
                return result
            }

            // 4. Return as-is - FIR will resolve via callback (same package, star imports, java.lang types)
            return rawTypeName
        }

    override val presentableText: String get() = node.text

    override val isRaw: Boolean by lazy {
        // A type is raw if it has no type arguments but the class has type parameters.
        // Note: REFERENCE_PARAMETER_LIST may exist but be empty (no TYPE children).
        val parameterList = node.findChildByType("REFERENCE_PARAMETER_LIST")
        val hasTypeArguments = parameterList?.children?.any { it.type.toString() == "TYPE" } == true
        !hasTypeArguments && (classifier as? JavaClass)?.typeParameters?.isNotEmpty() == true
    }

    override val typeArguments: List<JavaType> by lazy {
        // Collect all REFERENCE_PARAMETER_LISTs from this node and nested JAVA_CODE_REFERENCEs.
        // This handles both flat ("A<T>.B<U>" → [<T>, <U>] as direct children) and
        // nested ("A<T>.B<U>" → child JAVA_CODE_REF("A<T>") + sibling REFPARAMLIST(<U>)) structures.
        val allRefParamLists = collectAllRefParamLists(node)

        // The innermost class's explicit type arguments come from the LAST REFERENCE_PARAMETER_LIST.
        val explicitArgs = allRefParamLists.lastOrNull()
            ?.children
            ?.filter { it.type.toString() == "TYPE" }
            ?.map { typeNode -> createJavaType(typeNode, resolutionContext) }
            ?: emptyList()

        // For qualified generic types like "BaseOuter<H>.BaseInner<Double, String>", the earlier
        // REFERENCE_PARAMETER_LISTs contain explicit type arguments for the outer classes.
        // These are used directly instead of implicit outer type params — for cross-file types
        // (classifier == null) the source-level outer args are the only information available.
        if (allRefParamLists.size > 1) {
            val outerExplicitArgs = allRefParamLists.dropLast(1).reversed().flatMap { paramList ->
                paramList.children.filter { it.type.toString() == "TYPE" }
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
            when (child.type.toString()) {
                "JAVA_CODE_REFERENCE" -> result.addAll(collectAllRefParamLists(child))
                "REFERENCE_PARAMETER_LIST" -> result.add(child)
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

    override fun resolve(tryResolve: (ClassId) -> Boolean): ClassId? {
        return resolutionContext.resolve(rawTypeName, tryResolve)
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
    override fun resolve(tryResolve: (ClassId) -> Boolean): ClassId? {
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
    if (node.type.toString() == "TYPE") {
        val hasArrayBracket = node.findChildByType("LBRACKET") != null
        val hasVarargEllipsis = node.findChildByType("ELLIPSIS") != null
        if (hasArrayBracket || hasVarargEllipsis) {
            val componentTypeNode = node.findChildByType("TYPE")
            if (componentTypeNode != null) {
                val componentType = createJavaType(componentTypeNode, resolutionContext)
                return JavaArrayTypeOverAst(node, resolutionContext, componentType, extraAnnotations, memberAnnotations)
            }
        }

        // Wildcard type: TYPE contains QUEST (the '?'), optionally with EXTENDS_KEYWORD or SUPER_KEYWORD
        // AST structure: TYPE -> [QUEST, (EXTENDS_KEYWORD|SUPER_KEYWORD)?, TYPE?]
        // Must check on the input TYPE node BEFORE looking for nested TYPE (which would be the bound type)
        if (node.findChildByType("QUEST") != null) {
            val hasSuper = node.findChildByType("SUPER_KEYWORD") != null
            val boundTypeNode = node.findChildByType("TYPE")
            val bound = boundTypeNode?.let { createJavaType(it, resolutionContext) }
            val isExtends = !hasSuper
            return JavaWildcardTypeOverAst(node, resolutionContext, bound, isExtends, extraAnnotations, memberAnnotations)
        }
    }

    val typeNode = node.findChildByType("TYPE") ?: node

    // Also check for wildcard on the derived typeNode (for non-TYPE input nodes)
    if (typeNode.findChildByType("QUEST") != null) {
        val hasSuper = typeNode.findChildByType("SUPER_KEYWORD") != null
        val boundTypeNode = typeNode.findChildByType("TYPE")
        val bound = boundTypeNode?.let { createJavaType(it, resolutionContext) }
        val isExtends = !hasSuper
        return JavaWildcardTypeOverAst(typeNode, resolutionContext, bound, isExtends, extraAnnotations, memberAnnotations)
    }

    // Array type or vararg: TYPE contains nested TYPE + LBRACKET/RBRACKET or ELLIPSIS
    val hasArrayBracket = typeNode.findChildByType("LBRACKET") != null
    val hasVarargEllipsis = typeNode.findChildByType("ELLIPSIS") != null
    if (hasArrayBracket || hasVarargEllipsis) {
        val componentTypeNode = typeNode.findChildByType("TYPE")
        if (componentTypeNode != null) {
            val componentType = createJavaType(componentTypeNode, resolutionContext)
            return JavaArrayTypeOverAst(typeNode, resolutionContext, componentType, extraAnnotations, memberAnnotations)
        }
    }

    val primitiveNode = typeNode.children.find { it.type.toString().endsWith("_KEYWORD") }
    if (primitiveNode != null) {
        return JavaPrimitiveTypeOverAst(primitiveNode, resolutionContext, extraAnnotations, memberAnnotations)
    }

    val referenceNode = typeNode.findChildByType("JAVA_CODE_REFERENCE")
    if (referenceNode != null) {
        // TYPE_USE annotations on type arguments appear directly under the TYPE node (not in MODIFIER_LIST)
        // Extract them here and pass as extraAnnotations since we're using JAVA_CODE_REFERENCE as the node
        val typeNodeAnnotations = typeNode.getChildrenByType("ANNOTATION")
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
    val memberAnnotations = modifierList?.getChildrenByType("ANNOTATION")
        ?.map { JavaAnnotationOverAst(it, resolutionContext) }
        ?: emptyList()
    return createJavaType(typeNode, resolutionContext, memberAnnotations = memberAnnotations)
}

class JavaTypeParameterOverAst(
    node: JavaSyntaxNode,
    initialResolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaTypeParameter {

    // Resolution context can be updated after construction to include all sibling type parameters.
    // This is needed for resolving bounds like `S extends JsStubElement<E>` where E is another type param.
    private var resolutionContext: JavaResolutionContext = initialResolutionContext

    /**
     * Updates the resolution context used for resolving upper bounds.
     * Called after all type parameters are created to add them all to scope.
     */
    fun updateResolutionContext(newContext: JavaResolutionContext) {
        resolutionContext = newContext
    }

    override val name: Name
        get() = Name.identifier(node.findChildByType("IDENTIFIER")?.text ?: "<error>")

    override val upperBounds: Collection<JavaClassifierType> by lazy {
        val extendsList = node.findChildByType("EXTENDS_BOUND_LIST") ?: return@lazy emptyList()
        // Bounds may be TYPE nodes (with annotations like "T extends @NotNull Object") or bare
        // JAVA_CODE_REFERENCE nodes. Matches TreeBasedTypeParameter.upperBounds behavior.
        extendsList.children
            .filter { it.type.toString() !in listOf("WHITE_SPACE", "AND") }
            .mapNotNull { child ->
                when (child.type.toString()) {
                    "TYPE" -> createJavaType(child, resolutionContext) as? JavaClassifierType
                    "JAVA_CODE_REFERENCE" -> JavaClassifierTypeOverAst(child, resolutionContext)
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
            val modListAnns = node.findChildByType("MODIFIER_LIST")
                ?.getChildrenByType("ANNOTATION")
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
            val directAnns = node.getChildrenByType("ANNOTATION")
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
    override fun resolve(tryResolve: (ClassId) -> Boolean): ClassId? {
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
        override fun resolve(tryResolve: (ClassId) -> Boolean): ClassId? {
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
    override fun resolve(tryResolve: (ClassId) -> Boolean): ClassId? {
        val classId = ClassId.topLevel(FqName(classifierQualifiedName))
        return if (tryResolve(classId)) classId else null
    }
}
