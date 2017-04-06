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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.utils.sure

interface KtAnonymousInitializer : KtDeclaration, KtStatementExpression {
    val containingDeclaration: KtDeclaration
    val body: KtExpression?
}

class KtClassInitializer : KtDeclarationStub<KotlinPlaceHolderStub<KtClassInitializer>>, KtAnonymousInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtClassInitializer>) : super(stub, KtStubElementTypes.CLASS_INITIALIZER)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitClassInitializer(this, data)

    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    val openBraceNode: PsiElement?
        get() = (body as? KtBlockExpression)?.lBrace

    val initKeyword: PsiElement
        get() = findChildByType(KtTokens.INIT_KEYWORD)!!

    override val containingDeclaration: KtClassOrObject
        get() = getParentOfType<KtClassOrObject>(true).sure { "Should only be present in class or object" }
}

class KtScriptInitializer(node: ASTNode) : KtDeclarationImpl(node), KtAnonymousInitializer {
    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override val containingDeclaration: KtScript
        get() = getParentOfType<KtScript>(true).sure { "Should only be present in script" }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitScriptInitializer(this, data)
}