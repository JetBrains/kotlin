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

import com.google.common.collect.Lists
import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageInWholeProjectFix
import org.jetbrains.kotlin.idea.quickfix.ReplaceWith
import org.jetbrains.kotlin.idea.quickfix.ReplaceWithAnnotationAnalyzer
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

public class KotlinInlineFunctionHandler: InlineActionHandler() {
    override fun isEnabledForLanguage(language: Language) = JetLanguage.INSTANCE == language

    override fun canInlineElement(element: PsiElement): Boolean {
        return element is JetNamedFunction
               && element.hasBody() && !element.hasBlockBody() // TODO support multiline functions
               && element.getUseScope() is GlobalSearchScope // TODO support local functions
    }

    override fun inlineElement(project: Project, editor: Editor?, element: PsiElement) {
        element as JetNamedFunction

        val descriptor = element.resolveToDescriptor() as SimpleFunctionDescriptor

        val replacement = ReplaceWithAnnotationAnalyzer.analyze(
                ReplaceWith(element.getBodyExpression()!!.getText()),
                descriptor,
                element.getResolutionFacade(),
                project
        )

        DeprecatedSymbolUsageInWholeProjectFix.findAndReplaceUsages(
                project,
                element,
                replacement,
                RefactoringBundle.message("inline.command", element.getName()),
                true
        )
    }
}