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
        get() = Name.identifier(
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() } ?: "<error>"
        )

    protected val modifierList: JavaLightNode?
        get() = tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)

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

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.let { ml ->
            tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        } ?: emptyList()

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

    /**
     * Effective modifier list: own if present, otherwise inherited from the leading field
     * in a multi-field declaration.
     */
    private val effectiveModifierList: JavaLightNode?
        get() = tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)
            ?: leadingFieldNode?.let { tree.findChildByType(it, JavaSyntaxElementType.MODIFIER_LIST) }

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

    override val annotations: Collection<JavaAnnotation>
        get() = effectiveModifierList?.let { ml ->
            tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        } ?: emptyList()

    override val type: JavaType
        get() = computeType()

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
            return if (eqIndex < 0) null
            else children.drop(eqIndex + 1).firstOrNull {
                tree.getType(it) != JavaSyntaxTokenType.SEMICOLON
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
            // Verify the initializer is a potentially constant expression form.
            // This mirrors how PSI checks computeConstantValue() != null: method calls, object
            // creation, etc. can never be compile-time constants per JLS 15.29.
            // For cross-language references (e.g., Foo.FOO from Kotlin), we conservatively return
            // true (qualified names might be resolvable via the callback in resolveInitializerValue).
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

    override val isStatic: Boolean get() = containingClass.isInterface || isEnumEntry || hasFieldModifier(JavaSyntaxTokenType.STATIC_KEYWORD)
    override val isFinal: Boolean get() = containingClass.isInterface || isEnumEntry || hasFieldModifier(JavaSyntaxTokenType.FINAL_KEYWORD)
}

class JavaMethodOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    containingClass: JavaClassOverAst,
) : JavaMemberOverAst(node, tree, containingClass), JavaMethod {

    override val typeParameters: List<JavaTypeParameter>
        get() = computeTypeParameters(node, tree, containingClass.memberResolutionContext)

    override val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext.withTypeParameters(typeParameters)

    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.PARAMETER_LIST)
            return if (parameterList != null) {
                tree.getChildrenByType(parameterList, JavaSyntaxElementType.PARAMETER)
                    .map { JavaValueParameterOverAst(it, tree, resolutionContext) }
            } else emptyList()
        }

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

    // Interface methods are abstract unless they have 'default' or 'static' keyword.
    // Note: in Java, a non-default interface method body is a compile-time error, but we still see
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
                tree.getType(it) != JavaSyntaxTokenType.SEMICOLON
            } ?: return null

            return createAnnotationArgumentFromValue(null, valueNode, tree, resolutionContext)
        }

    override val hasAnnotationParameterDefaultValue: Boolean
        get() = containingClass.isAnnotationType &&
                tree.findChildByType(node, JavaSyntaxTokenType.DEFAULT_KEYWORD) != null
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

    override val typeParameters: List<JavaTypeParameter>
        get() = computeTypeParameters(node, tree, containingClass.memberResolutionContext)

    override val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext.withTypeParameters(typeParameters)

    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.PARAMETER_LIST)
            return if (parameterList != null) {
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

    private val modifierList: JavaLightNode?
        get() = tree.findChildByType(node, JavaSyntaxElementType.MODIFIER_LIST)

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.let { ml ->
            tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                .map { JavaAnnotationOverAst(it, tree, resolutionContext) }
        } ?: emptyList()

    override val isDeprecatedInJavaDoc: Boolean
        get() = isDeprecatedInJavaDoc(tree, node)

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
    override val isFromSource: Boolean get() = true
}
