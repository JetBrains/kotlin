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

import com.intellij.codeInsight.intention.IntentionAction
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

abstract class IntentionBasedInspection<TElement : PsiElement>(
        val intentions: List<IntentionBasedInspection.IntentionData<TElement>>,
        protected val problemText: String?,
        protected val elementType: Class<TElement>
) : AbstractKotlinInspection() {

    constructor(intention: SelfTargetingRangeIntention<TElement>, additionalChecker: (TElement) -> Boolean = { true })
    : this(listOf(IntentionData(intention, additionalChecker)), null, intention.elementType)

    data class IntentionData<TElement : PsiElement>(
            val intention: SelfTargetingRangeIntention<TElement>,
            val additionalChecker: (TElement) -> Boolean = { true }
    )

    open fun additionalFixes(element: TElement): List<LocalQuickFix>? = null

    open fun inspectionRange(element: TElement): TextRange? = null

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!elementType.isInstance(element) || element.textLength == 0) return

                @Suppress("UNCHECKED_CAST")
                val targetElement = element as TElement

                var problemRange: TextRange? = null
                var fixes: SmartList<LocalQuickFix>? = null

                for ((intention, additionalChecker) in intentions) {
                    synchronized(intention) {
                        val range = intention.applicabilityRange(targetElement)?.let { range ->
                            val elementRange = targetElement.textRange
                            assert(range in elementRange) { "Wrong applicabilityRange() result for $intention - should be within element's range" }
                            range.shiftRight(-elementRange.startOffset)
                        }

                        if (range != null && additionalChecker(targetElement)) {
                            problemRange = problemRange?.union(range) ?: range
                            if (fixes == null) {
                                fixes = SmartList<LocalQuickFix>()
                            }
                            fixes!!.add(IntentionBasedQuickFix(intention, intention.text, additionalChecker, targetElement))
                        }
                    }
                }

                val range = inspectionRange(targetElement) ?: problemRange
                if (range != null) {
                    val allFixes = fixes ?: SmartList<LocalQuickFix>()
                    additionalFixes(targetElement)?.let { allFixes.addAll(it) }
                    if (!allFixes.isEmpty()) {
                        holder.registerProblem(targetElement, problemText ?: allFixes.first().name,
                                               problemHighlightType, range, *allFixes.toTypedArray())
                    }
                }
            }
        }
    }

    protected open val problemHighlightType: ProblemHighlightType
        get() = ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    /* we implement IntentionAction to provide isAvailable which will be used to hide outdated items and make sure we never call 'invoke' for such item */
    private class IntentionBasedQuickFix<TElement : PsiElement>(
            private val intention: SelfTargetingRangeIntention<TElement>,
            private val text: String,
            private val additionalChecker: (TElement) -> Boolean,
            targetElement: TElement
    ) : LocalQuickFixOnPsiElement(targetElement), IntentionAction {

        // store text into variable because intention instance is shared and may change its text later
        override fun getFamilyName() = intention.familyName

        override fun getText(): String = text

        override fun startInWriteAction() = true

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = isAvailable()

        override fun isAvailable(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement): Boolean {
            assert(startElement == endElement)
            return intention.applicabilityRange(startElement as TElement) != null && additionalChecker(startElement)
        }

        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            applyFix()
        }

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            assert(startElement == endElement)
            if (!isAvailable(project, file, startElement, endElement)) return

            val editor = startElement.findExistingEditor()
            editor?.caretModel?.moveToOffset(startElement.textOffset)
            intention.applyTo(startElement as TElement, editor)
        }
    }
}

fun PsiElement.findExistingEditor(): Editor? {
    val file = containingFile?.virtualFile ?: return null
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return null

    val editorFactory = EditorFactory.getInstance()

    val editors = editorFactory.getEditors(document)
    return if (editors.isEmpty()) null else editors[0]
}
