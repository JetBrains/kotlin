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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.util.SmartList
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.reflect.KClass

// This class was originally created to make possible switching inspection off and using intention instead.
// Since IDEA 2017.1, it's possible to have inspection severity "No highlighting, only fix"
// thus making the original purpose useless.
// The class will probably not be deleted in the close future,
// but do not use them in your new code.
@Suppress("DEPRECATION")
@Deprecated("Please do not use for new inspections. Use AbstractKotlinInspection as base class for them")
abstract class IntentionBasedInspection<TElement : PsiElement>(
        private val intentionInfos: List<IntentionBasedInspection.IntentionData<TElement>>,
        protected open val problemText: String?
) : AbstractKotlinInspection() {

    constructor(
            intention: KClass<out SelfTargetingRangeIntention<TElement>>,
            problemText: String? = null
    ) : this(listOf(IntentionData(intention)), problemText)

    constructor(
            intention: KClass<out SelfTargetingRangeIntention<TElement>>,
            additionalChecker: (TElement, IntentionBasedInspection<TElement>) -> Boolean,
            problemText: String? = null
    ) : this(listOf(IntentionData(intention, additionalChecker)), problemText)

    constructor(
            intention: KClass<out SelfTargetingRangeIntention<TElement>>,
            additionalChecker: (TElement) -> Boolean,
            problemText: String? = null
    ) : this(listOf(IntentionData(intention, { element, _ -> additionalChecker(element) } )), problemText)



    data class IntentionData<TElement : PsiElement>(
            val intention: KClass<out SelfTargetingRangeIntention<TElement>>,
            val additionalChecker: (TElement, IntentionBasedInspection<TElement>) -> Boolean = { _, _ -> true }
    )

    open fun additionalFixes(element: TElement): List<LocalQuickFix>? = null

    open fun inspectionTarget(element: TElement): PsiElement? = null

    private fun PsiElement.toRange(baseElement: PsiElement): TextRange {
        val start = getStartOffsetIn(baseElement)
        return TextRange(start, start + endOffset - startOffset)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {

        val intentionsAndCheckers = intentionInfos.map {
            val instance = it.intention.constructors.single { it.parameters.isEmpty() } .call()
            instance.inspection = this
            instance to it.additionalChecker
        }
        val elementType = intentionsAndCheckers.map { it.first.elementType }.distinct().singleOrNull()
                          ?: error("$intentionInfos should have the same elementType")

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!elementType.isInstance(element) || element.textLength == 0) return

                @Suppress("UNCHECKED_CAST")
                val targetElement = element as TElement

                var problemRange: TextRange? = null
                var fixes: SmartList<LocalQuickFix>? = null

                for ((intention, additionalChecker) in intentionsAndCheckers) {
                    val range = intention.applicabilityRange(targetElement)?.let { range ->
                        val elementRange = targetElement.textRange
                        assert(range in elementRange) { "Wrong applicabilityRange() result for $intention - should be within element's range" }
                        range.shiftRight(-elementRange.startOffset)
                    }

                    if (range != null && additionalChecker(targetElement, this@IntentionBasedInspection)) {
                        problemRange = problemRange?.union(range) ?: range
                        if (fixes == null) {
                            fixes = SmartList<LocalQuickFix>()
                        }
                        fixes.add(createQuickFix(intention, additionalChecker, targetElement))
                    }
                }

                val range = inspectionTarget(targetElement)?.toRange(element) ?: problemRange
                if (range != null) {
                    val allFixes = fixes ?: SmartList<LocalQuickFix>()
                    additionalFixes(targetElement)?.let { allFixes.addAll(it) }
                    if (!allFixes.isEmpty()) {
                        holder.registerProblem(targetElement, problemText ?: allFixes.first().name,
                                               problemHighlightType(targetElement), range, *allFixes.toTypedArray())
                    }
                }
            }
        }
    }

    protected open fun problemHighlightType(element: TElement): ProblemHighlightType =
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    private fun createQuickFix(
            intention: SelfTargetingRangeIntention<TElement>,
            additionalChecker: (TElement, IntentionBasedInspection<TElement>) -> Boolean,
            targetElement: TElement
    ): IntentionBasedQuickFix {
        return when (intention) {
            is LowPriorityAction -> LowPriorityIntentionBasedQuickFix(intention, additionalChecker, targetElement)
            is HighPriorityAction -> HighPriorityIntentionBasedQuickFix(intention, additionalChecker, targetElement)
            else -> IntentionBasedQuickFix(intention, additionalChecker, targetElement)
        }
    }

    /* we implement IntentionAction to provide isAvailable which will be used to hide outdated items and make sure we never call 'invoke' for such item */
    internal open inner class IntentionBasedQuickFix(
            val intention: SelfTargetingRangeIntention<TElement>,
            private val additionalChecker: (TElement, IntentionBasedInspection<TElement>) -> Boolean,
            targetElement: TElement
    ) : LocalQuickFixOnPsiElement(targetElement), IntentionAction {

        private val text = intention.text

        // store text into variable because intention instance is shared and may change its text later
        override fun getFamilyName() = intention.familyName

        override fun getText(): String = text

        override fun startInWriteAction() = intention.startInWriteAction()

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = isAvailable()

        override fun isAvailable(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement): Boolean {
            assert(startElement == endElement)
            return intention.applicabilityRange(startElement as TElement) != null && additionalChecker(startElement, this@IntentionBasedInspection)
        }

        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            applyFix()
        }

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            assert(startElement == endElement)
            if (!isAvailable(project, file, startElement, endElement)) return
            if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

            val editor = startElement.findExistingEditor()
            editor?.caretModel?.moveToOffset(startElement.textOffset)
            intention.applyTo(startElement as TElement, editor)
        }
    }

    private inner class LowPriorityIntentionBasedQuickFix(
            intention: SelfTargetingRangeIntention<TElement>,
            additionalChecker: (TElement, IntentionBasedInspection<TElement>) -> Boolean,
            targetElement: TElement
    ) : IntentionBasedQuickFix(intention, additionalChecker, targetElement), LowPriorityAction

    private inner class HighPriorityIntentionBasedQuickFix(
            intention: SelfTargetingRangeIntention<TElement>,
            additionalChecker: (TElement, IntentionBasedInspection<TElement>) -> Boolean,
            targetElement: TElement
    ) : IntentionBasedQuickFix(intention, additionalChecker, targetElement), HighPriorityAction
}

fun PsiElement.findExistingEditor(): Editor? {
    val file = containingFile?.virtualFile ?: return null
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return null

    val editorFactory = EditorFactory.getInstance()

    val editors = editorFactory.getEditors(document)
    return if (editors.isEmpty()) null else editors[0]
}
