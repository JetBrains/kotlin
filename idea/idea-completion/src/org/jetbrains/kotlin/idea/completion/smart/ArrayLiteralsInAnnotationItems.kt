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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

object ArrayLiteralsInAnnotationItems {

    private fun MutableCollection<LookupElement>.addForUsage(expectedInfos: Collection<ExpectedInfo>,
                                                             position: PsiElement) {
        if (position.getParentOfType<KtAnnotationEntry>(false) != null) {
            expectedInfos.asSequence()
                    .filter { it.fuzzyType?.type?.let { type -> KotlinBuiltIns.isArray(type) } == true }
                    .filterNot { it.itemOptions.starPrefix }
                    .mapTo(this) { createLookupElement() }
        }
    }

    private fun MutableCollection<LookupElement>.addForDefaultArguments(expectedInfos: Collection<ExpectedInfo>,
                                                                        position: PsiElement) {

        // CLASS [MODIFIER_LIST, PRIMARY_CONSTRUCTOR [VALUE_PARAMETER_LIST [VALUE_PARAMETER [..., REFERENCE_EXPRESSION=position]]]]
        val valueParameter = position.parent as? KtParameter ?: return
        val klass = position.getParentOfType<KtClass>(true) ?: return
        if (!klass.hasModifier(KtTokens.ANNOTATION_KEYWORD)) return
        val primaryConstructor = klass.primaryConstructor ?: return

        if (primaryConstructor.valueParameterList == valueParameter.parent) {
            expectedInfos
                    .filter { it.fuzzyType?.type?.let { type -> KotlinBuiltIns.isArray(type) } == true }
                    .mapTo(this) { createLookupElement() }
        }
    }

    private fun createLookupElement(): LookupElement {
        return LookupElementBuilder.create("[]")
                .withInsertHandler { context, _ ->
                    context.editor.caretModel.moveToOffset(context.tailOffset - 1)
                }
                .apply { putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.ARRAY_LITERAL_IN_ANNOTATION) }
    }

    fun collect(expectedInfos: Collection<ExpectedInfo>, position: PsiElement): Collection<LookupElement> {
        return mutableListOf<LookupElement>().apply {
            addForUsage(expectedInfos, position)
            addForDefaultArguments(expectedInfos, position)
        }
    }
}