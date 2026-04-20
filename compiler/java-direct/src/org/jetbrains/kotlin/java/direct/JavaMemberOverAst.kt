/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

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
    node: JavaLightNode,
    tree: JavaLightTree,
    override val containingClass: JavaClassOverAst,
) : JavaElementOverAst(node, tree), JavaMember {

    protected open val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext

    override val name: Name
        get() = Name.identifier(
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() } ?: "<error>"
        )

    @Volatile private var _baseModifierList: Any? = NOT_COMPUTED
    private val modifierList: JavaLightNode?
        get() = cachedNullable({ _baseModifierList }, { _baseModifierList = it }) {
            tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
        }

    protected fun hasModifier(modifier: SyntaxElementType): Boolean {
        return modifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
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
        get() = cachedNonNull({ _baseAnnotations }, { _baseAnnotations = it }) {
            modifierList?.let { ml ->
                tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                    .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
            } ?: emptyList()
        }

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = tree.findChildByType(node, "DOC_COMMENT")?.let {
            tree.getText(it).toString().contains("@deprecated", ignoreCase = true)
        } == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaFieldOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass), JavaField {
    @Volatile private var _isEnumEntry: Int = -1
    override val isEnumEntry: Boolean
        get() = cachedBoolean({ _isEnumEntry }, { _isEnumEntry = it }) {
            tree.getType(node) == JavaSyntaxElementType.ENUM_CONSTANT
        }

    /**
     * For multi-field declarations like `public static int A = 1, B = 2, C = 3;`,
     * the parser only attaches MODIFIER_LIST and TYPE to the first FIELD node.
     */
    @Volatile private var _leadingFieldNode: Any? = NOT_COMPUTED
    private val leadingFieldNode: JavaLightNode?
        get() = cachedNullable({ _leadingFieldNode }, { _leadingFieldNode = it }) { computeLeadingFieldNode() }

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
        return (myIndex - 1 downTo 0)
            .map { siblings[it] }
            .firstOrNull { sibling ->
                tree.getType(sibling) == JavaSyntaxElementType.FIELD &&
                        (tree.findChildByType(sibling, JavaSyntaxElementType.MODIFIER_LIST) != null ||
                                tree.findChildByType(sibling, JavaSyntaxElementType.TYPE) != null)
            }
    }

    /**
     * Effective modifier list: own if present, otherwise inherited from the leading field
     * in a multi-field declaration.
     */
    @Volatile private var _effectiveModifierList: Any? = NOT_COMPUTED
    private val effectiveModifierList: JavaLightNode?
        get() = cachedNullable({ _effectiveModifierList }, { _effectiveModifierList = it }) {
            tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
                ?: leadingFieldNode?.let { tree.findChildByType(it, JavaSyntaxElementType.MODIFIER_LIST) }
        }

    private fun hasFieldModifier(modifier: SyntaxElementType): Boolean {
        return effectiveModifierList?.let { tree.hasChildOfType(it, modifier) } ?: false
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
        get() = cachedNonNull({ _fieldAnnotations }, { _fieldAnnotations = it }) {
            effectiveModifierList?.let { ml ->
                tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                    .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
            } ?: emptyList()
        }

    @Volatile private var _type: JavaType? = null
    override val type: JavaType
        get() = cachedNonNull({ _type }, { _type = it }) { computeType() }

    private fun computeType(): JavaType {
        if (isEnumEntry) {
            return JavaClassifierTypeForEnumEntry(containingClass)
        }
        val typeSourceNode = if (tree.findChildByType(node, JavaSyntaxElementType.TYPE) != null) node else leadingFieldNode ?: node
        return createJavaType(typeSourceNode, tree, resolutionContext)
    }

    /**
     * The initializer expression node, if present.
     */
    private val initializerNode: JavaLightNode?
        get() {
            val children = tree.getChildren(node)
            val eqIndex = children.indexOfFirst { tree.getType(it) == JavaSyntaxTokenType.EQ }
            if (eqIndex < 0) return null
            return children.drop(eqIndex + 1).firstOrNull {
                val t = tree.getType(it)
                t != SyntaxTokenTypes.WHITE_SPACE && t != JavaSyntaxTokenType.SEMICOLON
            }
        }

    override val hasConstantNotNullInitializer: Boolean
        get() {
            val init = initializerNode ?: return false
            if (!isFinal) return false
            val fieldType = type
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
    private fun isInitializerPotentiallyConstant(n: JavaLightNode): Boolean {
        return when (tree.getType(n)) {
            JavaSyntaxElementType.LITERAL_EXPRESSION -> {
                val child = tree.getChildren(n).firstOrNull()
                child?.let { tree.getType(it).toString() } != "NULL_LITERAL"
            }
            JavaSyntaxElementType.BINARY_EXPRESSION -> {
                val children = tree.getChildren(n).filter { tree.getType(it) != SyntaxTokenTypes.WHITE_SPACE }
                children.size >= 3 &&
                        isInitializerPotentiallyConstant(children[0]) &&
                        isInitializerPotentiallyConstant(children[2])
            }
            JavaSyntaxElementType.POLYADIC_EXPRESSION -> {
                val children = tree.getChildren(n).filter { tree.getType(it) != SyntaxTokenTypes.WHITE_SPACE }
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
                val children = tree.getChildren(n).filter { tree.getType(it) != SyntaxTokenTypes.WHITE_SPACE }
                children.size >= 2 && isInitializerPotentiallyConstant(children[1])
            }
            JavaSyntaxElementType.PARENTH_EXPRESSION -> {
                val inner = tree.getChildren(n).firstOrNull {
                    val t = tree.getType(it)
                    t != SyntaxTokenTypes.WHITE_SPACE && t != JavaSyntaxTokenType.LPARENTH && t != JavaSyntaxTokenType.RPARENTH
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
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass), JavaMethod {

    @Volatile private var _methodTypeParameters: List<JavaTypeParameter>? = null
    override val typeParameters: List<JavaTypeParameter>
        get() = cachedNonNull({ _methodTypeParameters }, { _methodTypeParameters = it }) {
            computeTypeParameters(node, tree, containingClass.memberResolutionContext)
        }

    @Volatile private var _methodResolutionContext: JavaResolutionContext? = null
    override val resolutionContext: JavaResolutionContext
        get() = cachedNonNull({ _methodResolutionContext }, { _methodResolutionContext = it }) {
            containingClass.memberResolutionContext.withTypeParameters(typeParameters)
        }

    @Volatile private var _methodValueParameters: List<JavaValueParameter>? = null
    override val valueParameters: List<JavaValueParameter>
        get() = cachedNonNull({ _methodValueParameters }, { _methodValueParameters = it }) {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.PARAMETER_LIST)
            if (parameterList != null) {
                tree.getChildrenByType(parameterList, JavaSyntaxElementType.PARAMETER)
                    .map { JavaValueParameterOverAst(it, tree, resolutionContext) }
            } else emptyList()
        }

    @Volatile private var _returnType: JavaType? = null
    override val returnType: JavaType
        get() = cachedNonNull({ _returnType }, { _returnType = it }) {
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE)
            if (typeNode != null) {
                // TYPE_USE annotations appear in the method modifier list but belong to the return type
                createJavaTypeWithAnnotations(typeNode, modifierList, tree, resolutionContext)
            } else {
                JavaPrimitiveTypeOverAst(node, tree, resolutionContext)
            }
        }

    @Volatile private var _methodModifierList: Any? = NOT_COMPUTED
    private val modifierList: JavaLightNode?
        get() = cachedNullable({ _methodModifierList }, { _methodModifierList = it }) {
            tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
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
        get() = modifierList?.let { tree.hasChildOfType(it, JavaSyntaxTokenType.DEFAULT_KEYWORD) } ?: false

    override val annotationParameterDefaultValue: JavaAnnotationArgument?
        get() {
            if (!containingClass.isAnnotationType) return null

            tree.findChildByType(node, JavaSyntaxTokenType.DEFAULT_KEYWORD) ?: return null

            val children = tree.getChildren(node)
            val defaultIndex = children.indexOfFirst { tree.getType(it) == JavaSyntaxTokenType.DEFAULT_KEYWORD }
            if (defaultIndex < 0) return null

            val valueNode = children.drop(defaultIndex + 1).firstOrNull {
                val t = tree.getType(it)
                t != SyntaxTokenTypes.WHITE_SPACE && t != JavaSyntaxTokenType.SEMICOLON
            } ?: return null

            return createAnnotationArgumentFromValue(null, valueNode, tree, resolutionContext)
        }

    override val hasAnnotationParameterDefaultValue: Boolean get() = annotationParameterDefaultValue != null
    override val isNative: Boolean get() = hasModifier(JavaSyntaxTokenType.NATIVE_KEYWORD)

    override val isFromSource: Boolean get() = true
}

class JavaConstructorOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass), JavaConstructor {
    override val isAbstract: Boolean get() = false
    override val isStatic: Boolean get() = false
    override val isFinal: Boolean get() = true

    @Volatile private var _ctorTypeParameters: List<JavaTypeParameter>? = null
    override val typeParameters: List<JavaTypeParameter>
        get() = cachedNonNull({ _ctorTypeParameters }, { _ctorTypeParameters = it }) {
            computeTypeParameters(node, tree, containingClass.memberResolutionContext)
        }

    @Volatile private var _ctorResolutionContext: JavaResolutionContext? = null
    override val resolutionContext: JavaResolutionContext
        get() = cachedNonNull({ _ctorResolutionContext }, { _ctorResolutionContext = it }) {
            containingClass.memberResolutionContext.withTypeParameters(typeParameters)
        }

    @Volatile private var _ctorValueParameters: List<JavaValueParameter>? = null
    override val valueParameters: List<JavaValueParameter>
        get() = cachedNonNull({ _ctorValueParameters }, { _ctorValueParameters = it }) {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.PARAMETER_LIST)
            if (parameterList != null) {
                tree.getChildrenByType(parameterList, JavaSyntaxElementType.PARAMETER)
                    .map { JavaValueParameterOverAst(it, tree, resolutionContext) }
            } else emptyList()
        }

    override val isFromSource: Boolean get() = true
}

class JavaValueParameterOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node, tree), JavaValueParameter {
    override val name: Name?
        get() = tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { Name.identifier(tree.getText(it).toString()) }

    @Volatile private var _type: JavaType? = null
    override val type: JavaType
        get() = cachedNonNull({ _type }, { _type = it }) {
            val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE) ?: node
            createJavaTypeWithAnnotations(typeNode, modifierList, tree, resolutionContext)
        }

    @Volatile private var _isVararg: Int = -1
    override val isVararg: Boolean
        get() = cachedBoolean({ _isVararg }, { _isVararg = it }) {
            if (tree.findChildByType(node, JavaSyntaxTokenType.ELLIPSIS) != null) {
                true
            } else {
                val typeNode = tree.findChildByType(node, JavaSyntaxElementType.TYPE)
                typeNode?.let { tree.findChildByType(it, JavaSyntaxTokenType.ELLIPSIS) } != null
            }
        }

    @Volatile private var _modifierList: Any? = NOT_COMPUTED
    private val modifierList: JavaLightNode?
        get() = cachedNullable({ _modifierList }, { _modifierList = it }) {
            tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
        }

    @Volatile private var _annotations: Collection<JavaAnnotation>? = null
    override val annotations: Collection<JavaAnnotation>
        get() = cachedNonNull({ _annotations }, { _annotations = it }) {
            modifierList?.let { ml ->
                tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                    .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
            } ?: emptyList()
        }

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = tree.findChildByType(node, "DOC_COMMENT")?.let {
            tree.getText(it).toString().contains("@deprecated", ignoreCase = true)
        } == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
    override val isFromSource: Boolean get() = true
}
