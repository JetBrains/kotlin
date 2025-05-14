/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtConstructorElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

abstract class KtConstructor<T : KtConstructor<T>> : KtDeclarationStub<KotlinConstructorStub<T>>, KtFunction {
    protected constructor(node: ASTNode) : super(node)
    protected constructor(stub: KotlinConstructorStub<T>, nodeType: KtConstructorElementType<T>) : super(stub, nodeType)

    abstract fun getContainingClassOrObject(): KtClassOrObject

    override fun isLocal() = false

    override fun getValueParameterList() = getStubOrPsiChild(KtStubElementTypes.VALUE_PARAMETER_LIST)

    override fun getValueParameters() = valueParameterList?.parameters ?: emptyList()

    override fun getReceiverTypeReference() = null

    override fun getStaticReceiverType(): KtUserType? = null

    override fun getTypeReference() = null

    @Throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: KtTypeReference?) = throw IncorrectOperationException("setTypeReference to constructor")

    override fun getColon() = findChildByType<PsiElement>(KtTokens.COLON)

    override fun getBodyExpression(): KtBlockExpression? = null

    override fun getEqualsToken() = null

    override fun hasBlockBody() = hasBody()

    fun isDelegatedCallToThis(): Boolean {
        greenStub?.let { return it.isDelegatedCallToThis() }
        return when (this) {
            is KtPrimaryConstructor -> false
            is KtSecondaryConstructor -> getDelegationCallOrNull()?.isCallToThis() ?: true
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    fun isExplicitDelegationCall(): Boolean {
        greenStub?.let { return it.isExplicitDelegationCall() }
        return when (this) {
            is KtPrimaryConstructor -> false
            is KtSecondaryConstructor -> getDelegationCallOrNull()?.isImplicit == false
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    override fun hasBody(): Boolean {
        greenStub?.let { return it.hasBody() }
        return bodyExpression != null
    }

    override fun hasDeclaredReturnType() = false

    override fun getTypeParameterList() = null

    override fun getTypeConstraintList() = null

    override fun getTypeConstraints() = emptyList<KtTypeConstraint>()

    override fun getTypeParameters() = emptyList<KtTypeParameter>()

    override fun getName(): String? = getContainingClassOrObject().name

    override fun getNameAsSafeName() = KtPsiUtil.safeName(name)

    override fun getFqName() = null

    override fun getNameAsName() = nameAsSafeName

    override fun getNameIdentifier() = null

    override fun getIdentifyingElement(): PsiElement? = getConstructorKeyword()

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = throw IncorrectOperationException("setName to constructor")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    open fun getConstructorKeyword(): PsiElement? = findChildByType(KtTokens.CONSTRUCTOR_KEYWORD)

    fun hasConstructorKeyword(): Boolean = stub != null || getConstructorKeyword() != null

    override fun getTextOffset(): Int {
        return getConstructorKeyword()?.textOffset
            ?: valueParameterList?.textOffset
            ?: super.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingClassOrObject().useScope
    }
}
