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
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.inline.InlineOptionsDialog
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction

// NB: similar to IDEA InlineMethodDialog / KotlinInlineValDialog
class KotlinInlineFunctionDialog(
        project: Project,
        private val function: KtNamedFunction,
        private val reference: KtSimpleNameReference?,
        private val replacementStrategy: CallableUsageReplacementStrategy,
        private val allowInlineThisOnly: Boolean
) : InlineOptionsDialog(project, true, function) {

    private var occurrencesNumber = initOccurrencesNumber(function)

    init {
        myInvokedOnReference = reference != null
        title = RefactoringBundle.message("inline.method.title")
        init()
    }

    override fun allowInlineAll() = true

    override fun getNameLabelText(): String {
        val occurrencesString =
                if (occurrencesNumber > -1) " - $occurrencesNumber occurrence${if (occurrencesNumber == 1) "" else "s"}"
                else ""
        val methodText = "${function.nameAsSafeName}" + function.valueParameters.joinToString(prefix = "(", postfix = ")") {
                             "${it.nameAsSafeName}: ${it.typeReference?.text}"
                         } + (function.getReturnTypeReference()?.let { ": " + it.text } ?: "")
        return RefactoringBundle.message("inline.method.method.label", methodText, occurrencesString)
    }

    override fun getBorderTitle(): String = RefactoringBundle.message("inline.method.border.title")

    override fun getInlineThisText(): String = RefactoringBundle.message("this.invocation.only.and.keep.the.method")

    override fun getInlineAllText(): String = RefactoringBundle.message(
            if (function.isWritable) "all.invocations.and.remove.the.method"
            else "all.invocations.in.project"
    )

    override fun getKeepTheDeclarationText(): String =
            if (function.isWritable) RefactoringBundle.message("all.invocations.keep.the.method")
            else super.getKeepTheDeclarationText()

    public override fun doAction() {
        invokeRefactoring(
                KotlinInlineFunctionProcessor(project, replacementStrategy, function, reference,
                                              inlineThisOnly = isInlineThisOnly || allowInlineThisOnly,
                                              deleteAfter = !isInlineThisOnly && !isKeepTheDeclaration && !allowInlineThisOnly)
        )

        val settings = JavaRefactoringSettings.getInstance()
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_METHOD_THIS = isInlineThisOnly
        }
    }

    override fun doHelpAction() =
            HelpManager.getInstance().invokeHelp(if (function is KtConstructor<*>) HelpID.INLINE_CONSTRUCTOR else HelpID.INLINE_METHOD)

    override fun canInlineThisOnly() = allowInlineThisOnly

    override fun isInlineThis() = JavaRefactoringSettings.getInstance().INLINE_METHOD_THIS
}
