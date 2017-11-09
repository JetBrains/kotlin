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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

class RemoveValVarFromParameterFix(element: KtValVarKeywordOwner) : KotlinQuickFixAction<KtValVarKeywordOwner>(element) {
    private val varOrVal: String

    init {
        val valOrVarNode = element.valOrVarKeyword
                           ?: throw AssertionError("Val or var node not found for " + element.getElementTextWithContext())
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
