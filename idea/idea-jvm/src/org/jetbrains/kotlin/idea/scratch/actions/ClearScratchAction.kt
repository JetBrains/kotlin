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

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.scratch.ui.findScratchFileEditorWithPreview

class ClearScratchAction : ScratchAction(
    KotlinJvmBundle.message("scratch.clear.button"),
    AllIcons.Actions.GC
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val selectedEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val textEditor = TextEditorProvider.getInstance().getTextEditor(selectedEditor)

        textEditor.findScratchFileEditorWithPreview()?.clearOutputHandlers()
    }
}