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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddInlineToReifiedFunctionInspection : IntentionBasedInspection<KtDeclarationModifierList>(AddInlineToReifiedFunctionIntention())

class AddInlineToReifiedFunctionIntention : SelfTargetingOffsetIndependentIntention<KtDeclarationModifierList>(
        KtDeclarationModifierList::class.java,
        "Add Inline to reified function"
) {
    override fun isApplicableTo(element: KtDeclarationModifierList): Boolean {
        if (!element.hasModifier(KtTokens.REIFIED_KEYWORD)) return false
        val containingFunction = element.getNonStrictParentOfType<KtNamedFunction>() ?: return false
        return !containingFunction.hasModifier(KtTokens.INLINE_KEYWORD)
    }


    override fun applyTo(element: KtDeclarationModifierList, editor: Editor?) {
        element.getNonStrictParentOfType<KtNamedFunction>()?.addModifier(KtTokens.INLINE_KEYWORD)
    }
}