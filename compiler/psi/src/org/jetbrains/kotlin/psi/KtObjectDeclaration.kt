/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtObjectDeclaration : KtClassOrObject {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinObjectStub) : super(stub, KtStubElementTypes.OBJECT_DECLARATION)

    private val _stub: KotlinObjectStub?
        get() = stub as? KotlinObjectStub

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

    fun isCompanion(): Boolean = _stub?.isCompanion() ?: hasModifier(KtTokens.COMPANION_KEYWORD)

    override fun getTextOffset(): Int = nameIdentifier?.textRange?.startOffset
            ?: getObjectKeyword()!!.textRange.startOffset

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitObjectDeclaration(this, data)
    }

    fun isObjectLiteral(): Boolean = _stub?.isObjectLiteral() ?: (parent is KtObjectLiteralExpression)

    fun getObjectKeyword(): PsiElement? = findChildByType(KtTokens.OBJECT_KEYWORD)

    override fun getIdentifyingElement(): PsiElement? = getObjectKeyword()

    override fun getCompanionObjects(): List<KtObjectDeclaration> = emptyList()
}
