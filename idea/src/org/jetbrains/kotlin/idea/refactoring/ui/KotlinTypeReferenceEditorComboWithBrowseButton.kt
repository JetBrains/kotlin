/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.refactoring.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.EditorComboBox
import com.intellij.ui.RecentsManager
import com.intellij.ui.TextAccessor
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeCodeFragment
import java.awt.event.ActionListener

class KotlinTypeReferenceEditorComboWithBrowseButton(
        browseActionListener: ActionListener,
        text: String?,
        contextElement: PsiElement,
        recentsKey: String
) : ComponentWithBrowseButton<EditorComboBox>(
        EditorComboBox(createDocument(text, contextElement), contextElement.project, KotlinFileType.INSTANCE),
        browseActionListener
), TextAccessor {
    companion object {
        private fun createDocument(text: String?, contextElement: PsiElement): Document? {
            val codeFragment = KtPsiFactory(contextElement).createTypeCodeFragment(text ?: "", contextElement)
            return PsiDocumentManager.getInstance(contextElement.project).getDocument(codeFragment)
        }
    }

    private val project = contextElement.project

    init {
        RecentsManager.getInstance(contextElement.project).getRecentEntries(recentsKey)?.let {
            childComponent.setHistory(ArrayUtil.toStringArray(it))
        }

        if (text != null) {
            if (text.isNotEmpty()) {
                childComponent.prependItem(text)
            }
            else {
                childComponent.selectedItem = null
            }
        }
    }

    override fun getText() = childComponent.text.trim { it <= ' ' }

    override fun setText(text: String) {
        childComponent.text = text
    }

    val codeFragment: KtTypeCodeFragment?
        get() = PsiDocumentManager.getInstance(project).getPsiFile(childComponent.document) as? KtTypeCodeFragment
}