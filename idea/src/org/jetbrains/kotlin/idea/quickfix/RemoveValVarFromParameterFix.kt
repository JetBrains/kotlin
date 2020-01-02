/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

class RemoveValVarFromParameterFix(element: KtValVarKeywordOwner) : KotlinQuickFixAction<KtValVarKeywordOwner>(element) {
    private val varOrVal: String

    init {
        val valOrVarNode =
            element.valOrVarKeyword ?: throw AssertionError("Val or var node not found for " + element.getElementTextWithContext())
        varOrVal = valOrVarNode.text
    }

    override fun getFamilyName() = "Remove 'val/var' from parameter"

    override fun getText(): String {
        val element = element ?: return ""
        val varOrVal = element.valOrVarKeyword?.text ?: return familyName
        return "Remove '$varOrVal' from parameter"
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.valOrVarKeyword?.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
            RemoveValVarFromParameterFix(diagnostic.psiElement.parent as KtValVarKeywordOwner)
    }
}
