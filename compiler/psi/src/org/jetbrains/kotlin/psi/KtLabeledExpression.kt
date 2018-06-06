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
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope

class KtLabeledExpression(node: ASTNode) : KtExpressionWithLabel(node), PsiNameIdentifierOwner {
    @get:IfNotParsed
    val baseExpression: KtExpression?
        get() = findChildByClass(KtExpression::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitLabeledExpression(this, data)

    override fun getName() = getLabelName()

    override fun setName(name: String): PsiElement {
        getTargetLabel()?.replace(KtPsiFactory(project).createLabeledExpression(name).getTargetLabel()!!)
        return this
    }

    override fun getNameIdentifier() = getTargetLabel()?.getIdentifier()

    override fun getUseScope() = LocalSearchScope(this)
}
