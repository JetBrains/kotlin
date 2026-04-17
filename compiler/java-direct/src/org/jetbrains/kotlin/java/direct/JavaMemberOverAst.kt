/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaMemberOverAst(
    node: JavaSyntaxNode,
    override val containingClass: JavaClassOverAst,
) : JavaElementOverAst(node), JavaMember {

    /**
     * Resolution context for this member. Includes the containing class's type parameters.
     * Subclasses (methods, constructors) may extend this with their own type parameters.
     */
    protected open val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext

    override val name: Name
        get() = Name.identifier(node.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text ?: "<error>")

    // Performance: manual @Volatile fields replace `by lazy(PUBLICATION)` delegates.
    // JavaMethodOverAst (~50K instances) and JavaFieldOverAst (~30K instances) each had
    // multiple delegates at ~32 bytes each; this saves ~13 MB total in large projects.

    @Volatile private var _baseModifierList: Any? = NOT_COMPUTED
    private val modifierList: JavaSyntaxNode?
        get() {
            val cached = _baseModifierList
            if (cached !== NOT_COMPUTED) return cached as JavaSyntaxNode?
            val computed = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
            _baseModifierList = computed
            return computed
        }

    protected fun hasModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.children?.any { it.type == modifier } ?: false
    }

    override val isAbstract: Boolean get() = hasModifier(JavaSyntaxTokenType.ABSTRACT_KEYWORD)
    override val isStatic: Boolean get() = hasModifier(JavaSyntaxTokenType.STATIC_KEYWORD)
    override val isFinal: Boolean get() = hasModifier(JavaSyntaxTokenType.FINAL_KEYWORD)

    override val visibility: Visibility
        get() {
            return when {
                containingClass.isInterface -> Visibilities.Public
                hasModifier(JavaSyntaxTokenType.PUBLIC_KEYWORD) -> Visibilities.Public
                hasModifier(JavaSyntaxTokenType.PROTECTED_KEYWORD) -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
                hasModifier(JavaSyntaxTokenType.PRIVATE_KEYWORD) -> Visibilities.Private
                else -> JavaVisibilities.PackageVisibility
            }
        }

    @Volatile private var _baseAnnotations: Collection<JavaAnnotation>? = null
    override val annotations: Collection<JavaAnnotation>
        get() {
            _baseAnnotations?.let { return it }
            val computed = modifierList?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
            _baseAnnotations = computed
            return computed
        }

    protected companion object {
        /** Sentinel for @Volatile nullable properties: distinguishes "not yet computed" from "computed as null". */
        @JvmField val NOT_COMPUTED: Any = Any()
    }

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaFieldOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, containingClass), JavaField {
    @Volatile private var _isEnumEntry: Int = -1
    override val isEnumEntry: Boolean
        get() {
            val cached = _isEnumEntry
            if (cached >= 0) return cached != 0
            val computed = node.type == JavaSyntaxElementType.ENUM_CONSTANT
            _isEnumEntry = if (computed) 1 else 0
            return computed
        }

    /**
     * For multi-field declarations like `public static int A = 1, B = 2, C = 3;`,
     * the parser only attaches MODIFIER_LIST and TYPE to the first FIELD node.
     * Subsequent fields (B, C) have no MODIFIER_LIST or TYPE of their own.
     * This property finds the leading FIELD sibling that carries the shared modifiers/type.
     */
    @Volatile private var _leadingFieldNode: Any? = NOT_COMPUTED
    private val leadingFieldNode: JavaSyntaxNode?
        get() {
            val cached = _leadingFieldNode
            if (cached !== NOT_COMPUTED) return cached as JavaSyntaxNode?
            val computed = computeLeadingFieldNode()
            _leadingFieldNode = computed
            return computed
        }

    private fun computeLeadingFieldNode(): JavaSyntaxNode? {
        if (node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST) != null || node.findChildByType(JavaSyntaxElementType.TYPE) != null) {
            return null // this node already has its own modifiers/type
        }
        val parent = node.parent ?: return null
        val siblings = parent.children
        val myIndex = siblings.indexOf(node)
        // Walk backwards to find the nearest FIELD sibling with a MODIFIER_LIST or TYPE
        return (myIndex - 1 downTo 0)
            .map { siblings[it] }
            .firstOrNull {
                it.type == JavaSyntaxElementType.FIELD && (it.findChildByType(JavaSyntaxElementType.MODIFIER_LIST) != null || it.findChildByType(
                    JavaSyntaxElementType.TYPE
                ) != null)
            }
    }

    /**
     * Effective modifier list: own if present, otherwise inherited from the leading field
     * in a multi-field declaration.
     */
    @Volatile private var _effectiveModifierList: Any? = NOT_COMPUTED
    private val effectiveModifierList: JavaSyntaxNode?
        get() {
            val cached = _effectiveModifierList
            if (cached !== NOT_COMPUTED) return cached as JavaSyntaxNode?
            val computed = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST) ?: leadingFieldNode?.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
            _effectiveModifierList = computed
            return computed
        }

    private fun hasFieldModifier(modifier: SyntaxElementType): Boolean {
        return effectiveModifierList?.children?.any { it.type == modifier } ?: false
    }

    // Enum constants are implicitly public (JLS 8.9.3)
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

    @Volatile private var _fieldAnnotations: Collection<JavaAnnotation>? = null
    override val annotations: Collection<JavaAnnotation>
        get() {
            _fieldAnnotations?.let { return it }
            val computed = effectiveModifierList?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
                ?.map { JavaAnnotationOverAst(it, resolutionContext) }
                ?: emptyList()
            _fieldAnnotations = computed
            return computed
        }

    @Volatile private var _type: JavaType? = null
    override val type: JavaType
        get() {
            _type?.let { return it }
            val computed = computeType()
            _type = computed
            return computed
        }

    private fun computeType(): JavaType {
        // For enum constants, the type is the containing enum class itself
        if (isEnumEntry) {
            return JavaClassifierTypeForEnumEntry(containingClass)
        }
        // For multi-field declarations, the TYPE node is on the leading field
        val typeSourceNode = if (node.findChildByType(JavaSyntaxElementType.TYPE) != null) node else leadingFieldNode ?: node
        return createJavaType(typeSourceNode, resolutionContext)
    }

    /**
     * The initializer expression node, if present.
     * For a field like `static final int X = 1 + 2`, this is the `1 + 2` expression.
     */
    private val initializerNode: JavaSyntaxNode?
        get() {
            // Find EQ token and get the expression after it
            val children = node.children
            val eqIndex = children.indexOfFirst { it.type == JavaSyntaxTokenType.EQ }
            if (eqIndex < 0) return null
            // The initializer is the next non-whitespace child after EQ
            return children.drop(eqIndex + 1).firstOrNull {
                it.type != SyntaxTokenTypes.WHITE_SPACE && it.type != JavaSyntaxTokenType.SEMICOLON
            }
        }

    override val hasConstantNotNullInitializer: Boolean
        get() {
            val init = initializerNode ?: return false
            // Must be final and have a primitive or String type
            if (!isFinal) return false
            val fieldType = type
            // For String, check both resolved "java.lang.String" and unresolved "String"
            // (which implicitly refers to java.lang.String)
            val isString = fieldType is JavaClassifierType &&
                    (fieldType.classifierQualifiedName == "java.lang.String" ||
                            fieldType.classifierQualifiedName == "String")
            if (fieldType !is JavaPrimitiveType && !isString) return false
            // Verify the initializer is a potentially-constant expression form.
            // This mirrors how PSI checks computeConstantValue() != null: method calls, object
            // creation, etc. can never be compile-time constants per JLS 15.29.
            // For cross-language references (e.g., Foo.FOO from Kotlin), we conservatively return
            // true (qualified names might be resolvable via the callback in resolveInitializerValue).
            return isInitializerPotentiallyConstant(init)
        }

    /**
     * Returns true if the initializer expression could possibly be a JLS compile-time constant
     * expression. This is conservative: qualified references (e.g., Foo.BAR) are assumed
     * potentially constant even if we cannot evaluate them locally, since they might be resolved
     * via cross-language callback. Unresolvable simple names and method calls return false.
     */
    private fun isInitializerPotentiallyConstant(node: JavaSyntaxNode): Boolean {
        return when (node.type) {
            JavaSyntaxElementType.LITERAL_EXPRESSION -> {
                val child = node.children.firstOrNull()
                child?.type.toString() != "NULL_LITERAL"
            }
            JavaSyntaxElementType.BINARY_EXPRESSION -> {
                val children = node.children.filter { it.type != SyntaxTokenTypes.WHITE_SPACE }
                // [lhs, operator, rhs] — check both operands
                children.size >= 3 &&
                        isInitializerPotentiallyConstant(children[0]) &&
                        isInitializerPotentiallyConstant(children[2])
            }
            JavaSyntaxElementType.POLYADIC_EXPRESSION -> {
                // Multiple operands with same operator; operands are at even indices
                val children = node.children.filter { it.type != SyntaxTokenTypes.WHITE_SPACE }
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
                val children = node.children.filter { it.type != SyntaxTokenTypes.WHITE_SPACE }
                children.size >= 2 && isInitializerPotentiallyConstant(children[1])
            }
            JavaSyntaxElementType.PARENTH_EXPRESSION -> {
                val inner = node.children.firstOrNull {
                    it.type != SyntaxTokenTypes.WHITE_SPACE && it.type != JavaSyntaxTokenType.LPARENTH && it.type != JavaSyntaxTokenType.RPARENTH
                }
                inner != null && isInitializerPotentiallyConstant(inner)
            }
            JavaSyntaxElementType.REFERENCE_EXPRESSION -> {
                val refText = node.text.trim()
                if (refText.contains('.')) {
                    // Qualified reference (e.g., MainKt.FOO, Foo.BAR) — might be an external
                    // constant resolvable via the cross-language callback
                    true
                } else {
                    // Simple name — must be resolvable as a constant in local scope
                    isSimpleNamePotentiallyConstant(refText)
                }
            }
            else -> false  // method calls, new expressions, etc. are never JLS constants
        }
    }

    private fun isSimpleNamePotentiallyConstant(name: String): Boolean {
        // Check if the name refers to a field in the same class
        val localField = containingClass.fields.find { it.name.asString() == name } as? JavaFieldOverAst
        if (localField != null) {
            // A field reference is only potentially constant if the field itself is final
            return localField.isFinal
        }
        // Not in the same class — check if it's a known single-type import (e.g., static import)
        return containingClass.resolutionContext.getSimpleImport(name) != null
    }

    override val initializerValue: Any?
        get() {
            if (!hasConstantNotNullInitializer) return null
            val init = initializerNode ?: return null
            return ConstantEvaluator(containingClass).evaluate(init)
        }

    override fun resolveInitializerValue(resolveReference: (classQualifier: String?, fieldName: String) -> Any?): Any? {
        if (!hasConstantNotNullInitializer) return null
        val init = initializerNode ?: return null
        return ConstantEvaluator(containingClass, resolveReference).evaluate(init)
    }

    override val isFromSource: Boolean get() = true

    // Interface fields are implicitly public static final
    // Enum constants are also implicitly public static final
    // For multi-field declarations, hasFieldModifier checks the effective (possibly inherited) modifier list
    override val isStatic: Boolean get() = containingClass.isInterface || isEnumEntry || hasFieldModifier(JavaSyntaxTokenType.STATIC_KEYWORD)
    override val isFinal: Boolean get() = containingClass.isInterface || isEnumEntry || hasFieldModifier(JavaSyntaxTokenType.FINAL_KEYWORD)
}

class JavaMethodOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, containingClass), JavaMethod {

    @Volatile private var _methodTypeParameters: List<JavaTypeParameter>? = null
    override val typeParameters: List<JavaTypeParameter>
        get() {
            _methodTypeParameters?.let { return it }
            val computed = computeTypeParameters(node, containingClass.memberResolutionContext)
            _methodTypeParameters = computed
            return computed
        }

    /**
     * Resolution context including both class and method type parameters.
     * Method's own type parameters shadow class type parameters with the same name.
     */
    @Volatile private var _methodResolutionContext: JavaResolutionContext? = null
    override val resolutionContext: JavaResolutionContext
        get() {
            _methodResolutionContext?.let { return it }
            val computed = containingClass.memberResolutionContext.withTypeParameters(typeParameters)
            _methodResolutionContext = computed
            return computed
        }

    @Volatile private var _methodValueParameters: List<JavaValueParameter>? = null
    override val valueParameters: List<JavaValueParameter>
        get() {
            _methodValueParameters?.let { return it }
            val parameterList = node.findChildByType(JavaSyntaxElementType.PARAMETER_LIST)
            val computed = if (parameterList != null) {
                parameterList.getChildrenByType(JavaSyntaxElementType.PARAMETER)
                    .map { JavaValueParameterOverAst(it, resolutionContext) }
            } else emptyList()
            _methodValueParameters = computed
            return computed
        }

