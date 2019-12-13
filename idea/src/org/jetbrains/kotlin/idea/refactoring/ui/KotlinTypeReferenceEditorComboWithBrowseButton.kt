/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
            } else {
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