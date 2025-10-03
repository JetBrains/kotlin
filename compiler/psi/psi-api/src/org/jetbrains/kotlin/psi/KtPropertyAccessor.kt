/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * Note: this class is not intended to be extended and is marked `open` solely for backward compatibility.
 */
open class KtPropertyAccessor : KtDeclarationStub<KotlinPropertyAccessorStub>, KtDeclarationWithBody, KtModifierListOwner,
    KtDeclarationWithInitializer, KtDeclarationWithReturnType {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPropertyAccessorStub) : super(stub, KtStubBasedElementTypes.PROPERTY_ACCESSOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R =
        visitor.visitPropertyAccessor(this, data)

    open val isGetter: Boolean
        get() {
            greenStub?.let {
                return it.isGetter
            }
            return findChildByType<PsiElement>(KtTokens.GET_KEYWORD) != null
        }

    open val isSetter: Boolean
        get() {
            greenStub?.let {
                return !it.isGetter
            }
            return findChildByType<PsiElement>(KtTokens.SET_KEYWORD) != null
        }

    open val parameterList: KtParameterList?
        get() = getStubOrPsiChild(KtStubBasedElementTypes.VALUE_PARAMETER_LIST)

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
        getStubOrPsiChild(KtStubBasedElementTypes.CONTRACT_EFFECT_LIST)

    override fun hasDeclaredReturnType(): Boolean = true

    override fun getTypeReference(): KtTypeReference? =
        getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE)

    open val namePlaceholder: PsiElement
        get() = findChildByType(KtTokens.GET_KEYWORD) ?: findChildByType(KtTokens.SET_KEYWORD)!!

    override fun getInitializer(): KtExpression? =
        PsiTreeUtil.getNextSiblingOfType(equalsToken, KtExpression::class.java)

    override fun hasInitializer(): Boolean =
        initializer != null

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
