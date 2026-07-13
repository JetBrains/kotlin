/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.getSimpleImport
import org.jetbrains.kotlin.java.direct.resolution.resolveExternalFieldValue
import org.jetbrains.kotlin.java.direct.util.ConstantEvaluator
import org.jetbrains.kotlin.java.direct.util.computeTypeParameters
import org.jetbrains.kotlin.java.direct.util.isDeprecatedInJavaDoc
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaMemberOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    override val containingClass: JavaClassOverAst,
) : JavaElementOverAst(node, tree), JavaMember {

    protected open val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext

    override val name: Name
        get() = Name.identifier(identifierText() ?: "<error>")

    protected fun hasModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
    }

    override val isAbstract: Boolean get() = hasModifier(JavaSyntaxTokenType.ABSTRACT_KEYWORD)
    override val isStatic: Boolean get() = hasModifier(JavaSyntaxTokenType.STATIC_KEYWORD)
    override val isFinal: Boolean get() = hasModifier(JavaSyntaxTokenType.FINAL_KEYWORD)

    override val visibility: Visibility
        get() {
            // Check explicit `private` first, including on interface members:
            // Java 9+ allows `private` methods inside interfaces (they must have a body and
            // are not implicitly public). Treating every interface member as `Public` would make
            // the override-checker look for an implementation of a method that should never have
            // been visible to subclasses.
            return when {
                hasModifier(JavaSyntaxTokenType.PRIVATE_KEYWORD) -> Visibilities.Private
                containingClass.isInterface -> Visibilities.Public
                hasModifier(JavaSyntaxTokenType.PUBLIC_KEYWORD) -> Visibilities.Public
                hasModifier(JavaSyntaxTokenType.PROTECTED_KEYWORD) -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
                else -> JavaVisibilities.PackageVisibility
            }
        }

    override val annotations: Collection<JavaAnnotation>
        get() = parseAnnotationsFromModifierList(modifierList, tree, resolutionContext)

    override val isDeprecatedInJavaDoc: Boolean
        get() = isDeprecatedInJavaDoc(tree, node)

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaFieldOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass), JavaField {
    override val isEnumEntry: Boolean
        get() = tree.getType(node) == JavaSyntaxElementType.ENUM_CONSTANT

    /**
     * For multi-field declarations like `public static int A = 1, B = 2, C = 3;`,
     * the parser only attaches MODIFIER_LIST and TYPE to the first FIELD node.
     */
    private val leadingFieldNode: JavaLightNode?
        get() = computeLeadingFieldNode()

    private fun computeLeadingFieldNode(): JavaLightNode? {
        if (tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST) != null ||
            tree.findChildByType(node, JavaSyntaxElementType.TYPE) != null
        ) {
            return null
        }
        val parent = tree.getParent(node) ?: return null
        val siblings = tree.getChildren(parent)
        val myIndex = siblings.indexOfFirst { it == node }
        // Walk backwards to find the nearest FIELD sibling with a MODIFIER_LIST or TYPE
        for (i in myIndex - 1 downTo 0) {
            val sibling = siblings[i]
            if (tree.getType(sibling) == JavaSyntaxElementType.FIELD &&
                (tree.findChildByType(sibling, JavaSyntaxElementType.MODIFIER_LIST) != null ||
                        tree.findChildByType(sibling, JavaSyntaxElementType.TYPE) != null)
            ) {
                return sibling
            }
        }
        return null
    }

    override val modifierList: JavaLightNode?
        get() = tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
            ?: leadingFieldNode?.let { tree.findChildByType(it, JavaSyntaxElementType.MODIFIER_LIST) }

    private fun hasFieldModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
    }

    // Enum constants are implicitly public (JLS 8.9.3).
    // Asymmetry vs JavaMethodOverAst.visibility: fields check `isInterface` BEFORE the
    // PRIVATE_KEYWORD branch because JLS 9.3 forbids private interface fields; methods
    // check PRIVATE_KEYWORD first because Java 9+ allows private interface methods.
    override val visibility: Visibility
        get() {
            if (isEnumEntry) return Visibilities.Public
            return when {
                containingClass.isInterface -> Visibilities.Public
                hasFieldModifier(JavaSyntaxTokenType.PUBLIC_KEYWORD) -> Visibilities.Public
                hasFieldModifier(JavaSyntaxTokenType.PROTECTED_KEYWORD) -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
                hasFieldModifier(JavaSyntaxTokenType.PRIVATE_KEYWORD) -> Visibilities.Private
                else -> JavaVisibilities.PackageVisibility
            }
        }

    override val annotations: Collection<JavaAnnotation>
        get() = parseAnnotationsFromModifierList(modifierList, tree, resolutionContext)

    override val type: JavaType
        get() = computeType()

    private fun computeType(): JavaType {
        if (isEnumEntry) {
            // The constant's type is its containing enum class, already resolved.
            return ResolvedJavaClassifierType(containingClass)
        }
        // leadingFieldNode is null whenever the node has its own TYPE (see computeLeadingFieldNode),
        val typeSourceNode = leadingFieldNode ?: node
        // TYPE_USE annotations (e.g. JSpecify `@Nullable`/`@NonNull`) written in the field's
        // modifier list belong to the field type, matching PSI's `PsiField.type.annotations`.
        return createJavaTypeWithAnnotations(typeSourceNode, modifierList, tree, resolutionContext)
    }

    /**
     * The initializer expression node, if present.
     */
    private val initializerNode: JavaLightNode?
        get() {
            val children = tree.getChildren(node)
            val eqIndex = children.indexOfFirst { tree.getType(it) == JavaSyntaxTokenType.EQ }
            return if (eqIndex < 0) null
            else children.drop(eqIndex + 1).firstOrNull {
                tree.getType(it) != JavaSyntaxTokenType.SEMICOLON
            }
        }

    override val hasInitializer: Boolean
        get() = initializerNode != null

    override val hasConstantNotNullInitializer: Boolean
        get() {
            val init = initializerNode ?: return false
            if (!isFinal) return false
            val fieldType = type
            val isString = fieldType is JavaClassifierType &&
                    (fieldType.classifierQualifiedName == "java.lang.String" ||
                            fieldType.classifierQualifiedName == "String")
            if (fieldType !is JavaPrimitiveType && !isString) return false
            // Mirrors PSI's computeConstantValue() != null check: method calls, object creation,
            // etc. can never be compile-time constants per JLS 15.29.
            return isInitializerPotentiallyConstant(init)
        }

    /**
     * Returns true if the initializer expression could possibly be a JLS compile-time constant
     * expression. This is conservative: qualified references (e.g., `Foo.BAR`) are assumed
     * potentially constant even if we cannot evaluate them locally, since they might be resolved
     * via cross-language callback. Unresolvable simple names and method calls return false.
     */
    private fun isInitializerPotentiallyConstant(n: JavaLightNode): Boolean {
        return when (tree.getType(n)) {
            JavaSyntaxElementType.LITERAL_EXPRESSION -> {
                val child = tree.getChildren(n).firstOrNull()
                child != null && tree.getType(child) != JavaSyntaxTokenType.NULL_KEYWORD
            }
            JavaSyntaxElementType.BINARY_EXPRESSION -> {
                val children = tree.getChildren(n)
                children.size >= 3 &&
                        isInitializerPotentiallyConstant(children[0]) &&
                        isInitializerPotentiallyConstant(children[2])
            }
            JavaSyntaxElementType.POLYADIC_EXPRESSION -> {
                val children = tree.getChildren(n)
                var i = 0
                var result = true
                while (i < children.size) {
                    if (!isInitializerPotentiallyConstant(children[i])) {
                        result = false
                        break
                    }
                    i += 2  // skip operator token
                }
                result
            }
            JavaSyntaxElementType.PREFIX_EXPRESSION -> {
                val children = tree.getChildren(n)
                children.size >= 2 && isInitializerPotentiallyConstant(children[1])
            }
            JavaSyntaxElementType.PARENTH_EXPRESSION -> {
                val inner = tree.getChildren(n).firstOrNull {
                    val t = tree.getType(it)
                    t != JavaSyntaxTokenType.LPARENTH && t != JavaSyntaxTokenType.RPARENTH
                }
                inner != null && isInitializerPotentiallyConstant(inner)
            }
            JavaSyntaxElementType.REFERENCE_EXPRESSION -> {
                val refText = tree.getText(n).toString().trim()
                if (refText.contains('.')) {
                    true
                } else {
                    isSimpleNamePotentiallyConstant(refText)
                }
            }
            else -> false
        }
    }

    private fun isSimpleNamePotentiallyConstant(name: String): Boolean {
        val localField = containingClass.fields.find { it.name.asString() == name } as? JavaFieldOverAst
        if (localField != null) {
            return localField.isFinal
        }
        return with(containingClass.resolutionContext) { getSimpleImport(name) } != null
    }

    /**
     * Evaluates the field's initializer using the local [ConstantEvaluator]; qualified references
     * to Kotlin `const val`s (e.g. `Foo.BAR`) are routed through
     * `JavaResolutionContext.resolveExternalFieldValue`, which delegates to the session-backed
     * cross-language resolver in `JavaExternalConstResolver.kt`.
     */
    override val initializerValue: Any?
        get() {
            if (!hasConstantNotNullInitializer) return null
            val init = initializerNode ?: return null
            val evaluator = ConstantEvaluator(containingClass) { classQualifier, fieldName ->
                with(containingClass.resolutionContext) { resolveExternalFieldValue(classQualifier, fieldName) }
            }
            return coerceConstantToFieldType(evaluator.evaluate(init))
        }

    /**
     * Applies JLS 5.1 widening and 5.2 narrowing-of-constant-expression conversions so the
     * constant value matches the field's declared primitive type. Without this,
     * `public static final long T = 100;` yields an `Int 100`; FIR then stamps the
     * `FirJavaField` with `ConstantValueKind.Int`, and the IR backend emits an int push into a
     * slot read as `long` — malformed bytecode that crashes ASM's `Frame.merge` with
     * `NegativeArraySizeException`. PSI's `PsiField.computeConstantValue()` coerces implicitly.
     */
    private fun coerceConstantToFieldType(value: Any?): Any? {
        if (value == null) return null
        val primitive = (type as? JavaPrimitiveType)?.type ?: return value  // String / non-primitive — no coercion
        // else -> null = no constant for this declared primitive type; mirrors PSI.
        return when (primitive) {
            PrimitiveType.BOOLEAN -> value as? Boolean
            PrimitiveType.CHAR -> when (value) {
                is Char -> value
                is Number -> value.toInt().toChar()
                else -> null
            }
            PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT,
            PrimitiveType.LONG, PrimitiveType.FLOAT, PrimitiveType.DOUBLE -> coerceNumberOrChar(value, primitive)
        }
    }

    override val isStatic: Boolean get() = containingClass.isInterface || isEnumEntry || hasFieldModifier(JavaSyntaxTokenType.STATIC_KEYWORD)
    override val isFinal: Boolean get() = containingClass.isInterface || isEnumEntry || hasFieldModifier(JavaSyntaxTokenType.FINAL_KEYWORD)
}

