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

package org.jetbrains.kotlin.idea.quickfix.quickfixUtil

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.ifEmpty

inline fun <reified T : PsiElement> Diagnostic.createIntentionForFirstParentOfType(
    factory: (T) -> KotlinQuickFixAction<T>?
) = psiElement.getNonStrictParentOfType<T>()?.let(factory)


fun createIntentionFactory(
    factory: (Diagnostic) -> IntentionAction?
) = object : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic) = factory(diagnostic)
}

fun KtPrimaryConstructor.addConstructorKeyword(): PsiElement? {
    val keyword = KtPsiFactory(this).createConstructorKeyword()
    return addAfter(keyword, modifierList ?: return null)
}

fun getDataFlowAwareTypes(
    expression: KtExpression,
    bindingContext: BindingContext = expression.analyze(),
    originalType: KotlinType? = bindingContext.getType(expression)
): Collection<KotlinType> {
    if (originalType == null) return emptyList()
    val dataFlowInfo = bindingContext.getDataFlowInfoAfter(expression)
    val dataFlowValueFactory = expression.getResolutionFacade().frontendService<DataFlowValueFactory>()
    val expressionType = bindingContext.getType(expression) ?: return listOf(originalType)
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(
        expression, expressionType, bindingContext, expression.getResolutionFacade().moduleDescriptor
    )
    return dataFlowInfo.getCollectedTypes(dataFlowValue, expression.languageVersionSettings).ifEmpty { listOf(originalType) }
}
