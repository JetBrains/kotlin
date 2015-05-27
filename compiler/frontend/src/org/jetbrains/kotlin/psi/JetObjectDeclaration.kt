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
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

import java.util.Collections

public class JetObjectDeclaration : JetNamedDeclarationStub<KotlinObjectStub>, JetClassOrObject {
    public constructor(node: ASTNode) : super(node) {
    }

    public constructor(stub: KotlinObjectStub) : super(stub, JetStubElementTypes.OBJECT_DECLARATION) {
    }

    override fun getName(): String? {
        val stub = getStub()
        if (stub != null) {
            return stub.getName()
        }

        val nameAsDeclaration = getNameAsDeclaration()
        if (nameAsDeclaration == null && isCompanion()) {
            //NOTE: a hack in PSI that simplifies writing frontend code
            return SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.toString()
        }
        return nameAsDeclaration?.getName()
    }

    override fun isTopLevel(): Boolean {
        val stub = getStub()
        if (stub != null) {
            return stub.isTopLevel()
        }

        return getParent() is JetFile
    }

    override fun getNameIdentifier(): PsiElement? {
        val nameAsDeclaration = getNameAsDeclaration()
        return nameAsDeclaration?.getNameIdentifier()
    }

    throws(IncorrectOperationException::class)
    override fun setName(NonNls name: String): PsiElement {
        val declarationName = getNameAsDeclaration()
        if (declarationName == null) {
            val psiFactory = JetPsiFactory(getProject())
            val result = addAfter(psiFactory.createObjectDeclarationName(name), getObjectKeyword())
            addAfter(psiFactory.createWhiteSpace(), getObjectKeyword())

            return result
        }
        else {
            return declarationName.setName(name)
        }
    }

    override fun getNameAsDeclaration(): JetObjectDeclarationName? {
        return findChildByType<PsiElement>(JetNodeTypes.OBJECT_DECLARATION_NAME) as JetObjectDeclarationName
    }

    public fun isCompanion(): Boolean {
        val stub = getStub()
        if (stub != null) {
            return stub.isCompanion()
        }
        return hasModifier(JetTokens.COMPANION_KEYWORD)
    }

    override fun hasModifier(modifier: JetModifierKeywordToken): Boolean {
        val modifierList = getModifierList()
        return modifierList != null && modifierList.hasModifier(modifier)
    }

    override fun getDelegationSpecifierList(): JetDelegationSpecifierList? {
        return getStubOrPsiChild(JetStubElementTypes.DELEGATION_SPECIFIER_LIST)
    }

    override fun getDelegationSpecifiers(): List<JetDelegationSpecifier> {
        val list = getDelegationSpecifierList()
        return if (list != null) list.getDelegationSpecifiers() else emptyList<JetDelegationSpecifier>()
    }

    override fun getAnonymousInitializers(): List<JetClassInitializer> {
        val body = getBody() ?: return emptyList<JetClassInitializer>()

        return body.getAnonymousInitializers()
    }

    override fun getBody(): JetClassBody? {
        return getStubOrPsiChild(JetStubElementTypes.CLASS_BODY)
    }

    override fun isLocal(): Boolean {
        val stub = getStub()
        if (stub != null) {
            return stub.isLocal()
        }
        return JetPsiUtil.isLocal(this)
    }

    override fun getTextOffset(): Int {
        val nameIdentifier = getNameIdentifier()
        if (nameIdentifier != null) {
            return nameIdentifier.getTextRange().getStartOffset()
        }
        else {
            return getObjectKeyword().getTextRange().getStartOffset()
        }
    }

    override fun getDeclarations(): List<JetDeclaration> {
        val body = getBody() ?: return emptyList<JetDeclaration>()

        return body.getDeclarations()
    }

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitObjectDeclaration(this, data)
    }

    public fun isObjectLiteral(): Boolean {
        val stub = getStub()
        if (stub != null) {
            return stub.isObjectLiteral()
        }
        return getParent() is JetObjectLiteralExpression
    }

    public fun getObjectKeyword(): PsiElement {
        return findChildByType(JetTokens.OBJECT_KEYWORD)
    }

    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }
}
