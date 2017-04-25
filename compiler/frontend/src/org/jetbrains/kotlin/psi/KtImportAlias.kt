/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtImportAlias : KtElementImplStub<KotlinImportAliasStub>, PsiNameIdentifierOwner {
    @Suppress("unused") constructor(node: ASTNode) : super(node)
    @Suppress("unused") constructor(stub: KotlinImportAliasStub) : super(stub, KtStubElementTypes.IMPORT_ALIAS)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitImportAlias(this, data)
    }

    override fun getName() = stub?.getName() ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(KtPsiFactory(this).createNameIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = findChildByType(KtTokens.IDENTIFIER)
}