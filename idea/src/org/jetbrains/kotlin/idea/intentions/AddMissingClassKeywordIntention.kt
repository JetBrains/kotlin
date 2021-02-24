/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class AddMissingClassKeywordIntention : SelfTargetingIntention<PsiElement>(
    PsiElement::class.java,
    KotlinBundle.lazyMessage("add.missing.class.keyword")
) {
    companion object {
        private val targetKeywords = listOf(
            KtTokens.ANNOTATION_KEYWORD,
            KtTokens.DATA_KEYWORD,
            KtTokens.SEALED_KEYWORD,
            KtTokens.INNER_KEYWORD
        )
    }

    override fun isApplicableTo(element: PsiElement, caretOffset: Int): Boolean {
        if (element.node?.elementType != KtTokens.IDENTIFIER) return false
        val ktClass = element.parent as? KtClass
        if (ktClass == null) {
            val errorElement = element.parent as? PsiErrorElement ?: return false
            val modifierList = errorElement.getPrevSiblingIgnoringWhitespaceAndComments() as? KtModifierList ?: return false
            return targetKeywords.any { modifierList.hasModifier(it) }
        } else {
            return ktClass.isEnum() && ktClass.getClassKeyword() == null
        }
    }

    override fun applyTo(element: PsiElement, editor: Editor?) {
        val document = editor?.document ?: return
        val targetElement = (element.parent as? PsiErrorElement) ?: element
        document.insertString(targetElement.startOffset, "class ")
        PsiDocumentManager.getInstance(element.project).commitDocument(document)
    }
}
