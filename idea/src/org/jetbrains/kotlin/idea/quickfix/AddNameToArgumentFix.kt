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

import com.google.common.collect.Lists
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.mapArgumentsToParameters
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.checker.JetTypeChecker

public class AddNameToArgumentFix(argument: JetValueArgument, private val possibleNames: List<String>) : JetIntentionAction<JetValueArgument>(argument) {

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        if (possibleNames.size() == 1 || editor == null || !editor.component.isShowing) {
            addName(project, element, possibleNames.get(0))
        }
        else {
            chooseNameAndAdd(project, editor)
        }
    }

    private fun chooseNameAndAdd(project: Project, editor: Editor) {
        JBPopupFactory.getInstance().createListPopup(getNamePopup(project)).showInBestPositionFor(editor)
    }

    private fun getNamePopup(project: Project): ListPopupStep<String> {
        return object : BaseListPopupStep<String>(JetBundle.message("add.name.to.parameter.name.chooser.title"), possibleNames) {
            override fun onChosen(selectedName: String, finalChoice: Boolean): PopupStep<Any> {
                if (finalChoice) {
                    addName(project, element, selectedName)
                }
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(name: String) = JetIcons.PARAMETER

            override fun getTextFor(name: String) = getParsedArgumentWithName(name, element).text
        }
    }

    override fun getText(): String {
        return possibleNames
                       .singleOrNull()
                       ?.let { "Add name to argument: '${getParsedArgumentWithName(it, element).text}'" }
               ?: "Add name to argument..."
    }

    override fun getFamilyName() = "Add Name to Argument"

    companion object {
        private fun generatePossibleNames(argument: JetValueArgument): List<String> {
            val callElement = PsiTreeUtil.getParentOfType(argument, JetCallElement::class.java)
            assert(callElement != null, "The argument has to be inside a function or constructor call")

            val context = argument.analyze()
            val resolvedCall = callElement!!.getResolvedCall(context) ?: return emptyList()

            val callableDescriptor = resolvedCall.resultingDescriptor
            val argExpression = argument.getArgumentExpression()
            val type = if (argExpression != null) context.getType(argExpression) else null
            val usedParameters = resolvedCall.call.mapArgumentsToParameters(callableDescriptor).values().toSet()
            val names = Lists.newArrayList<String>()
            for (parameter in callableDescriptor.valueParameters) {
                if (!usedParameters.contains(parameter) && (type == null || JetTypeChecker.DEFAULT.isSubtypeOf(type, parameter.type))) {
                    names.add(parameter.name.asString())
                }
            }
            return names
        }

        private fun addName(project: Project, argument: JetValueArgument, name: String) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            CommandProcessor.getInstance().executeCommand(
                    project,
                    object : Runnable {
                        override fun run() {
                            ApplicationManager.getApplication().runWriteAction(object : Runnable {
                                override fun run() {
                                    val newArgument = getParsedArgumentWithName(name, argument)
                                    argument.replace(newArgument)
                                }
                            })
                        }
                    },
                    "Add name to argument...",
                    null)
        }

        private fun getParsedArgumentWithName(name: String, argument: JetValueArgument): JetValueArgument {
            val argumentExpression = argument.getArgumentExpression()
            assert(argumentExpression != null, "Argument should be already parsed.")
            return JetPsiFactory(argument).createArgument(argumentExpression!!, Name.identifier(name), argument.getSpreadElement() != null)
        }

        public fun createFactory(): JetIntentionActionsFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val argument = QuickFixUtil.getParentElementOfType(diagnostic, JetValueArgument::class.java) ?: return null
                    val possibleNames = generatePossibleNames(argument)
                    if (possibleNames.isEmpty()) return null
                    return AddNameToArgumentFix(argument, possibleNames)
                }
            }
        }
    }
}
