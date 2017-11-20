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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinInlineFunctionHandler: InlineActionHandler() {
    override fun isEnabledForLanguage(language: Language) = language == KotlinLanguage.INSTANCE

    //TODO: overrides etc
    override fun canInlineElement(element: PsiElement) = element is KtNamedFunction && element.hasBody()

    override fun inlineElement(project: Project, editor: Editor?, element: PsiElement) {
        element as KtNamedFunction

        val reference = editor?.let { TargetElementUtil.findReference(it, it.caretModel.offset) }
        val nameReference = when (reference) {
            is KtSimpleNameReference -> reference
            is PsiMultiReference -> reference.references.firstIsInstanceOrNull<KtSimpleNameReference>()
            else -> null
        }
        val recursive = element.isRecursive()
        if (recursive && nameReference == null) {
            val message = RefactoringBundle.getCannotRefactorMessage("Inline recursive function is supported only on references")
            CommonRefactoringUtil.showErrorHint(project, editor, message, "Inline Function", null)
            return
        }

        val descriptor = element.unsafeResolveToDescriptor() as SimpleFunctionDescriptor
        val codeToInline = buildCodeToInline(
                element,
                descriptor.returnType,
                element.hasDeclaredReturnType(),
                element.bodyExpression!!,
                element.hasBlockBody(),
                editor
        ) ?: return

        val replacementStrategy = CallableUsageReplacementStrategy(codeToInline)

        val dialog = KotlinInlineFunctionDialog(project, element, nameReference, replacementStrategy,
                                                allowInlineThisOnly = recursive)
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            dialog.show()
        }
        else {
            dialog.doAction()
        }
    }

    private fun KtNamedFunction.isRecursive(): Boolean {
        val context = analyzeFully()
        return bodyExpression?.includesCallOf(context[BindingContext.FUNCTION, this] ?: return false, context) ?: false
    }

    private fun KtExpression.includesCallOf(descriptor: FunctionDescriptor, context: BindingContext): Boolean {
        val refDescriptor = getResolvedCall(context)?.resultingDescriptor
        return descriptor == refDescriptor || anyDescendantOfType<KtExpression> {
                   it !== this && descriptor == it.getResolvedCall(context)?.resultingDescriptor
               }
    }

}