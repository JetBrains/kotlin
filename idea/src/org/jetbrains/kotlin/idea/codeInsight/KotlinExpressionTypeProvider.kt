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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.KotlinType

class KotlinExpressionTypeProvider : ExpressionTypeProvider<KtExpression>() {
    override fun getExpressionsAt(elementAt: PsiElement): List<KtExpression> =
            elementAt.parentsWithSelf.filterIsInstance<KtExpression>().filter { it.shouldShowType() }.toList()

    private fun KtExpression.shouldShowType() = when(this) {
        is KtFunction -> !hasBlockBody() && !hasDeclaredReturnType()
        is KtProperty, is KtDestructuringDeclarationEntry -> true
        is KtStatementExpression, is KtDestructuringDeclaration -> false
        else -> true
    }

    override fun getInformationHint(element: KtExpression): String {
        val type = typeByExpression(element) ?: return "Type is unknown"
        return renderTypeHint(type)
    }

    override fun getErrorHint(): String = "No expression found"

    companion object {
        fun renderTypeHint(type: KotlinType) = "<html>" + DescriptorRenderer.HTML.renderType(type) + "</html>"

        fun typeByExpression(expression: KtExpression): KotlinType? {
            val bindingContext = expression.analyze()

            if (expression is KtCallableDeclaration) {
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, expression] as? CallableDescriptor
                if (descriptor != null) {
                    return descriptor.returnType
                }
            }

            return expression.getType(bindingContext)
        }
    }
}

