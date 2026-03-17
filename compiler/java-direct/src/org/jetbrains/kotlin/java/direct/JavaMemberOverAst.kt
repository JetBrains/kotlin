/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class JavaMemberOverAst(
    node: JavaSyntaxNode,
    override val containingClass: JavaClassOverAst
) : JavaElementOverAst(node), JavaMember {

    /**
     * Resolution context for this member. Includes the containing class's type parameters.
     * Subclasses (methods, constructors) may extend this with their own type parameters.
     */
    protected open val resolutionContext: JavaResolutionContext
        get() = containingClass.memberResolutionContext

    override val name: Name
        get() = Name.identifier(node.findChildByType("IDENTIFIER")?.text ?: "<error>")

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType("MODIFIER_LIST")

    protected fun hasModifier(modifier: String): Boolean {
        return modifierList?.children?.any { it.type.toString() == modifier } ?: false
    }

    override val isAbstract: Boolean get() = hasModifier("ABSTRACT_KEYWORD")
    override val isStatic: Boolean get() = hasModifier("STATIC_KEYWORD")
    override val isFinal: Boolean get() = hasModifier("FINAL_KEYWORD")

    override val visibility: Visibility
        get() = when {
            containingClass.isInterface -> Visibilities.Public
            hasModifier("PUBLIC_KEYWORD") -> Visibilities.Public
            hasModifier("PROTECTED_KEYWORD") -> if (isStatic) JavaVisibilities.ProtectedStaticVisibility else JavaVisibilities.ProtectedAndPackage
            hasModifier("PRIVATE_KEYWORD") -> Visibilities.Private
            else -> JavaVisibilities.PackageVisibility
        }

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.getChildrenByType("ANNOTATION")
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
}

class JavaFieldOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaField {
    override val isEnumEntry: Boolean get() = node.type.toString() == "ENUM_CONSTANT"

    // Enum constants are implicitly public (JLS 8.9.3)
    override val visibility: Visibility get() = if (isEnumEntry) Visibilities.Public else super.visibility

    override val type: JavaType
        get() {
            // For enum constants, the type is the containing enum class itself
            if (isEnumEntry) {
                return JavaClassifierTypeForEnumEntry(containingClass)
            }
            return createJavaType(node, resolutionContext)
        }

