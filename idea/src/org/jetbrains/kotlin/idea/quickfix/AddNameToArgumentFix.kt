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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError

class AddNameToArgumentFix(argument: KtValueArgument) : KotlinQuickFixAction<KtValueArgument>(argument) {
    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        if (element.getArgumentExpression() == null) return false
        return calculatePossibleArgumentNames().isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val possibleNames = calculatePossibleArgumentNames()
        assert(possibleNames.isNotEmpty()) { "isAvailable() should be checked before invoke()" }
        if (possibleNames.size == 1 || editor == null || !editor.component.isShowing) {
            addName(project, element, possibleNames.first())
        } else {
            chooseNameAndAdd(project, editor, possibleNames)
        }
    }

    override fun getText(): String {
        return calculatePossibleArgumentNames()
            .singleOrNull()
            ?.let { "Add name to argument: '${createArgumentWithName(it, reformat = false).text}'" }
            ?: "Add name to argument..."
    }

    override fun getFamilyName() = "Add name to argument"

    private fun calculatePossibleArgumentNames(): List<Name> {
        val callElement = element!!.getParentOfType<KtCallElement>(true) ?: return emptyList()

        val context = element!!.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = callElement.getResolvedCall(context) ?: return emptyList()

        val argumentType = element!!.getArgumentExpression()?.let { context.getType(it) }

        val usedParameters = resolvedCall.call.valueArguments
            .asSequence()
            .map { resolvedCall.getArgumentMapping(it) }
            .filterIsInstance<ArgumentMatch>()
            .filter { argumentMatch -> argumentType == null || argumentType.isError || !argumentMatch.isError() }
            .map { it.valueParameter }
            .toSet()

        return resolvedCall.resultingDescriptor.valueParameters
            .asSequence()
            .filter { it !in usedParameters }
            .map { it.name }
            .toList()
    }

    private fun addName(project: Project, argument: KtValueArgument, name: Name) {
        project.executeWriteCommand("Add name to argument") {
            argument.replace(createArgumentWithName(name))
        }
    }

    private fun createArgumentWithName(name: Name, reformat: Boolean = true): KtValueArgument {
        val argumentExpression = element!!.getArgumentExpression()!!
        return KtPsiFactory(element!!).createArgument(argumentExpression, name, element!!.getSpreadElement() != null, reformat = reformat)
    }

    private fun chooseNameAndAdd(project: Project, editor: Editor, names: List<Name>) {
        JBPopupFactory.getInstance().createListPopup(getNamePopup(project, names)).showInBestPositionFor(editor)
    }

    private fun getNamePopup(project: Project, names: List<Name>): ListPopupStep<Name> {
        return object : BaseListPopupStep<Name>("Choose parameter name", names) {
            override fun onChosen(selectedValue: Name, finalChoice: Boolean): PopupStep<*>? {
                addName(project, element!!, selectedValue)
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(name: Name) = KotlinIcons.PARAMETER

            override fun getTextFor(name: Name) = createArgumentWithName(name).text
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val argument = diagnostic.psiElement.getParentOfType<KtValueArgument>(false) ?: return null
            return AddNameToArgumentFix(argument)
        }

    }
}
