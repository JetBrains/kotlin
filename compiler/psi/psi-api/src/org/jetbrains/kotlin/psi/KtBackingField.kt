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
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub

class KtBackingField : KtDeclarationStub<KotlinBackingFieldStub>, KtModifierListOwner, KtDeclarationWithInitializer,
    KtDeclarationWithReturnType {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinBackingFieldStub) : super(stub, KtStubBasedElementTypes.BACKING_FIELD)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R =
        visitor.visitBackingField(this, data)

    val equalsToken: PsiElement?
        get() = findChildByType(KtTokens.EQ)

    override fun getTypeReference(): KtTypeReference? =
        getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE)

    val namePlaceholder: PsiElement
        get() = fieldKeyword ?: node.psi

    override fun getInitializer(): KtExpression? {
        val stub = greenStub
        if (stub != null && !stub.hasInitializer) {
            return null
        }

        return PsiTreeUtil.getNextSiblingOfType(equalsToken, KtExpression::class.java)
    }

    override fun hasInitializer(): Boolean {
        greenStub?.let {
            return it.hasInitializer
        }

        return getInitializer() != null
    }

    override fun getTextOffset(): Int =
        namePlaceholder.textRange.startOffset

    val fieldKeyword: PsiElement?
        get() = findChildByType(KtTokens.FIELD_KEYWORD)

    @Suppress("unused")
    @Deprecated("Use typeReference instead", ReplaceWith("typeReference"))
    val returnTypeReference: KtTypeReference?
        get() = typeReference
}
