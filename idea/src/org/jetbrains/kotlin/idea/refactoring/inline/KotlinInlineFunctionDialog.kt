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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.help.HelpManager
import com.intellij.openapi.project.Project
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinInlineFunctionDialog(
        project: Project,
        function: KtNamedFunction,
        reference: KtSimpleNameReference?,
        private val replacementStrategy: UsageReplacementStrategy,
        private val allowInlineThisOnly: Boolean
) : AbstractKotlinInlineDialog(function, reference, project) {

    init {
        init()
    }

    override fun isInlineThis() = KotlinRefactoringSettings.instance.INLINE_METHOD_THIS

    public override fun doAction() {
        invokeRefactoring(
                KotlinInlineCallableProcessor(project, replacementStrategy, callable, reference,
                                              inlineThisOnly = isInlineThisOnly || allowInlineThisOnly,
                                              deleteAfter = !isInlineThisOnly && !isKeepTheDeclaration && !allowInlineThisOnly)
        )

        val settings = KotlinRefactoringSettings.instance
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_METHOD_THIS = isInlineThisOnly
        }
    }

    override fun doHelpAction() =
            HelpManager.getInstance().invokeHelp(if (callable is KtConstructor<*>) HelpID.INLINE_CONSTRUCTOR else HelpID.INLINE_METHOD)

    override fun canInlineThisOnly() = allowInlineThisOnly
}
