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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

public class DeprecatedSymbolUsageFix(
        element: JetSimpleNameExpression/*TODO?*/,
        replaceWith: ReplaceWith
) : DeprecatedSymbolUsageFixBase(element, replaceWith), CleanupFix, HighPriorityAction {

    override fun getFamilyName() = "Replace deprecated symbol usage"

    override fun getText() = "Replace with '${replaceWith.expression}'" //TODO: substitute?

    override fun invoke(
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            bindingContext: BindingContext,
            replacement: ReplaceWithAnnotationAnalyzer.ReplacementExpression,
            project: Project,
            editor: Editor?
    ) {
        val result = DeprecatedSymbolUsageFixBase.performReplacement(element, bindingContext, resolvedCall, replacement)

        val offset = (result.getCalleeExpressionIfAny() ?: result).getTextOffset()
        editor?.moveCaret(offset)
    }

    override fun elementToBeInvalidated(): PsiElement? {
        val parent = element.getParent()
        return when (parent) {
            is JetCallExpression -> {
                val qualified = parent.getParent() as? JetQualifiedExpression
                if (parent == qualified?.getSelectorExpression()) qualified else parent
            }
            is JetQualifiedExpression -> if (element == parent.getSelectorExpression()) parent else element
            is JetOperationExpression -> if (element == parent.getOperationReference()) parent else element
            else -> element
        }
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val nameExpression = diagnostic.getPsiElement() as? JetSimpleNameExpression ?: return null
            val descriptor = Errors.DEPRECATED_SYMBOL_WITH_MESSAGE.cast(diagnostic).getA()
            val replacement = DeprecatedSymbolUsageFixBase.replaceWithPattern(descriptor) ?: return null
            return DeprecatedSymbolUsageFix(nameExpression, replacement)
        }

    }
}
