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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.containsInside
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.*
import kotlin.reflect.KClass

abstract class SelfTargetingIntention<TElement : PsiElement>(
        val elementType: Class<TElement>,
        private var text: String,
        private val familyName: String = text
) : IntentionAction {

    protected val defaultText: String = text

    protected fun setText(text: String) {
        this.text = text
    }

    final override fun getText() = text
    final override fun getFamilyName() = familyName

    abstract fun isApplicableTo(element: TElement, caretOffset: Int): Boolean

    abstract fun applyTo(element: TElement, editor: Editor?)

    private fun getTarget(editor: Editor, file: PsiFile): TElement? {
        val offset = editor.caretModel.offset
        val leaf1 = file.findElementAt(offset)
        val leaf2 = file.findElementAt(offset - 1)
        val commonParent = if (leaf1 != null && leaf2 != null) PsiTreeUtil.findCommonParent(leaf1, leaf2) else null

        var elementsToCheck: Sequence<PsiElement> = emptySequence()
        if (leaf1 != null) {
            elementsToCheck += leaf1.parentsWithSelf.takeWhile { it != commonParent }
        }
        if (leaf2 != null) {
            elementsToCheck += leaf2.parentsWithSelf.takeWhile { it != commonParent }
        }
        if (commonParent != null && commonParent !is PsiFile) {
            elementsToCheck += commonParent.parentsWithSelf
        }

        for (element in elementsToCheck) {
            @Suppress("UNCHECKED_CAST")
            if (elementType.isInstance(element) && isApplicableTo(element as TElement, offset)) {
                return element
            }
            if (!allowCaretInsideElement(element) && element.textRange.containsInside(offset)) break
        }
        return null
    }

    protected open fun allowCaretInsideElement(element: PsiElement): Boolean =
            element !is KtBlockExpression

    final override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val target = getTarget(editor, file) ?: return false
        return !isIntentionBaseInspectionEnabled(project, target)
    }

    protected fun isIntentionBaseInspectionEnabled(project: Project, target: TElement): Boolean {
        val inspection = findInspection(this.javaClass.kotlin) ?: return false

        val key = HighlightDisplayKey.find(inspection.shortName)
        if (!InspectionProjectProfileManager.getInstance(project).getInspectionProfile(target).isToolEnabled(key)) {
            return false
        }

        return inspection.intentionInfos.single { it.intention == this.javaClass.kotlin }.additionalChecker(target, inspection)
    }

    final override fun invoke(project: Project, editor: Editor, file: PsiFile): Unit {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val target = getTarget(editor, file) ?: return
        applyTo(target, editor)
    }

    override fun startInWriteAction() = true

    override fun toString(): String = getText()

    companion object {
        private val intentionBasedInspections = HashMap<KClass<out SelfTargetingIntention<*>>, IntentionBasedInspection<*>?>()

        fun <TElement : PsiElement> findInspection(intentionClass: KClass<out SelfTargetingIntention<TElement>>): IntentionBasedInspection<TElement>? {
            if (intentionBasedInspections.containsKey(intentionClass)) {
                @Suppress("UNCHECKED_CAST")
                return intentionBasedInspections[intentionClass] as IntentionBasedInspection<TElement>?
            }

            for (extension in Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION)) {
                val inspection = extension.instance as? IntentionBasedInspection<*> ?: continue
                if (inspection.intentionInfos.any { it.intention == intentionClass }) {
                    intentionBasedInspections[intentionClass] = inspection
                    @Suppress("UNCHECKED_CAST")
                    return inspection as IntentionBasedInspection<TElement>
                }
            }

            return null
        }
    }
}

abstract class SelfTargetingRangeIntention<TElement : PsiElement>(
        elementType: Class<TElement>,
        text: String,
        familyName: String = text
) : SelfTargetingIntention<TElement>(elementType, text, familyName) {

    abstract fun applicabilityRange(element: TElement): TextRange?

    override final fun isApplicableTo(element: TElement, caretOffset: Int): Boolean {
        val range = applicabilityRange(element) ?: return false
        return range.containsOffset(caretOffset)
    }
}

abstract class SelfTargetingOffsetIndependentIntention<TElement : KtElement>(
        elementType: Class<TElement>,
        text: String,
        familyName: String = text
) : SelfTargetingRangeIntention<TElement>(elementType, text, familyName) {

    abstract fun isApplicableTo(element: TElement): Boolean

    override final fun applicabilityRange(element: TElement): TextRange? {
        return if (isApplicableTo(element)) element.textRange else null
    }
}