    /**
     * The initializer expression node, if present.
     * For a field like `static final int X = 1 + 2`, this is the `1 + 2` expression.
     */
    private val initializerNode: JavaSyntaxNode?
        get() {
            // Find EQ token and get the expression after it
            val children = node.children
            val eqIndex = children.indexOfFirst { it.type.toString() == "EQ" }
            if (eqIndex < 0) return null
            // The initializer is the next non-whitespace child after EQ
            return children.drop(eqIndex + 1).firstOrNull {
                it.type.toString() !in listOf("WHITE_SPACE", "SEMICOLON")
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
        return when (node.type.toString()) {
            "LITERAL_EXPRESSION" -> {
                val child = node.children.firstOrNull()
                child?.type.toString() != "NULL_LITERAL"
            }
            "BINARY_EXPRESSION" -> {
                val children = node.children.filter { it.type.toString() != "WHITE_SPACE" }
                // [lhs, operator, rhs] — check both operands
                children.size >= 3 &&
                    isInitializerPotentiallyConstant(children[0]) &&
                    isInitializerPotentiallyConstant(children[2])
            }
            "POLYADIC_EXPRESSION" -> {
                // Multiple operands with same operator; operands are at even indices
                val children = node.children.filter { it.type.toString() != "WHITE_SPACE" }
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
            "PREFIX_EXPRESSION" -> {
                val children = node.children.filter { it.type.toString() != "WHITE_SPACE" }
                children.size >= 2 && isInitializerPotentiallyConstant(children[1])
            }
            "PARENS_EXPRESSION" -> {
                val inner = node.children.firstOrNull {
                    it.type.toString() !in listOf("WHITE_SPACE", "LPARENTH", "RPARENTH")
                }
                inner != null && isInitializerPotentiallyConstant(inner)
            }
            "REFERENCE_EXPRESSION" -> {
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
    override val isStatic: Boolean get() = containingClass.isInterface || isEnumEntry || super.isStatic
    override val isFinal: Boolean get() = containingClass.isInterface || isEnumEntry || super.isFinal
}

class JavaMethodOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaMethod {

    override val typeParameters: List<JavaTypeParameter> by lazy {
        computeTypeParameters(node, containingClass.memberResolutionContext)
    }

    /**
     * Resolution context including both class and method type parameters.
     * Method's own type parameters shadow class type parameters with the same name.
     */
    override val resolutionContext: JavaResolutionContext by lazy {
        containingClass.memberResolutionContext.withTypeParameters(typeParameters)
    }

    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = node.findChildByType("PARAMETER_LIST") ?: return emptyList()
            return parameterList.getChildrenByType("PARAMETER")
                .map { JavaValueParameterOverAst(it, resolutionContext) }
        }

    override val returnType: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE")
                ?: return JavaPrimitiveTypeOverAst(node, resolutionContext)
            // TYPE_USE annotations appear in the method modifier list but belong to the return type
            return createJavaTypeWithAnnotations(typeNode, modifierList, resolutionContext)
        }

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType("MODIFIER_LIST")

    // Interface methods without a body are implicitly abstract
    override val isAbstract: Boolean
        get() = super.isAbstract || (containingClass.isInterface && !hasBody)

    private val hasBody: Boolean
        get() = node.findChildByType("CODE_BLOCK") != null

    override val annotationParameterDefaultValue: JavaAnnotationArgument?
        get() {
            // Only annotation interface methods can have default values
            if (!containingClass.isAnnotationType) return null

            // Look for DEFAULT_KEYWORD followed by the default value
            val defaultKeyword = node.findChildByType("DEFAULT_KEYWORD") ?: return null

            // Find the value node - it follows DEFAULT_KEYWORD in the children list
            val children = node.children
            val defaultIndex = children.indexOfFirst { it.type.toString() == "DEFAULT_KEYWORD" }
            if (defaultIndex < 0) return null

            // The value expression is the next non-whitespace child after DEFAULT_KEYWORD
            val valueNode = children.drop(defaultIndex + 1).firstOrNull {
                it.type.toString() !in listOf("WHITE_SPACE", "SEMICOLON")
            } ?: return null

            return createAnnotationArgumentFromValue(null, valueNode, resolutionContext)
        }

    override val hasAnnotationParameterDefaultValue: Boolean get() = annotationParameterDefaultValue != null
    override val isNative: Boolean get() = hasModifier("NATIVE_KEYWORD")

    override val isFromSource: Boolean get() = true
}

class JavaConstructorOverAst(
    node: JavaSyntaxNode,
    containingClass: JavaClassOverAst
) : JavaMemberOverAst(node, containingClass), JavaConstructor {
    // Constructors are never static, abstract, and are always final (can't be overridden)
    override val isAbstract: Boolean get() = false
    override val isStatic: Boolean get() = false
    override val isFinal: Boolean get() = true

    override val typeParameters: List<JavaTypeParameter> by lazy {
        computeTypeParameters(node, containingClass.memberResolutionContext)
    }

    /**
     * Resolution context including both class and constructor type parameters.
     */
    override val resolutionContext: JavaResolutionContext by lazy {
        containingClass.memberResolutionContext.withTypeParameters(typeParameters)
    }

    override val valueParameters: List<JavaValueParameter>
        get() {
            val parameterList = node.findChildByType("PARAMETER_LIST") ?: return emptyList()
            return parameterList.getChildrenByType("PARAMETER")
                .map { JavaValueParameterOverAst(it, resolutionContext) }
        }

    override val isFromSource: Boolean get() = true
}

class JavaValueParameterOverAst(
    node: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext
) : JavaElementOverAst(node), JavaValueParameter {
    override val name: Name?
        get() = node.findChildByType("IDENTIFIER")?.text?.let { Name.identifier(it) }

    override val type: JavaType
        get() {
            val typeNode = node.findChildByType("TYPE") ?: node
            return createJavaType(typeNode, resolutionContext)
        }

    override val isVararg: Boolean
        get() {
            if (node.findChildByType("ELLIPSIS") != null) return true
            val typeNode = node.findChildByType("TYPE")
            return typeNode?.findChildByType("ELLIPSIS") != null
        }

    private val modifierList: JavaSyntaxNode?
        get() = node.findChildByType("MODIFIER_LIST")

    override val annotations: Collection<JavaAnnotation>
        get() = modifierList?.getChildrenByType("ANNOTATION")
            ?.map { JavaAnnotationOverAst(it, resolutionContext) }
            ?: emptyList()

    // Javadoc @deprecated tag: DOC_COMMENT is bound as a child of the declaration node
    override val isDeprecatedInJavaDoc: Boolean
        get() = node.findChildByType("DOC_COMMENT")?.text?.contains("@deprecated", ignoreCase = true) == true

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = annotations.find { it.classId?.asSingleFqName() == fqName }
    override val isFromSource: Boolean get() = true
}
