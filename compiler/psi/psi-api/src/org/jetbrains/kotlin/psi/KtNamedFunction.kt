/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.isContractPresentPsiCheck
import org.jetbrains.kotlin.psi.psiUtil.isKtFile
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.typeRefHelpers.getTypeReference
import org.jetbrains.kotlin.psi.typeRefHelpers.setTypeReference

/**
 * Note: this class is not intended to be extended and is marked `open` solely for backward compatibility.
 */
open class KtNamedFunction : KtTypeParameterListOwnerStub<KotlinFunctionStub>, KtFunction, KtDeclarationWithInitializer {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinFunctionStub) : super(stub, /* nodeType = */ KtStubBasedElementTypes.FUNCTION)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R =
        visitor.visitNamedFunction(this, data)

    open fun hasTypeParameterListBeforeFunctionName(): Boolean {
        greenStub?.let {
            return it.hasTypeParameterListBeforeFunctionName
        }

        val typeParameterList = typeParameterList ?: return false
        val nameIdentifier = nameIdentifier ?: return true
        return nameIdentifier.textOffset > typeParameterList.textOffset
    }

    override fun hasBlockBody(): Boolean {
        greenStub?.let {
            return it.hasNoExpressionBody
        }
        return equalsToken == null
    }

    @get:IfNotParsed // "function" with no "fun" keyword is created by parser for "{...}" on top-level or in the class body
    open val funKeyword: PsiElement?
        get() = findChildByType(KtTokens.FUN_KEYWORD)

    override fun getEqualsToken(): PsiElement? =
        findChildByType(KtTokens.EQ)

    override fun getInitializer(): KtExpression? =
        PsiTreeUtil.getNextSiblingOfType(/* sibling = */ equalsToken, /* aClass = */ KtExpression::class.java)

    override fun hasInitializer(): Boolean =
        initializer != null

    override fun getPresentation(): ItemPresentation? =
        ItemPresentationProviders.getItemPresentation(/* element = */ this)

    override fun getValueParameterList(): KtParameterList? =
        getStubOrPsiChild(KtStubBasedElementTypes.VALUE_PARAMETER_LIST)

    override fun getValueParameters(): List<KtParameter> =
        valueParameterList?.parameters.orEmpty()

    override fun getBodyExpression(): KtExpression? {
        val stub = greenStub
        if (stub != null && !stub.hasBody) {
            return null
        }

        return findChildByClass(KtExpression::class.java)
    }

    override fun getBodyBlockExpression(): KtBlockExpression? {
        val stub = greenStub
        if (stub != null) {
            if (!(stub.hasNoExpressionBody && stub.hasBody)) {
                return null
            }
        }

        return findChildByClass(KtExpression::class.java) as? KtBlockExpression
    }

    override fun hasBody(): Boolean {
        greenStub?.let {
            return it.hasBody
        }
        return bodyExpression != null
    }

    override fun hasDeclaredReturnType(): Boolean =
        typeReference != null

    override fun getReceiverTypeReference(): KtTypeReference? {
        val stub = greenStub ?: return receiverTypeRefByTree
        if (!stub.isExtension) {
            return null
        }
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_REFERENCE).firstOrNull()
    }

    private val receiverTypeRefByTree: KtTypeReference?
        get() {
            var child = firstChild
            while (child != null) {
                val tt = child.node.elementType
                if (tt === KtTokens.LPAR || tt === KtTokens.COLON) break
                if (child is KtTypeReference) {
                    return child
                }
                child = child.nextSibling
            }

            return null
        }

    override fun getContextReceivers(): List<KtContextReceiver> =
        contextReceiverList?.contextReceivers().orEmpty()

    override fun getTypeReference(): KtTypeReference? {
        val stub = greenStub ?: return getTypeReference(declaration = this)

        val typeReferences = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_REFERENCE)
        val returnTypeIndex = if (stub.isExtension) 1 else 0
        return if (returnTypeIndex < typeReferences.size) typeReferences[returnTypeIndex] else null
    }

    override fun setTypeReference(typeRef: KtTypeReference?): KtTypeReference? =
        setTypeReference(declaration = this, addAfter = valueParameterList, typeRef = typeRef)

    override fun getColon(): PsiElement? =
        findChildByType(KtTokens.COLON)

    override fun isLocal(): Boolean {
        val parent = parent
        return !(isKtFile(parent) || parent is KtClassBody || parent.parent is KtScript)
    }

    open val isAnonymous: Boolean
        get() = name == null && isLocal

    open val isTopLevel: Boolean
        get() {
            greenStub?.let {
                return it.isTopLevel
            }
            return isKtFile(parent)
        }

    @Suppress("unused") // keep for compatibility with potential plugins
    open fun shouldChangeModificationCount(place: PsiElement?): Boolean =
        // Suppress Java check for out-of-block
        false

    override fun getContractDescription(): KtContractEffectList? =
        getStubOrPsiChild(KtStubBasedElementTypes.CONTRACT_EFFECT_LIST)

    @OptIn(KtImplementationDetail::class)
    override fun mayHaveContract(): Boolean {
        greenStub?.let {
            return it.mayHaveContract
        }
        return isLegacyContractPresentPsiCheck()
    }

    open fun mayHaveContract(isAllowedOnMembers: Boolean): Boolean {
        greenStub?.let {
            return it.mayHaveContract
        }
        return isContractPresentPsiCheck(isAllowedOnMembers)
    }
}
