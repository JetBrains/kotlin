/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.plugin.JetBundle
import java.util.Collections
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.codeInspection.SuppressIntentionAction

public class KotlinSuppressIntentionAction(
        private val suppressAt: JetDeclaration,
        private val diagnosticFactory: DiagnosticFactory,
        private val kind: DeclarationKind
) : SuppressIntentionAction() {

    override fun getFamilyName() = JetBundle.message("suppress.warnings.family")
    override fun getText() = JetBundle.message("suppress.warning.for", diagnosticFactory.getName(), kind.kind, kind.name)

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) = element.isValid()

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val id = "\"${diagnosticFactory.getName()}\""
        val modifierList = suppressAt.getModifierList()
        if (modifierList == null) {
            // create a modifier list from scratch
            val newModifierList = JetPsiFactory.createModifierList(project, "[suppress($id)]")
            val replaced = JetPsiUtil.replaceModifierList(suppressAt, newModifierList)
            val whiteSpace = project.createWhiteSpace(kind)
            suppressAt.addAfter(whiteSpace, replaced)
        }
        else {
            val entry = findSuppressAnnotation(modifierList)
            if (entry == null) {
                val newAnnotation = JetPsiFactory.createAnnotation(project, "[suppress($id)]")
                val addedAnnotation = modifierList.addBefore(newAnnotation, modifierList.getFirstChild())
                val whiteSpace = project.createWhiteSpace(kind)
                modifierList.addAfter(whiteSpace, addedAnnotation)
            }
            else {
                // add new arguments to an existing entry
                val args = entry.getValueArgumentList()
                val newArgList = JetPsiFactory.createCallArguments(project, "($id)")
                if (args == null) {
                    // new argument list
                    entry.addAfter(newArgList, entry.getLastChild())
                }
                else if (args.getArguments().isEmpty()) {
                    // replace '()' with a new argument list
                    args.replace(newArgList)
                }
                else {
                    val rightParen = args.getRightParenthesis()
                    args.addBefore(JetPsiFactory.createComma(project), rightParen)
                    args.addBefore(JetPsiFactory.createWhiteSpace(project), rightParen)
                    args.addBefore(newArgList.getArguments()[0], rightParen)
                }
            }
        }

    }

    private fun findSuppressAnnotation(modifierList: JetModifierList): JetAnnotationEntry? {
        val suppressAnnotationClass = KotlinBuiltIns.getInstance().getSuppressAnnotationClass()
        val context = AnalyzerFacadeWithCache.getContextForElement(modifierList)
        for (entry in modifierList.getAnnotationEntries()) {
            val annotationDescriptor = context.get(BindingContext.ANNOTATION, entry)
            if (annotationDescriptor != null && suppressAnnotationClass.getTypeConstructor() == annotationDescriptor.getType().getConstructor()) {
                return entry
            }
        }
        return null
    }
}

public class DeclarationKind(val kind: String, val name: String, val newLineNeeded: Boolean)
private fun Project.createWhiteSpace(kind: DeclarationKind): PsiElement =
        if (kind.newLineNeeded)
            JetPsiFactory.createNewLine(this)
        else
            JetPsiFactory.createWhiteSpace(this)