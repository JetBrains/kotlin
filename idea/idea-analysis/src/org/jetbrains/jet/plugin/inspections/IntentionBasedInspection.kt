/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.inspections

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.openapi.project.Project
import com.intellij.codeInspection.ProblemDescriptor
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Editor

public abstract class IntentionBasedInspection<T: JetElement>(
        protected val intention: JetSelfTargetingIntention<T>
) : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object: PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (!intention.elementType.isInstance(element)) return

                [suppress("UNCHECKED_CAST")]
                val targetElement = element as T

                if (!intention.isApplicableTo(targetElement)) return

                val fix = object: LocalQuickFix {
                    override fun getFamilyName(): String {
                        return getName()
                    }

                    override fun getName(): String {
                        return intention.getText()
                    }

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        targetElement.getOrCreateEditor()?.let { editor ->
                            editor.getCaretModel().moveToOffset(targetElement.getTextOffset())
                            intention.applyTo(targetElement, editor)
                        }
                    }
                }

                holder.registerProblem(targetElement, intention.getText(), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fix)
            }
        }
    }
}

private fun PsiElement.getOrCreateEditor(): Editor? {
    val file = getContainingFile()?.getVirtualFile()
    if (file == null) return null

    val document = FileDocumentManager.getInstance()!!.getDocument(file)
    if (document == null) return null

    val editorFactory = EditorFactory.getInstance()!!

    val editors = editorFactory.getEditors(document)
    return if (editors.isEmpty()) editorFactory.createEditor(document) else editors[0]
}
