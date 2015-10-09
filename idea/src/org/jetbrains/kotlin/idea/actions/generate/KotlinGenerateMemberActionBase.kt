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

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.codeInsight.CodeInsightUtilBase
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInspection.ex.GlobalInspectionContextBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetDeclaration

abstract class KotlinGenerateMemberActionBase<Info : Any> : KotlinGenerateActionBase() {
    protected abstract fun prepareMembersInfo(klass: JetClassOrObject, project: Project): Info?

    protected abstract fun generateMembers(editor: Editor, info: Info): List<JetDeclaration>

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        if (!CodeInsightUtilBase.prepareEditorForWrite(editor)) return
        if (!FileDocumentManager.getInstance().requestWriting(editor.document, project)) return
        val klass = getTargetClass(editor, file) ?: return
        val membersInfo = prepareMembersInfo(klass, project) ?: return

        project.executeWriteCommand(commandName, this) {
            val newMembers = generateMembers(editor, membersInfo)
            GlobalInspectionContextBase.cleanupElements(project, null, *newMembers.toTypedArray())
            newMembers.firstOrNull()?.let { GenerateMembersUtil.positionCaret(editor, it, false) }
        }
    }
}
