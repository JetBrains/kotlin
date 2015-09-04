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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class AddNameToArgumentFix(argument: JetValueArgument, private val possibleNames: List<Name>) : JetIntentionAction<JetValueArgument>(argument) {

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        if (possibleNames.size() == 1 || editor == null || !editor.component.isShowing) {
            addName(project, element, possibleNames.first())
        }
        else {
            chooseNameAndAdd(project, editor)
        }
    }

    private fun chooseNameAndAdd(project: Project, editor: Editor) {
        JBPopupFactory.getInstance().createListPopup(getNamePopup(project)).showInBestPositionFor(editor)
    }

    private fun getNamePopup(project: Project): ListPopupStep<Name> {
        return object : BaseListPopupStep<Name>("Choose parameter name", possibleNames) {
            override fun onChosen(selectedValue: Name, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    addName(project, element, selectedValue)
                }
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(name: Name) = JetIcons.PARAMETER

            override fun getTextFor(name: Name) = getParsedArgumentWithName(name, element).text
        }
    }

    override fun getText(): String {
        return possibleNames
                       .singleOrNull()
                       ?.let { "Add name to argument: '${getParsedArgumentWithName(it, element).text}'" }
               ?: "Add name to argument..."
    }

    override fun getFamilyName() = "Add Name to Argument"

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val argument = diagnostic.psiElement.getParentOfType<JetValueArgument>(false) ?: return null
            val possibleNames = generatePossibleNames(argument)
            if (possibleNames.isEmpty()) return null
            return AddNameToArgumentFix(argument, possibleNames)
        }

        private fun generatePossibleNames(argument: JetValueArgument): List<Name> {
            val callElement = argument.getParentOfType<JetCallElement>(true) ?: return emptyList()

            val context = argument.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = callElement.getResolvedCall(context) ?: return emptyList()

            val argumentType = argument.getArgumentExpression()?.let { context.getType(it) }

            val usedParameters = resolvedCall.call.valueArguments
                    .map { resolvedCall.getArgumentMapping(it) }
                    .filterIsInstance<ArgumentMatch>()
                    .filter { argumentMatch -> argumentType == null || argumentType.isError || !argumentMatch.isError() }
                    .map { it.valueParameter }
                    .toSet()

            return resolvedCall.resultingDescriptor.valueParameters
                    .filter { it !in usedParameters }
                    .map { it.name }
        }

        private fun addName(project: Project, argument: JetValueArgument, name: Name) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            project.executeWriteCommand("Add name to argument...") {
                val newArgument = getParsedArgumentWithName(name, argument)
                argument.replace(newArgument)
            }
        }

        private fun getParsedArgumentWithName(name: Name, argument: JetValueArgument): JetValueArgument {
            val argumentExpression = argument.getArgumentExpression()
                                     ?: error("Argument should be already parsed.")
            return JetPsiFactory(argument).createArgument(argumentExpression, name, argument.getSpreadElement() != null)
        }
    }
}
