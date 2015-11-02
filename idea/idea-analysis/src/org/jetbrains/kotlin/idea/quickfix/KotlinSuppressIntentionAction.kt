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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import com.intellij.codeInspection.SuppressIntentionAction
import org.jetbrains.kotlin.idea.util.PsiPrecedences
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public class KotlinSuppressIntentionAction(
        private val suppressAt: KtExpression,
        private val diagnosticFactory: DiagnosticFactory<*>,
        private val kind: AnnotationHostKind
) : SuppressIntentionAction() {

    override fun getFamilyName() = KotlinBundle.message("suppress.warnings.family")
    override fun getText() = KotlinBundle.message("suppress.warning.for", diagnosticFactory.getName(), kind.kind, kind.name)

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) = element.isValid()

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val id = "\"${diagnosticFactory.getName()}\""
        if (suppressAt is KtModifierListOwner) {
            suppressAtModifierListOwner(suppressAt, id)
        }
        else if (suppressAt is KtAnnotatedExpression) {
            suppressAtAnnotatedExpression(CaretBox(suppressAt, editor), id)
        }
        else if (suppressAt is KtExpression) {
            suppressAtExpression(CaretBox(suppressAt, editor), id)
        }
    }

    private fun suppressAtModifierListOwner(suppressAt: KtModifierListOwner, id: String) {
        val modifierList = suppressAt.getModifierList()
        val psiFactory = KtPsiFactory(suppressAt)
        if (modifierList == null) {
            // create a modifier list from scratch
            val newModifierList = psiFactory.createModifierList(suppressAnnotationText(id))
            val replaced = KtPsiUtil.replaceModifierList(suppressAt, newModifierList)
            val whiteSpace = psiFactory.createWhiteSpace(kind)
            suppressAt.addAfter(whiteSpace, replaced)
        }
        else {
            val entry = findSuppressAnnotation(suppressAt)
            if (entry == null) {
                // no [suppress] annotation
                val newAnnotation = psiFactory.createAnnotationEntry(suppressAnnotationText(id))
                val addedAnnotation = modifierList.addBefore(newAnnotation, modifierList.getFirstChild())
                val whiteSpace = psiFactory.createWhiteSpace(kind)
                modifierList.addAfter(whiteSpace, addedAnnotation)
            }
            else {
                // already annotated with [suppress]
                addArgumentToSuppressAnnotation(entry, id)
            }
        }
    }

    private fun suppressAtAnnotatedExpression(suppressAt: CaretBox<KtAnnotatedExpression>, id: String) {
        val entry = findSuppressAnnotation(suppressAt.expression)
        if (entry != null) {
            // already annotated with @suppress
            addArgumentToSuppressAnnotation(entry, id)
        }
        else {
            suppressAtExpression(suppressAt, id)
        }
    }

    private fun suppressAtExpression(caretBox: CaretBox<KtExpression>, id: String) {
        val suppressAt = caretBox.expression
        assert(suppressAt !is KtDeclaration) { "Declarations should have been checked for above" }

        val parentheses = PsiPrecedences.getPrecedence(suppressAt) > PsiPrecedences.PRECEDENCE_OF_PREFIX_EXPRESSION
        val placeholderText = "PLACEHOLDER_ID"
        val inner = if (parentheses) "($placeholderText)" else placeholderText
        val annotatedExpression = KtPsiFactory(suppressAt).createExpression(suppressAnnotationText(id) + "\n" + inner)

        val copy = suppressAt.copy()!!

        val afterReplace = suppressAt.replace(annotatedExpression) as KtAnnotatedExpression
        val toReplace = afterReplace.findElementAt(afterReplace.getTextLength() - 2)!!
        assert (toReplace.getText() == placeholderText)
        val result = toReplace.replace(copy)!!

        caretBox.positionCaretInCopy(result)
    }

    private fun addArgumentToSuppressAnnotation(entry: KtAnnotationEntry, id: String) {
        // add new arguments to an existing entry
        val args = entry.getValueArgumentList()
        val psiFactory = KtPsiFactory(entry)
        val newArgList = psiFactory.createCallArguments("($id)")
        if (args == null) {
            // new argument list
            entry.addAfter(newArgList, entry.getLastChild())
        }
        else if (args.getArguments().isEmpty()) {
            // replace '()' with a new argument list
            args.replace(newArgList)
        }
        else {
            args.addArgument(newArgList.getArguments()[0])
        }
    }

    private fun suppressAnnotationText(id: String) = "@Suppress($id)"

    private fun findSuppressAnnotation(annotated: KtAnnotated): KtAnnotationEntry? {
        val context = annotated.analyze()
        for (entry in annotated.getAnnotationEntries()) {
            val annotationDescriptor = context.get(BindingContext.ANNOTATION, entry)
            if (annotationDescriptor != null && KotlinBuiltIns.isSuppressAnnotation(annotationDescriptor)) {
                return entry
            }
        }
        return null
    }
}

public class AnnotationHostKind(val kind: String, val name: String, val newLineNeeded: Boolean)

private fun KtPsiFactory.createWhiteSpace(kind: AnnotationHostKind): PsiElement {
    return if (kind.newLineNeeded) createNewLine() else createWhiteSpace()
}

private class CaretBox<out E: KtExpression>(
        val expression: E,
        private val editor: Editor?
) {
    private val offsetInExpression: Int = (editor?.getCaretModel()?.getOffset() ?: 0) - expression.getTextRange()!!.getStartOffset()

    fun positionCaretInCopy(copy: PsiElement) {
        if (editor == null) return
        editor.getCaretModel().moveToOffset(copy.getTextOffset() + offsetInExpression)
    }
}
