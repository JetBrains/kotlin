/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.resolution.KtResolvable

/**
 * Represents a type reference.
 *
 * ### Example:
 *
 * ```kotlin
 * val x: String = ""
 * //     ^____^
 * ```
 */
@OptIn(KtExperimentalApi::class)
class KtTypeReference : KtModifierListOwnerStub<KotlinPlaceHolderStub<KtTypeReference>>,
    KtAnnotated, KtAnnotationsContainer, KtResolvable {

    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtTypeReference>) : super(stub, KtStubBasedElementTypes.TYPE_REFERENCE)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    /**
     * `true` if this reference is the underscore placeholder type `_` (used, for example, in partially specified
     * generic type arguments where the rest is inferred).
     */
    val isPlaceholder: Boolean
        get() = ((typeElement as? KtUserType)?.referenceExpression as? KtNameReferenceExpression)?.isPlaceholder == true

    /**
     * The actual type syntax this reference wraps (for example, a [KtUserType] or a [KtFunctionType]), stripped of
     * leading annotations and modifiers, or `null` if it is absent in incomplete code.
     */
    val typeElement: KtTypeElement?
        get() = KtStubbedPsiUtil.getStubOrPsiChild(this, KtTokenSets.TYPE_ELEMENT_TYPES, KtTypeElement.ARRAY_FACTORY)

    override fun getAnnotations(): List<KtAnnotation> {
        return modifierList?.annotations.orEmpty()
    }

    override fun getAnnotationEntries(): List<KtAnnotationEntry> {
        return modifierList?.annotationEntries.orEmpty()
    }

    /**
     * Returns `true` if this type reference is wrapped in parentheses (as in `(Int) -> String` written as
     * `((Int)) -> String`, or a parenthesized nullable function type).
     */
    fun hasParentheses(): Boolean {
        return findChildByType<PsiElement>(KtTokens.LPAR) != null && findChildByType<PsiElement>(KtTokens.RPAR) != null
    }

    /**
     * Returns the short name to use as an implicit label for a receiver of this type (the simple name of the user
     * type), or `null` if the type is not a user type.
     */
    fun nameForReceiverLabel() = (typeElement as? KtUserType)?.referencedName

    /**
     * Returns fully qualified presentable text for the underlying type based on stubs when provided.
     * No decompilation happens if [KtTypeReference] represents compiled code.
     */
    fun getTypeText(): String {
        return stub?.let { getTypeText(typeElement, ::getQualifiedName) } ?: text
    }

    /**
     * Returns short names presentable text, for `() -> kotlin.Boolean` result would be `() -> Boolean`
     * No decompilation happens if [KtTypeReference] represents compiled code.
     */
    fun getShortTypeText(): String {
        return stub?.let { getTypeText(typeElement) { it.referencedName } } ?: text
    }

    private fun getQualifiedName(userType: KtUserType): String? {
        val qualifier = userType.qualifier ?: return userType.referencedName
        return getQualifiedName(qualifier) + "." + userType.referencedName
    }

    private fun getTypeText(typeElement: KtTypeElement?, nameFunction: (KtUserType) -> String?): String? {
        return when (typeElement) {
            is KtUserType -> buildString {
                append(nameFunction(typeElement))
                val args = typeElement.typeArguments
                if (args.isNotEmpty()) {
                    append(args.joinToString(", ", "<", ">") {
                        val projection = when (it.projectionKind) {
                            KtProjectionKind.IN -> "in "
                            KtProjectionKind.OUT -> "out "
                            KtProjectionKind.STAR -> "*"
                            KtProjectionKind.NONE -> ""
                        }
                        projection + (getTypeText(it.typeReference?.typeElement, nameFunction) ?: "")
                    })
                }
            }
            is KtFunctionType -> buildString {
                if (hasModifier(KtTokens.SUSPEND_KEYWORD)) {
                    append("suspend ")
                }
                val contextReceivers = typeElement.contextReceiversTypeReferences
                if (contextReceivers.isNotEmpty()) {
                    append(contextReceivers.joinToString(", ", "context(", ")") { getTypeText(it.typeElement, nameFunction) ?: "" })
                }
                typeElement.receiverTypeReference?.let { append(getTypeText(it.typeElement, nameFunction)) }
                append(typeElement.parameters.joinToString(", ", "(", ")") { param ->
                    param.name?.let { "$it: " }.orEmpty() + getTypeText(param.typeReference?.typeElement, nameFunction).orEmpty()
                })
                typeElement.returnTypeReference?.let { returnType ->
                    append(" -> ")
                    append(getTypeText(returnType.typeElement, nameFunction))
                }
            }
            is KtIntersectionType -> getTypeText(
                typeElement.getLeftTypeRef()?.typeElement,
                nameFunction
            ) + " & " + getTypeText(typeElement.getRightTypeRef()?.typeElement, nameFunction)
            is KtNullableType -> {
                val innerType = typeElement.innerType
                buildString {
                    val parenthesisRequired = innerType is KtFunctionType
                    if (parenthesisRequired) {
                        append("(")
                    }
                    append(getTypeText(innerType, nameFunction))
                    append("?")
                    if (parenthesisRequired) {
                        append(")")
                    }
                }
            }
            is KtDynamicType -> "dynamic"
            null -> null
            else -> error("Unsupported type $typeElement")
        }
    }
}
