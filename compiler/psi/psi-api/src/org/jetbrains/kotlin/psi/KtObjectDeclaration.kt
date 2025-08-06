/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub

class KtObjectDeclaration : KtClassOrObject {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinObjectStub) : super(stub, KtStubBasedElementTypes.OBJECT_DECLARATION)

    private val _stub: KotlinObjectStub?
        get() = greenStub as? KotlinObjectStub

    override fun getName(): String? {
        super.getName()?.let { return it }

        if (isCompanion() && !isTopLevel()) {
            //NOTE: a hack in PSI that simplifies writing frontend code
            return SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.toString()
        }

        return null
    }

    override fun setName(@NonNls name: String): PsiElement {
        return if (nameIdentifier == null) {
            val psiFactory = KtPsiFactory(project)
            val result = addAfter(psiFactory.createIdentifier(name), getObjectKeyword()!!)
            addAfter(psiFactory.createWhiteSpace(), getObjectKeyword()!!)

            result
        } else {
            super.setName(name)
        }
    }

    fun isCompanion(): Boolean = hasModifier(KtTokens.COMPANION_KEYWORD)

    override fun getTextOffset(): Int = nameIdentifier?.textRange?.startOffset
        ?: getObjectKeyword()!!.textRange.startOffset

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitObjectDeclaration(this, data)
    }

    fun isObjectLiteral(): Boolean = _stub?.isObjectLiteral ?: (parent is KtObjectLiteralExpression)

    fun getObjectKeyword(): PsiElement? = findChildByType(KtTokens.OBJECT_KEYWORD)

    override fun getIdentifyingElement(): PsiElement? = getObjectKeyword()

    override fun getCompanionObjects(): List<KtObjectDeclaration> = emptyList()
}
