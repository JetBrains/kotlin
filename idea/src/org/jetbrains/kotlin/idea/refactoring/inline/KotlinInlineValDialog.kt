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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.inline.InlineOptionsDialog
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtProperty

class KotlinInlineValDialog(
        private val property: KtProperty,
        private val reference: KtSimpleNameReference?,
        private val replacementStrategy: CallableUsageReplacementStrategy,
        private val assignments: Set<PsiElement>
 ) : InlineOptionsDialog(property.project, true, property) {

    private var occurrenceCount = initOccurrencesNumber(property)

    private val kind = if (property.isLocal) "local variable" else "property"

    private val refactoringName = "Inline ${StringUtil.capitalizeWords(kind, true)}"

    init {
        myInvokedOnReference = reference != null
        title = refactoringName
        init()
    }

    override fun getBorderTitle() = refactoringName

    override fun getNameLabelText() = "${kind.capitalize()} ${property.name}"

    private val occurrencesString get() = if (occurrenceCount >= 0) {
        " (" + occurrenceCount + " occurrence" + (if (occurrenceCount == 1) ")" else "s)")
    } else ""

    override fun getInlineAllText() =
            "Inline all references and remove the $kind" + occurrencesString

    override fun isInlineThis() = JavaRefactoringSettings.getInstance().INLINE_LOCAL_THIS

    override fun getInlineThisText() = "Inline this occurrence and leave the $kind"

    public override fun doAction() {
        invokeRefactoring(
                KotlinInlineCallableProcessor(project, replacementStrategy, property, reference,
                                              inlineThisOnly = isInlineThisOnly,
                                              deleteAfter = !isInlineThisOnly,
                                              assignments = assignments)
        )

        val settings = JavaRefactoringSettings.getInstance()
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_LOCAL_THIS = isInlineThisOnly
        }
    }
}
