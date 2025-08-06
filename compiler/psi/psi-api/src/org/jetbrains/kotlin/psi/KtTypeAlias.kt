/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.psiUtil.ClassIdCalculator
import org.jetbrains.kotlin.psi.psiUtil.isKtFile
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub

class KtTypeAlias : KtTypeParameterListOwnerStub<KotlinTypeAliasStub>, KtNamedDeclaration, KtClassLikeDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinTypeAliasStub) : super(stub, KtStubBasedElementTypes.TYPEALIAS)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    fun isTopLevel(): Boolean = greenStub?.isTopLevel() ?: isKtFile(parent)

    @IfNotParsed
    fun getTypeAliasKeyword(): PsiElement? =
        findChildByType(KtTokens.TYPE_ALIAS_KEYWORD)

    @IfNotParsed
    fun getTypeReference(): KtTypeReference? = getStubOrPsiChild<KtTypeReference>(KtStubBasedElementTypes.TYPE_REFERENCE)

    override fun getClassId(): ClassId? {
        greenStub?.let { return it.classId }
        return ClassIdCalculator.calculateClassId(this)
    }

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)
}
