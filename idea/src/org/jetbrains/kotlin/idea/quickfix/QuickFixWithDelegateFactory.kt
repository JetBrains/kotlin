/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

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

        val action = delegateFactory() ?: return

        assert(action.detectPriority() == this.detectPriority()) {
            "Incorrect priority of QuickFixWithDelegateFactory wrapper for ${action::class.java.name}"
        }

        action.invoke(project, editor, file)
    }
}

class LowPriorityQuickFixWithDelegateFactory(
    delegateFactory: () -> IntentionAction?
) : QuickFixWithDelegateFactory(delegateFactory), LowPriorityAction

class HighPriorityQuickFixWithDelegateFactory(
    delegateFactory: () -> IntentionAction?
) : QuickFixWithDelegateFactory(delegateFactory), HighPriorityAction

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