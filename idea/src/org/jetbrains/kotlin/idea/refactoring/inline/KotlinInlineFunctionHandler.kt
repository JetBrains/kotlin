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

import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.replacement.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.replacement.ReplacementBuilder
import org.jetbrains.kotlin.idea.replacement.replaceUsagesInWholeProject
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinInlineFunctionHandler: InlineActionHandler() {
    override fun isEnabledForLanguage(language: Language) = language == KotlinLanguage.INSTANCE

    override fun canInlineElement(element: PsiElement): Boolean {
        return element is KtNamedFunction
               && element.hasBody() && !element.hasBlockBody() // TODO support multiline functions
               && element.getUseScope() is GlobalSearchScope // TODO support local functions
    }

    override fun inlineElement(project: Project, editor: Editor?, element: PsiElement) {
        element as KtNamedFunction

        val descriptor = element.resolveToDescriptor() as SimpleFunctionDescriptor

        val bodyExpression = element.bodyExpression!!
        val replacement = ReplacementBuilder(descriptor, element.getResolutionFacade())
                .buildReplacementExpression(bodyExpression, bodyExpression.getResolutionScope())

        val commandName = RefactoringBundle.message("inline.command", element.name)
        CallableUsageReplacementStrategy(replacement)
                .replaceUsagesInWholeProject(element, commandName, commandName, postAction = { element.delete() })
    }
}