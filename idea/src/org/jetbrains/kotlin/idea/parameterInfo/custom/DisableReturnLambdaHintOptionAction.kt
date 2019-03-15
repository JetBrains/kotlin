/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo.custom

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.hints.InlayParameterHintsExtension
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.parameterInfo.HintType
import org.jetbrains.kotlin.idea.util.refreshAllOpenEditors

class DisableReturnLambdaHintOptionAction : IntentionAction, LowPriorityAction {
    override fun getText(): String {
        val optionName = HintType.LAMBDA_RETURN_EXPRESSION.option.name
        return if (optionName.startsWith("show", ignoreCase = true)) {
            "Do not ${optionName.toLowerCase()}"
        } else {
            CodeInsightBundle.message("inlay.hints.disable.custom.option", optionName)
        }
    }

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.language != KotlinLanguage.INSTANCE) return false

        InlayParameterHintsExtension.forLanguage(file.language) ?: return false

        if (!EditorSettingsExternalizable.getInstance().isShowParameterNameHints || !HintType.LAMBDA_RETURN_EXPRESSION.enabled) {
            return false
        }

        return KotlinCodeHintsModel.getInstance(project).getExtensionInfoAtOffset(editor) != null
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        HintType.LAMBDA_RETURN_EXPRESSION.option.set(false)
        refreshAllOpenEditors()
    }

    override fun startInWriteAction(): Boolean = false
}
