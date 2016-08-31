/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtTypeAlias : KtTypeParameterListOwnerStub<KotlinTypeAliasStub>, KtNamedDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinTypeAliasStub) : super(stub, KtStubElementTypes.TYPEALIAS)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R =
            visitor.visitTypeAlias(this, data)

    fun isTopLevel(): Boolean =
            stub?.isTopLevel() ?: parent is KtFile

    @IfNotParsed
    fun getTypeAliasKeyword(): PsiElement? =
            findChildByType(KtTokens.TYPE_ALIAS_KEYWORD)

    @IfNotParsed
    fun getTypeReference(): KtTypeReference? {
        if (stub != null) {
            val typeReferences = getStubOrPsiChildrenAsList<KtTypeReference, KotlinPlaceHolderStub<KtTypeReference>>(KtStubElementTypes.TYPE_REFERENCE)
            return typeReferences[0]
        }
        else {
            return findChildByType(KtNodeTypes.TYPE_REFERENCE)
        }
    }

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)
}
