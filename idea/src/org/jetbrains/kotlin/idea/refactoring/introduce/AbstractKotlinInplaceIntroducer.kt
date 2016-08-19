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
import com.intellij.psi.PsiFile
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer
import com.intellij.refactoring.rename.inplace.InplaceRefactoring
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import java.awt.BorderLayout

abstract class AbstractKotlinInplaceIntroducer<D: KtNamedDeclaration>(
        localVariable: D?,
        expression: KtExpression?,
        occurrences: Array<KtExpression>,
        title: String,
        project: Project,
        editor: Editor
): AbstractInplaceIntroducer<D, KtExpression>(project, editor, expression, localVariable, occurrences, title, KotlinFileType.INSTANCE) {
    protected fun initFormComponents(init: FormBuilder.() -> Unit) {
        myWholePanel.layout = BorderLayout()

        with(FormBuilder.createFormBuilder()) {
            init()
            myWholePanel.add(panel, BorderLayout.CENTER)
        }
    }

    protected fun runWriteCommandAndRestart(action: () -> Unit) {
        myEditor.putUserData(InplaceRefactoring.INTRODUCE_RESTART, true)
        try {
            stopIntroduce(myEditor)
            myProject.executeWriteCommand(commandName, commandName, action)
            // myExprMarker was invalidated by stopIntroduce()
            myExprMarker = myExpr?.let { createMarker(it) }
            startInplaceIntroduceTemplate()
        }
        finally {
            myEditor.putUserData(InplaceRefactoring.INTRODUCE_RESTART, false)
        }
    }

    protected fun updateVariableName() {
        val currentName = inputName.quoteIfNeeded()
        if (KotlinNameSuggester.isIdentifier(currentName)) {
            localVariable.setName(currentName)
        }
    }

    override fun getActionName(): String? = null

    override fun restoreExpression(
            containingFile: PsiFile,
            declaration: D,
            marker: RangeMarker,
            exprText: String?
    ): KtExpression? {
        if (exprText == null || !declaration.isValid) return null

        val leaf = containingFile.findElementAt(marker.startOffset) ?: return null

        leaf.getParentOfTypeAndBranch<KtProperty> { nameIdentifier }?.let {
            return it.replaced(KtPsiFactory(myProject).createDeclaration(exprText))
        }

        val occurrenceExprText = (myExpr as? KtProperty)?.name ?: exprText
        return leaf
                .getNonStrictParentOfType<KtSimpleNameExpression>()
                ?.replaced(KtPsiFactory(myProject).createExpression(occurrenceExprText))
    }

    override fun updateTitle(declaration: D?) = updateTitle(declaration, null)

    override fun saveSettings(declaration: D) {

    }
}