    @Volatile private var _returnType: JavaType? = null
    override val returnType: JavaType
        get() {
            _returnType?.let { return it }
            val typeNode = node.findChildByType(JavaSyntaxElementType.TYPE)
            val computed = if (typeNode != null) {
                // TYPE_USE annotations appear in the method modifier list but belong to the return type
                createJavaTypeWithAnnotations(typeNode, modifierList, resolutionContext)
            } else {
                JavaPrimitiveTypeOverAst(node, resolutionContext)
            }
            _returnType = computed
            return computed
        }

    @Volatile private var _methodModifierList: Any? = NOT_COMPUTED
    private val modifierList: JavaSyntaxNode?
        get() {
            val cached = _methodModifierList
            if (cached !== NOT_COMPUTED) return cached as JavaSyntaxNode?
            val computed = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)
            _methodModifierList = computed
            return computed
        }

    // Interface methods are abstract unless they have 'default' or 'static' keyword.
    // Note: in Java, a non-default interface method body is a compile error, but we still see
    // the body in the AST. We must NOT use hasBody to determine abstractness — interface
    // methods without 'default' are always abstract regardless of whether a body is present.
    // This matches PSI behavior which only checks for explicit 'default'/'static' keywords.
    override val isAbstract: Boolean
        get() = super.isAbstract || (containingClass.isInterface && !hasDefaultKeyword && !isStatic)

    private val hasDefaultKeyword: Boolean
        // DEFAULT_KEYWORD is inside MODIFIER_LIST, not a direct child of the method node
        get() = modifierList?.children?.any { it.type == JavaSyntaxTokenType.DEFAULT_KEYWORD } ?: false

    override val annotationParameterDefaultValue: JavaAnnotationArgument?
        get() {
            // Only annotation interface methods can have default values
            if (!containingClass.isAnnotationType) return null

            // Look for DEFAULT_KEYWORD followed by the default value
            val defaultKeyword = node.findChildByType(JavaSyntaxTokenType.DEFAULT_KEYWORD) ?: return null

            // Find the value node - it follows DEFAULT_KEYWORD in the children list
            val children = node.children
            val defaultIndex = children.indexOfFirst { it.type == JavaSyntaxTokenType.DEFAULT_KEYWORD }
            if (defaultIndex < 0) return null

            // The value expression is the next non-whitespace child after DEFAULT_KEYWORD
            val valueNode = children.drop(defaultIndex + 1).firstOrNull {
                it.type != SyntaxTokenTypes.WHITE_SPACE && it.type != JavaSyntaxTokenType.SEMICOLON
            } ?: return null

            return createAnnotationArgumentFromValue(null, valueNode, resolutionContext)
        }

    override val hasAnnotationParameterDefaultValue: Boolean get() = annotationParameterDefaultValue != null
    override val isNative: Boolean get() = hasModifier(JavaSyntaxTokenType.NATIVE_KEYWORD)

    override val isFromSource: Boolean get() = true
}

class JavaConstructorOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, containingClass), JavaConstructor {
    // Constructors are never static, abstract, and are always final (can't be overridden)
    override val isAbstract: Boolean get() = false
    override val isStatic: Boolean get() = false
    override val isFinal: Boolean get() = true

    @Volatile private var _ctorTypeParameters: List<JavaTypeParameter>? = null
    override val typeParameters: List<JavaTypeParameter>
        get() {
            _ctorTypeParameters?.let { return it }
            val computed = computeTypeParameters(node, containingClass.memberResolutionContext)
            _ctorTypeParameters = computed
            return computed
        }

    /**
     * Resolution context including both class and constructor type parameters.
     */
    @Volatile private var _ctorResolutionContext: JavaResolutionContext? = null
    override val resolutionContext: JavaResolutionContext
        get() {
            _ctorResolutionContext?.let { return it }
            val computed = containingClass.memberResolutionContext.withTypeParameters(typeParameters)
            _ctorResolutionContext = computed
            return computed
        }

    @Volatile private var _ctorValueParameters: List<JavaValueParameter>? = null
    override val valueParameters: List<JavaValueParameter>
        get() {
            _ctorValueParameters?.let { return it }
            val parameterList = node.findChildByType(JavaSyntaxElementType.PARAMETER_LIST)
            val computed = if (parameterList != null) {
                parameterList.getChildrenByType(JavaSyntaxElementType.PARAMETER)
                    .map { JavaValueParameterOverAst(it, resolutionContext) }
            } else emptyList()
            _ctorValueParameters = computed
            return computed
        }

    override val isFromSource: Boolean get() = true
}

class JavaValueParameterOverAst(
    node: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaValueParameter {
    override val name: Name?
        get() = node.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text?.let { Name.identifier(it) }

    override val type: JavaType
        get() {
            val typeNode = node.findChildByType(JavaSyntaxElementType.TYPE) ?: node
            // Pass modifier list annotations as extra annotations to the type.
            // Matches TreeBasedValueParameter which passes annotations to TreeBasedType.create().
            return createJavaTypeWithAnnotations(typeNode, modifierList, resolutionContext)
        }

    override val isVararg: Boolean
        get() {
            if (node.findChildByType(JavaSyntaxTokenType.ELLIPSIS) != null) return true
            val typeNode = node.findChildByType(JavaSyntaxElementType.TYPE)
            return typeNode?.findChildByType(JavaSyntaxTokenType.ELLIPSIS) != null
        }

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
    override val isFromSource: Boolean get() = true
}
