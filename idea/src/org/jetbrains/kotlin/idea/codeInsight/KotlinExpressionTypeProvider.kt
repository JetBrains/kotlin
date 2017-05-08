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
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinExpressionTypeProvider : ExpressionTypeProvider<KtExpression>() {
    private val typeRenderer = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
        textFormat = RenderingFormat.HTML
        classifierNamePolicy = object : ClassifierNamePolicy {
            override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
                if (DescriptorUtils.isAnonymousObject(classifier)) {
                    return "&lt;anonymous object&gt;"
                }
                return ClassifierNamePolicy.SHORT.renderClassifier(classifier, renderer)
            }
        }
    }

    override fun getExpressionsAt(elementAt: PsiElement): List<KtExpression> {
        val candidates = elementAt.parentsWithSelf.filterIsInstance<KtExpression>().filter { it.shouldShowType() }.toList()
        return candidates.takeWhile { it.textRange.startOffset == candidates.first().textRange.startOffset }
    }

    private fun KtExpression.shouldShowType() = when (this) {
        is KtFunctionLiteral -> false
        is KtFunction -> !hasBlockBody() && !hasDeclaredReturnType()
        is KtProperty -> typeReference == null
        is KtPropertyAccessor -> false
        is KtDestructuringDeclarationEntry -> true
        is KtStatementExpression, is KtDestructuringDeclaration -> false
        is KtIfExpression, is KtWhenExpression, is KtTryExpression -> shouldShowStatementType()
        is KtLoopExpression -> false
        is KtConstantExpression -> false
        else -> getQualifiedExpressionForSelector() == null && parent !is KtCallableReferenceExpression && !isFunctionCallee()
    }

    private fun KtExpression.shouldShowStatementType(): Boolean {
        if (parent !is KtBlockExpression) return true
        if (parent.children.lastOrNull() == this) {
            return analyze(BodyResolveMode.PARTIAL)[BindingContext.USED_AS_EXPRESSION, this] ?: false
        }
        return false
    }

    private fun KtExpression.isFunctionCallee(): Boolean {
        val callExpression = parent as? KtCallExpression ?: return false
        if (callExpression.calleeExpression != this) return false
        return mainReference?.resolve() is KtFunction
    }

    override fun getInformationHint(element: KtExpression): String {
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)

        return "<html>${renderExpressionType(element, bindingContext)}</html>"
    }

    private fun renderExpressionType(element: KtExpression, bindingContext: BindingContext): String {
        if (element is KtCallableDeclaration) {
            val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? CallableDescriptor
            if (descriptor != null) {
                return descriptor.returnType?.let { typeRenderer.renderType(it) } ?: "Type is unknown"
            }
        }

        val expressionTypeInfo = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, element] ?: return "Type is unknown"
        val expressionType = element.getType(bindingContext)
        val result = expressionType?.let { typeRenderer.renderType(it) } ?: return "Type is unknown"

        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(element, expressionType, bindingContext, element.findModuleDescriptor())
        val types = expressionTypeInfo.dataFlowInfo.getStableTypes(dataFlowValue)
        if (!types.isEmpty()) {
            return types.joinToString(separator = " & ") { typeRenderer.renderType(it) } + " (smart cast from " + result + ")"
        }

        val smartCast = bindingContext[BindingContext.SMARTCAST, element]
        if (smartCast != null && element is KtReferenceExpression) {
            val declaredType = (bindingContext[BindingContext.REFERENCE_TARGET, element] as? CallableDescriptor)?.returnType
            if (declaredType != null) {
                return result + " (smart cast from " + typeRenderer.renderType(declaredType) + ")"
            }
        }
        return result
    }

    override fun getErrorHint(): String = "No expression found"
}
