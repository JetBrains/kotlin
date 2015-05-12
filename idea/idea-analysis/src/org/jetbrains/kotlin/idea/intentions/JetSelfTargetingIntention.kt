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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.psiUtil.parents

public abstract class JetSelfTargetingIntention<TElement : JetElement>(
        public val elementType: Class<TElement>,
        private var text: String,
        private val familyName: String = text,
        private val firstElementOfTypeOnly: Boolean = false
) : IntentionAction {

    protected val defaultText: String = text

    protected fun setText(text: String) {
        this.text = text
    }

    final override fun getText() = text
    final override fun getFamilyName() = familyName

    public abstract fun isApplicableTo(element: TElement, caretOffset: Int): Boolean

    public abstract fun applyTo(element: TElement, editor: Editor)

    private fun getTarget(editor: Editor, file: PsiFile): TElement? {
        val offset = editor.getCaretModel().getOffset()
        val leaf1 = file.findElementAt(offset)
        val leaf2 = file.findElementAt(offset - 1)
        val commonParent = if (leaf1 != null && leaf2 != null) PsiTreeUtil.findCommonParent(leaf1, leaf2) else null

        var elementsToCheck: Sequence<PsiElement> = sequence { null }
        if (leaf1 != null) {
            elementsToCheck += leaf1.parents().takeWhile { it != commonParent }
        }
        if (leaf2 != null) {
            elementsToCheck += leaf2.parents().takeWhile { it != commonParent }
        }
        if (commonParent != null) {
            elementsToCheck += commonParent.parents()
        }

        val elementsOfType = elementsToCheck.filterIsInstance(elementType)
        if (firstElementOfTypeOnly) {
            val candidate = elementsOfType.firstOrNull() ?: return null
            return if (isApplicableTo(candidate, offset)) candidate else null
        }
        else {
            return elementsOfType.firstOrNull { isApplicableTo(it, offset) }
        }
    }

    final override fun isAvailable(project: Project, editor: Editor, file: PsiFile)
            = getTarget(editor, file) != null

    final override fun invoke(project: Project, editor: Editor, file: PsiFile): Unit {
        val target = getTarget(editor, file) ?: error("Intention is not applicable")
        applyTo(target, editor)
    }

    override fun startInWriteAction() = true

    override fun toString(): String = getText()
}

public abstract class JetSelfTargetingRangeIntention<TElement : JetElement>(
        elementType: Class<TElement>,
        text: String,
        familyName: String = text,
        firstElementOfTypeOnly: Boolean = false
) : JetSelfTargetingIntention<TElement>(elementType, text, familyName, firstElementOfTypeOnly) {

    public abstract fun applicabilityRange(element: TElement): TextRange?

    override final fun isApplicableTo(element: TElement, caretOffset: Int): Boolean {
        val range = applicabilityRange(element) ?: return false
        return range.containsOffset(caretOffset)
    }
}

public abstract class JetSelfTargetingOffsetIndependentIntention<TElement : JetElement>(
        elementType: Class<TElement>,
        text: String,
        familyName: String = text,
        firstElementOfTypeOnly: Boolean = false
) : JetSelfTargetingRangeIntention<TElement>(elementType, text, familyName, firstElementOfTypeOnly) {

    public abstract fun isApplicableTo(element: TElement): Boolean

    override final fun applicabilityRange(element: TElement): TextRange? {
        return if (isApplicableTo(element)) element.getTextRange() else null
    }
}
