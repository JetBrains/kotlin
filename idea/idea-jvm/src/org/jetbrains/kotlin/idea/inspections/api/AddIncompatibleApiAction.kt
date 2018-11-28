/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.api

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager

class AddIncompatibleApiAction : EditorAction(AddIncompatibleApiInspectionHandler())

private class AddIncompatibleApiInspectionHandler : EditorActionHandler() {
    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        if (!super.isEnabledForCaret(editor, caret, dataContext)) {
            return false
        }

        val project = editor.project ?: return false
        val incompatibleAPIToolState = ProjectInspectionProfileManager.getInstance(project).currentProfile.getToolDefaultState(
            IncompatibleAPIInspection.SHORT_NAME, project
        )

        return incompatibleAPIToolState.isEnabled
    }

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        val project = editor.project ?: return
        val element = TargetElementUtil.findTargetElement(
            editor, TargetElementUtil.ELEMENT_NAME_ACCEPTED or TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
        ) ?: return
        val qualified = getQualifiedNameFromProviders(element) ?: return
        AddToIncompatibleApiDialog(project, qualified).show()
    }
}