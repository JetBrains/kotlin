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

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.JetElement

public abstract class IntentionBasedInspection<T: JetElement>(
        protected val intentions: List<JetSelfTargetingRangeIntention<T>>,
        protected val elementType: Class<T>
) : AbstractKotlinInspection() {
    constructor(intention: JetSelfTargetingRangeIntention<T>): this(listOf(intention), intention.elementType)

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object: PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!elementType.isInstance(element) || element.getTextLength() == 0) return

                [suppress("UNCHECKED_CAST")]
                val targetElement = element as T

                for (intention in intentions) {
                    val range = intention.applicabilityRange(targetElement) ?: continue
                    val elementRange = targetElement.getTextRange()
                    assert(range in elementRange, "Wrong applicabilityRange() result for $intention - should be within element's range")
                    val rangeInElement = range.shiftRight(-elementRange.getStartOffset())

                    val fix = object: LocalQuickFix {
                        private val text = intention.getText()

                        override fun getFamilyName() = getName()

                        override fun getName() = text

                        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                            targetElement.getOrCreateEditor()?.let { editor ->
                                editor.getCaretModel().moveToOffset(targetElement.getTextOffset())
                                intention.applyTo(targetElement, editor)
                            }
                        }
                    }

                    holder.registerProblem(targetElement, intention.getText(), problemHighlightType, rangeInElement, fix)
                }
            }
        }
    }

    protected open val problemHighlightType: ProblemHighlightType
        get() = ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    private fun PsiElement.getOrCreateEditor(): Editor? {
        val file = getContainingFile()?.getVirtualFile() ?: return null
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return null

        val editorFactory = EditorFactory.getInstance()

        val editors = editorFactory.getEditors(document)
        return if (editors.isEmpty()) editorFactory.createEditor(document) else editors[0]
    }
}
