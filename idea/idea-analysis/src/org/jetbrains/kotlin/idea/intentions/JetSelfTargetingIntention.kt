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

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.psiUtil.containsInside
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.HashMap

public abstract class JetSelfTargetingIntention<TElement : JetElement>(
        public val elementType: Class<TElement>,
        private var text: String,
        private val familyName: String = text
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
            elementsToCheck += leaf1.parentsWithSelf.takeWhile { it != commonParent }
        }
        if (leaf2 != null) {
            elementsToCheck += leaf2.parentsWithSelf.takeWhile { it != commonParent }
        }
        if (commonParent != null) {
            elementsToCheck += commonParent.parentsWithSelf
        }

        for (element in elementsToCheck) {
            @suppress("UNCHECKED_CAST")
            if (elementType.isInstance(element) && isApplicableTo(element as TElement, offset)) {
                return element as TElement
            }
            if (!allowCaretInsideElement(element) && element.getTextRange().containsInside(offset)) break
        }
        return null
    }

    protected open fun allowCaretInsideElement(element: PsiElement): Boolean = true

    final override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val target = getTarget(editor, file) ?: return false
        return !isIntentionBaseInspectionEnabled(project, target)
    }

    private fun isIntentionBaseInspectionEnabled(project: Project, target: TElement): Boolean {
        val inspection = findInspection(javaClass) ?: return false

        val key = HighlightDisplayKey.find(inspection.shortName)
        if (!InspectionProjectProfileManager.getInstance(project).getInspectionProfile(target).isToolEnabled(key)) return false

        return inspection.intentions.single { it.intention.javaClass == javaClass }.additionalChecker(target)
    }

    final override fun invoke(project: Project, editor: Editor, file: PsiFile): Unit {
        val target = getTarget(editor, file) ?: error("Intention is not applicable")
        applyTo(target, editor)
    }

    override fun startInWriteAction() = true

    override fun toString(): String = getText()

    companion object {
        private val intentionBasedInspections = HashMap<Class<out JetSelfTargetingIntention<*>>, IntentionBasedInspection<*>?>()

        fun <TElement : JetElement> findInspection(intentionClass: Class<out JetSelfTargetingIntention<TElement>>): IntentionBasedInspection<TElement>? {
            if (intentionBasedInspections.containsKey(intentionClass)) {
                @suppress("UNCHECKED_CAST")
                return intentionBasedInspections[intentionClass] as IntentionBasedInspection<TElement>?
            }

            for (extension in Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION)) {
                val inspection = extension.instance as? IntentionBasedInspection<*> ?: continue
                if (inspection.intentions.any { it.intention.javaClass == intentionClass }) {
                    intentionBasedInspections[intentionClass] = inspection
                    @suppress("UNCHECKED_CAST")
                    return inspection as IntentionBasedInspection<TElement>
                }
            }

            return null
        }
    }
}

public abstract class JetSelfTargetingRangeIntention<TElement : JetElement>(
        elementType: Class<TElement>,
        text: String,
        familyName: String = text
) : JetSelfTargetingIntention<TElement>(elementType, text, familyName) {

    public abstract fun applicabilityRange(element: TElement): TextRange?

    override final fun isApplicableTo(element: TElement, caretOffset: Int): Boolean {
        val range = applicabilityRange(element) ?: return false
        return range.containsOffset(caretOffset)
    }
}

public abstract class JetSelfTargetingOffsetIndependentIntention<TElement : JetElement>(
        elementType: Class<TElement>,
        text: String,
        familyName: String = text
) : JetSelfTargetingRangeIntention<TElement>(elementType, text, familyName) {

    public abstract fun isApplicableTo(element: TElement): Boolean

    override final fun applicabilityRange(element: TElement): TextRange? {
        return if (isApplicableTo(element)) element.getTextRange() else null
    }
}