abstract class JavaMethodBaseOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass) {

    // FIR matches Java type parameters by object identity; preserve identity across repeated
    // accesses on the same instance (see JavaClassCache.kt KDoc).
    val typeParameters: List<JavaTypeParameter> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computeTypeParameters(node, tree, containingClass.memberResolutionContext)
    }

    override val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext.withTypeParameters(typeParameters)

    val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.PARAMETER_LIST) ?: return emptyList()
            return tree.getChildrenByType(parameterList, JavaSyntaxElementType.PARAMETER)
                .map { JavaValueParameterOverAst(it, tree, resolutionContext) }
        }
}

class JavaMethodOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMethodBaseOverAst(node, tree, containingClass), JavaMethod {

    override val returnType: JavaType
        get() {
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE)
            return if (typeNode != null) {
                // TYPE_USE annotations appear in the method modifier list but belong to the return type
                createJavaTypeWithAnnotations(typeNode, modifierList, tree, resolutionContext)
            } else {
                JavaPrimitiveTypeOverAst(node, tree, resolutionContext)
            }
        }

    // Interface methods are abstract unless they have 'default', 'static', or 'private'
    // (Java 9+) modifiers. We must NOT use hasBody to determine abstractness — non-default
    // non-private interface methods without bodies are abstract regardless of whether a body
    // happens to be present in the AST (a stray body is a compile-time error, not our concern
    // here). This matches PSI behavior, which sets `PsiModifier.ABSTRACT` only when none of
    // `default` / `static` / `private` is present.
    override val isAbstract: Boolean
        get() = super.isAbstract || (containingClass.isInterface && !hasDefaultKeyword && !isStatic
                && !hasModifier(JavaSyntaxTokenType.PRIVATE_KEYWORD))

    private val hasDefaultKeyword: Boolean
        // DEFAULT_KEYWORD is inside MODIFIER_LIST, not a direct child of the method node
        get() = modifierList?.let { tree.hasChildOfType(it, JavaSyntaxTokenType.DEFAULT_KEYWORD) } ?: false

    override val annotationParameterDefaultValue: JavaAnnotationArgument?
        get() {
            if (!containingClass.isAnnotationType) return null

            tree.findChildByType(node, JavaSyntaxTokenType.DEFAULT_KEYWORD) ?: return null

            val children = tree.getChildren(node)
            val defaultIndex = children.indexOfFirst { tree.getType(it) == JavaSyntaxTokenType.DEFAULT_KEYWORD }
            if (defaultIndex < 0) return null

            val valueNode = children.drop(defaultIndex + 1).firstOrNull {
                tree.getType(it) != JavaSyntaxTokenType.SEMICOLON
            } ?: return null

            return createAnnotationArgumentFromValue(null, valueNode, tree, resolutionContext)
        }

    override val hasAnnotationParameterDefaultValue: Boolean
        get() = containingClass.isAnnotationType &&
                tree.findChildByType(node, JavaSyntaxTokenType.DEFAULT_KEYWORD) != null
    override val isNative: Boolean get() = hasModifier(JavaSyntaxTokenType.NATIVE_KEYWORD)
}

class JavaConstructorOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMethodBaseOverAst(node, tree, containingClass), JavaConstructor {
    override val isAbstract: Boolean get() = false
    override val isStatic: Boolean get() = false
    override val isFinal: Boolean get() = true
}

class JavaValueParameterOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node, tree), JavaValueParameter {
    override val name: Name?
        get() = identifierText()?.let { Name.identifier(it) }

    override val type: JavaType
        get() {
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE) ?: node
            return createJavaTypeWithAnnotations(typeNode, modifierList, tree, resolutionContext)
        }

    override val isVararg: Boolean
        get() = if (tree.findChildByType(node, JavaSyntaxTokenType.ELLIPSIS) != null) {
            true
        } else {
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE)
            typeNode?.let { tree.findChildByType(it, JavaSyntaxTokenType.ELLIPSIS) } != null
        }

    override val annotations: Collection<JavaAnnotation>
        get() = parseAnnotationsFromModifierList(modifierList, tree, resolutionContext)

    override val isDeprecatedInJavaDoc: Boolean
        get() = isDeprecatedInJavaDoc(tree, node)

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

// JLS 5.2 narrowing-of-constant-expression conversion for the six numeric primitive types.
// Returns null for non-Number / non-Char inputs (mirrors PSI: no constant value for this type).
private fun coerceNumberOrChar(value: Any, primitive: PrimitiveType): Any? {
    val n: Number = when (value) {
        is Number -> value
        is Char -> value.code
        else -> return null
    }
    return when (primitive) {
        PrimitiveType.BYTE -> n.toByte()
        PrimitiveType.SHORT -> n.toShort()
        PrimitiveType.INT -> n.toInt()
        PrimitiveType.LONG -> n.toLong()
        PrimitiveType.FLOAT -> n.toFloat()
        PrimitiveType.DOUBLE -> n.toDouble()
        else -> null
    }
}
