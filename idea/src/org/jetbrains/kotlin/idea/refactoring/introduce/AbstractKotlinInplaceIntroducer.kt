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

package org.jetbrains.kotlin.idea.refactoring.introduce

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer
import com.intellij.refactoring.rename.inplace.InplaceRefactoring
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.awt.BorderLayout

public abstract class AbstractKotlinInplaceIntroducer<D: JetNamedDeclaration>(
        localVariable: D?,
        expression: JetExpression?,
        occurrences: Array<JetExpression>,
        title: String,
        project: Project,
        editor: Editor
): AbstractInplaceIntroducer<D, JetExpression>(project, editor, expression, localVariable, occurrences, title, JetFileType.INSTANCE) {
    protected fun initFormComponents(init: FormBuilder.() -> Unit) {
        myWholePanel.setLayout(BorderLayout())

        with(FormBuilder.createFormBuilder()) {
            init()
            myWholePanel.add(getPanel(), BorderLayout.CENTER)
        }
    }

    protected fun runWriteCommandAndRestart(action: () -> Unit) {
        myEditor.putUserData(InplaceRefactoring.INTRODUCE_RESTART, true)
        try {
            stopIntroduce(myEditor)
            myProject.executeWriteCommand(getCommandName(), getCommandName(), action)
            // myExprMarker was invalidated by stopIntroduce()
            myExprMarker = myExpr?.let { createMarker(it) }
            startInplaceIntroduceTemplate()
        }
        finally {
            myEditor.putUserData(InplaceRefactoring.INTRODUCE_RESTART, false)
        }
    }

    override fun getActionName(): String? = null

    override fun restoreExpression(
            containingFile: PsiFile,
            declaration: D,
            marker: RangeMarker,
            exprText: String?
    ): JetExpression? {
        if (exprText == null || !declaration.isValid()) return null

        return containingFile
                .findElementAt(marker.getStartOffset())
                ?.getNonStrictParentOfType<JetSimpleNameExpression>()
                ?.replaced(JetPsiFactory(myProject).createExpression(exprText))
    }

    override fun updateTitle(declaration: D?) = updateTitle(declaration, null)

    override fun saveSettings(declaration: D?) {

    }
}