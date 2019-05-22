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

import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtProperty

class KotlinInlineValDialog(
    property: KtProperty,
    reference: KtSimpleNameReference?,
    private val replacementStrategy: UsageReplacementStrategy,
    private val assignmentToDelete: KtBinaryExpression?,
    withPreview: Boolean = true
) : AbstractKotlinInlineDialog(property, reference) {

    private val isLocal = (callable as KtProperty).isLocal

    private val simpleLocal = isLocal && (reference == null || occurrencesNumber == 1)

    init {
        setPreviewResults(withPreview && shouldBeShown())
        if (simpleLocal) {
            setDoNotAskOption(object : DoNotAskOption {
                override fun isToBeShown() = EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog

                override fun setToBeShown(value: Boolean, exitCode: Int) {
                    EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog = value
                }

                override fun canBeHidden() = true

                override fun shouldSaveOptionsOnCancel() = false

                override fun getDoNotShowMessage() = "Do not show for local variables in future"
            })
        }
        init()
    }

    fun shouldBeShown() = !simpleLocal || EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog

    override fun isInlineThis() = KotlinRefactoringSettings.instance.INLINE_LOCAL_THIS

    public override fun doAction() {
        invokeRefactoring(
            KotlinInlineCallableProcessor(
                project, replacementStrategy, callable, reference,
                inlineThisOnly = isInlineThisOnly,
                deleteAfter = !isInlineThisOnly && !isKeepTheDeclaration,
                statementToDelete = assignmentToDelete
            )
        )

        val settings = KotlinRefactoringSettings.instance
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_LOCAL_THIS = isInlineThisOnly
        }
    }
}
