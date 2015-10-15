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

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetPsiFactory

/**
 * Changes method signature to one of provided signatures.
 * Based on [KotlinAddImportAction]
 */
class JetAddFunctionToClassifierAction(
        private val project: Project,
        private val editor: Editor?,
        private val functionsToAdd: List<FunctionDescriptor>
) {
    private fun addFunction(
            project: Project,
            typeDescriptor: ClassDescriptor,
            functionDescriptor: FunctionDescriptor
    ) {
        val signatureString = IdeDescriptorRenderers.SOURCE_CODE.render(functionDescriptor)

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val classifierDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, typeDescriptor) as JetClass

        project.executeWriteCommand(JetBundle.message("add.function.to.type.action")) {
            val body = classifierDeclaration.getOrCreateBody()

            var functionBody = ""
            if (typeDescriptor.kind != ClassKind.INTERFACE && functionDescriptor.modality != Modality.ABSTRACT) {
                functionBody = "{}"
                val returnType = functionDescriptor.returnType
                if (returnType == null || !KotlinBuiltIns.isUnit(returnType)) {
                    functionBody = "{ throw UnsupportedOperationException() }"
                }
            }
            val functionElement = JetPsiFactory(project).createFunction(signatureString + functionBody)
            val insertedFunctionElement = body.addBefore(functionElement, body.rBrace) as JetNamedFunction

            ShortenReferences.DEFAULT.process(insertedFunctionElement)
        }
    }

    fun execute() {
        if (functionsToAdd.isEmpty()) return

        if (functionsToAdd.size == 1 || editor == null || !editor.component.isShowing) {
            addFunction(functionsToAdd.get(0))
        }
        else {
            JBPopupFactory.getInstance().createListPopup(functionPopup).showInBestPositionFor(editor)
        }
    }

    private val functionPopup: ListPopupStep<*>
        get() {
            return object : BaseListPopupStep<FunctionDescriptor>(JetBundle.message("add.function.to.type.action.type.chooser"), functionsToAdd) {
                override fun isAutoSelectionEnabled() = false

                override fun onChosen(selectedValue: FunctionDescriptor, finalChoice: Boolean): PopupStep<Any>? {
                    if (finalChoice) {
                        addFunction(selectedValue)
                    }
                    return PopupStep.FINAL_CHOICE
                }

                override fun getIconFor(aValue: FunctionDescriptor) = PlatformIcons.FUNCTION_ICON

                override fun getTextFor(functionDescriptor: FunctionDescriptor): String {
                    val type = functionDescriptor.containingDeclaration as ClassDescriptor
                    return JetBundle.message("add.function.to.type.action.single",
                            IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(functionDescriptor),
                            type.name.toString())
                }
            }
        }

    private fun addFunction(functionToAdd: FunctionDescriptor) {
        addFunction(project, functionToAdd.containingDeclaration as ClassDescriptor, functionToAdd)
    }
}
