/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.idea.completion.handlers.indexOfSkippingSpace
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.moveCaretIntoGeneratedElement
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import javax.swing.Icon

class OverridesCompletionLookupElementDecorator(
    lookupElement: LookupElement,
    private val declaration: KtCallableDeclaration?,
    private val text: String,
    private val isImplement: Boolean,
    private val icon: RowIcon,
    private val baseClassName: String,
    private val baseClassIcon: Icon?,
    private val isConstructorParameter: Boolean,
    private val isSuspend: Boolean,
    private val generateMember: () -> KtCallableDeclaration,
    private val shortenReferences: (KtElement) -> Unit,
) : LookupElementDecorator<LookupElement>(lookupElement) {
    override fun getLookupString() =
        if (declaration == null) "override" else delegate.lookupString // don't use "override" as lookup string when already in the name of declaration

    override fun getAllLookupStrings() = setOf(lookupString, delegate.lookupString)

    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)

        presentation.itemText = text
        presentation.isItemTextBold = isImplement
        presentation.icon = icon
        presentation.clearTail()
        presentation.setTypeText(baseClassName, baseClassIcon)
    }

    override fun handleInsert(context: InsertionContext) {
        val dummyMemberHead = when {
            declaration != null -> ""
            isConstructorParameter -> "override val "
            else -> "override fun "
        }
        val dummyMemberTail = when {
            isConstructorParameter || declaration is KtProperty -> "dummy: Dummy ,@"
            else -> "dummy() {}"
        }
        val dummyMemberText = dummyMemberHead + dummyMemberTail
        val override = KtTokens.OVERRIDE_KEYWORD.value

        tailrec fun calcStartOffset(startOffset: Int, diff: Int = 0): Int {
            return when {
                context.document.text[startOffset - 1].isWhitespace() -> calcStartOffset(startOffset - 1, diff + 1)
                context.document.text.substring(startOffset - override.length, startOffset) == override -> {
                    startOffset - override.length
                }
                else -> diff + startOffset
            }
        }

        val startOffset = calcStartOffset(context.startOffset)
        val tailOffset = context.tailOffset
        context.document.replaceString(startOffset, tailOffset, dummyMemberText)

        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()

        val dummyMember = context.file.findElementAt(startOffset)!!.getStrictParentOfType<KtNamedDeclaration>()!!

        // keep original modifiers
        val modifierList = KtPsiFactory(context.project).createModifierList(dummyMember.modifierList!!.text)

        val prototype = generateMember()
        prototype.modifierList!!.replace(modifierList)
        val insertedMember = dummyMember.replaced(prototype)
        if (isSuspend) insertedMember.addModifier(KtTokens.SUSPEND_KEYWORD)

        shortenReferences(insertedMember)

        if (isConstructorParameter) {
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

            val offset = insertedMember.endOffset
            val chars = context.document.charsSequence
            val commaOffset = chars.indexOfSkippingSpace(',', offset)!!
            val atCharOffset = chars.indexOfSkippingSpace('@', commaOffset + 1)!!
            context.document.deleteString(offset, atCharOffset + 1)

            context.editor.moveCaret(offset)
        } else {
            moveCaretIntoGeneratedElement(context.editor, insertedMember)
        }
    }
}