/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub

/**
 * Represents a property getter or setter accessor.
 *
 * ### Example:
 *
 * ```kotlin
 * val x: Int
 *     get() = 2
 * //  ^_______^
 * ```
 *
 * Note: this class is not intended to be extended and is marked `open` solely for backward compatibility.
 */
open class KtPropertyAccessor : KtDeclarationStub<KotlinPropertyAccessorStub>, KtDeclarationWithBody, KtModifierListOwner,
    KtDeclarationWithInitializer, KtDeclarationWithReturnType {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPropertyAccessorStub) : super(stub, KtStubBasedElementTypes.PROPERTY_ACCESSOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccessor(this, data)

    /**
     * `true` if this accessor is a getter (`get`).
     */
    open val isGetter: Boolean
        get() {
            greenStub?.let {
                return it.isGetter
            }
            return findChildByType<PsiElement>(KtTokens.GET_KEYWORD) != null
        }

    /**
     * `true` if this accessor is a setter (`set`).
     */
    open val isSetter: Boolean
        get() {
            greenStub?.let {
                return !it.isGetter
            }
            return findChildByType<PsiElement>(KtTokens.SET_KEYWORD) != null
        }

    /**
     * The parenthesized parameter list of the accessor, or `null` if there is none (a getter, or a setter written
     * without an explicit parameter).
     */
    open val parameterList: KtParameterList?
        get() =
            @Suppress("DEPRECATION") // KT-78356
            getStubOrPsiChild(KtStubBasedElementTypes.VALUE_PARAMETER_LIST)

    /**
     * The single parameter of a setter (the new value), or `null` for a getter or when it is absent.
     */
    open val parameter: KtParameter?
        get() = parameterList?.parameters?.firstOrNull()

    override fun getValueParameters(): List<KtParameter> =
        listOfNotNull(parameter)

    override fun getBodyExpression(): KtExpression? {
        greenStub?.let {
            if (!it.hasBody) return null
        }

        return findChildByClass(KtExpression::class.java)
    }

    override fun getBodyBlockExpression(): KtBlockExpression? {
        greenStub?.let {
            if (!(it.hasNoExpressionBody && it.hasBody)) return null
        }

        return findChildByClass(KtExpression::class.java) as? KtBlockExpression
    }

    override fun hasBlockBody(): Boolean {
        greenStub?.let {
            return it.hasNoExpressionBody
        }
        return equalsToken == null
    }

    override fun hasBody(): Boolean {
        greenStub?.let {
            return it.hasBody
        }
        return getBodyExpression() != null
    }

    override fun getEqualsToken(): PsiElement? =
        findChildByType(KtTokens.EQ)

    override fun getContractDescription(): KtContractEffectList? =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.CONTRACT_EFFECT_LIST)

    /** Always `true`: an accessor's return type is always known (it is the property's type). */
    override fun hasDeclaredReturnType(): Boolean = true

    override fun getTypeReference(): KtTypeReference? =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE)

    /**
     * The `get` or `set` keyword, which stands in for the accessor's name (an accessor has no name of its own).
     */
    open val namePlaceholder: PsiElement
        get() = findChildByType(KtTokens.GET_KEYWORD) ?: findChildByType(KtTokens.SET_KEYWORD)!!

    override fun getInitializer(): KtExpression? =
        PsiTreeUtil.getNextSiblingOfType(equalsToken, KtExpression::class.java)

    override fun hasInitializer(): Boolean =
        initializer != null

    /**
     * The property this accessor belongs to.
     */
    open val property: KtProperty
        get() = parent as KtProperty

    override fun getTextOffset(): Int =
        namePlaceholder.textRange.startOffset

    @OptIn(KtImplementationDetail::class)
    override fun mayHaveContract(): Boolean {
        greenStub?.let {
            return it.mayHaveContract
        }
        return isLegacyContractPresentPsiCheck()
    }

    @Suppress("unused")
    @Deprecated("Use typeReference instead", ReplaceWith("typeReference"))
    open val returnTypeReference: KtTypeReference?
        get() = typeReference

    @Suppress("unused")
    @Deprecated("use `parameterList?.leftParenthesis`", ReplaceWith("parameterList?.leftParenthesis"))
    open val leftParenthesis: PsiElement?
        get() = parameterList?.leftParenthesis

    @Suppress("unused")
    @Deprecated("use `parameterList?.rightParenthesis`", ReplaceWith("parameterList?.rightParenthesis"))
    open val rightParenthesis: PsiElement?
        get() = parameterList?.rightParenthesis
}
