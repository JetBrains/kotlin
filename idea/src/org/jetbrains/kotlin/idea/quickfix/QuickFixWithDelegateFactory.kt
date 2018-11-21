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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.internal.statistic.service.fus.collectors.FUSApplicationUsageTrigger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.statistics.KotlinIdeQuickfixTrigger

open class QuickFixWithDelegateFactory(
        private val delegateFactory: () -> IntentionAction?
) : IntentionAction {
    private val familyName: String
    private val text: String
    private val startInWriteAction: Boolean

    init {
        val delegate = delegateFactory()
        familyName = delegate?.familyName ?: ""
        text = delegate?.text ?: ""
        startInWriteAction = delegate != null && delegate.startInWriteAction()
    }

    override fun getFamilyName() = familyName

    override fun getText() = text

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val action = delegateFactory() ?: return false
        return action.isAvailable(project, editor, file)
    }

    override fun startInWriteAction() = startInWriteAction

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return
        }

        FUSApplicationUsageTrigger.getInstance().trigger(
                KotlinIdeQuickfixTrigger::class.java,
                this::class.java.simpleName
        )

        val action = delegateFactory() ?: return

        assert(action.detectPriority() == this.detectPriority()) {
            "Incorrect priority of QuickFixWithDelegateFactory wrapper for ${action::class.java.name}"
        }

        action.invoke(project, editor, file)
    }
}

class LowPriorityQuickFixWithDelegateFactory(
        delegateFactory: () -> IntentionAction?
): QuickFixWithDelegateFactory(delegateFactory), LowPriorityAction

class HighPriorityQuickFixWithDelegateFactory(
        delegateFactory: () -> IntentionAction?
): QuickFixWithDelegateFactory(delegateFactory), HighPriorityAction

enum class IntentionActionPriority {
    LOW, NORMAL, HIGH
}

fun IntentionAction.detectPriority(): IntentionActionPriority {
    return when (this) {
        is LowPriorityAction -> IntentionActionPriority.LOW
        is HighPriorityAction -> IntentionActionPriority.HIGH
        else -> IntentionActionPriority.NORMAL
    }
}

fun QuickFixWithDelegateFactory(priority: IntentionActionPriority, createAction: () -> IntentionAction?): QuickFixWithDelegateFactory {
    return when (priority) {
        IntentionActionPriority.NORMAL -> QuickFixWithDelegateFactory(createAction)
        IntentionActionPriority.HIGH -> HighPriorityQuickFixWithDelegateFactory(createAction)
        IntentionActionPriority.LOW -> LowPriorityQuickFixWithDelegateFactory(createAction)
    }
}