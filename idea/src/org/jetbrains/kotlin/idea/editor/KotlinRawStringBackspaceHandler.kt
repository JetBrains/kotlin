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

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinRawStringBackspaceHandler : BackspaceHandlerDelegate() {
    var ele: SmartPsiElementPointer<KtStringTemplateExpression>? = null

    override fun beforeCharDeleted(c: Char, file: PsiFile?, editor: Editor?) {
        ele = null
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) {
            return
        }
        if (file !is KtFile) {
            return
        }
        if (editor != null) {
            val offset = editor.caretModel.offset
            val psiElement = file.findElementAt(offset) ?: return

            val possibleString: KtStringTemplateExpression = getStringTemplate(psiElement) ?: return
            if (possibleString.text == "\"\"\"\"\"\"") {
                ele = possibleString.createSmartPointer()
            }
        }
    }

    override fun charDeleted(c: Char, file: PsiFile?, editor: Editor?): Boolean {
        ele?.element?.let {
            editor?.document?.deleteString(it.startOffset, it.endOffset - 1)
            ele = null
            return true
        }
        return false
    }

    private fun getStringTemplate(psiElement: PsiElement): KtStringTemplateExpression? {
        if (psiElement is KtStringTemplateExpression) {
            return psiElement
        }
        val sibling = psiElement.prevSibling ?: return null
        if (sibling is KtStringTemplateExpression) {
            return sibling
        }

        var sibling2 = sibling.lastChild
        if (sibling2 is KtStringTemplateExpression) {
            return sibling2
        }

        sibling2 = sibling.parent
        if (sibling2 is KtStringTemplateExpression) {
            return sibling2
        }

        return null
    }
}