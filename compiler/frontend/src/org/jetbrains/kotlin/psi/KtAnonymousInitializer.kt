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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.utils.sure

class KtClassInitializer : KtDeclarationStub<KotlinPlaceHolderStub<KtClassInitializer>>, KtAnonymousInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinPlaceHolderStub<KtClassInitializer>) : super(stub, KtStubElementTypes.ANONYMOUS_INITIALIZER)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitAnonymousInitializer(this, data)

    override val body: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override val openBraceNode: PsiElement?
        get() {
            val body = body
            return if (body is KtBlockExpression) body.lBrace else null
        }

    override val containingDeclaration: KtDeclaration
        get() {
            val classOrObject = PsiTreeUtil.getParentOfType(this, KtClassOrObject::class.java, true)
            if (classOrObject != null) {
                return classOrObject
            }
            val type = PsiTreeUtil.getParentOfType(this, KtScript::class.java, true)
            return type.sure { "Should only be present in class, object or script" }
        }
}

public interface KtAnonymousInitializer : KtDeclaration, KtStatementExpression {
    val containingDeclaration: KtDeclaration
    val body: KtExpression?
    val openBraceNode: PsiElement?
}