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
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.util.*

class AddFunctionToSupertypeFix private constructor(
        element: JetNamedFunction,
        private val functions: List<AddFunctionToSupertypeFix.FunctionData>
) : KotlinQuickFixAction<JetNamedFunction>(element) {

    init {
        assert(functions.isNotEmpty())
    }

    private class FunctionData(
            val signaturePreview: String,
            val sourceCode: String,
            val targetClass: JetClass
    )

    override fun getText(): String {
        val single = functions.singleOrNull()
        return if (single != null)
            actionName(single)
        else
            "Add function to supertype..."
    }

    override fun getFamilyName() = "Add function to supertype"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        CommandProcessor.getInstance().runUndoTransparentAction(object : Runnable {
            override fun run() {
                if (functions.size == 1 || editor == null || !editor.component.isShowing) {
                    addFunction(functions.first(), project)
                }
                else {
                    JBPopupFactory.getInstance().createListPopup(createFunctionPopup(project)).showInBestPositionFor(editor)
                }
            }
        })
    }

    private fun addFunction(functionData: FunctionData, project: Project) {
        project.executeWriteCommand("Add Function to Type") {
            val classBody = functionData.targetClass.getOrCreateBody()

            val functionElement = JetPsiFactory(project).createFunction(functionData.sourceCode)
            val insertedFunctionElement = classBody.addBefore(functionElement, classBody.rBrace) as JetNamedFunction

            ShortenReferences.DEFAULT.process(insertedFunctionElement)
        }
    }

    private fun createFunctionPopup(project: Project): ListPopupStep<*> {
        return object : BaseListPopupStep<FunctionData>("Choose Type", functions) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: FunctionData, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    addFunction(selectedValue, project)
                }
                return PopupStep.FINAL_CHOICE
            }

            override fun getIconFor(value: FunctionData) = PlatformIcons.FUNCTION_ICON
            override fun getTextFor(value: FunctionData) = actionName(value)
        }
    }

    private fun actionName(functionData: FunctionData)
            = "Add '${functionData.signaturePreview}' to '${functionData.targetClass.name}'"

    companion object: JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = diagnostic.psiElement as? JetNamedFunction ?: return null

            val descriptors = generateFunctionsToAdd(function)
            if (descriptors.isEmpty()) return null

            val project = diagnostic.psiFile.project
            val functionData = descriptors.map { createFunctionData(it, project) }.filterNotNull()
            if (functionData.isEmpty()) return null

            return AddFunctionToSupertypeFix(function, functionData)
        }

        private fun createFunctionData(functionDescriptor: FunctionDescriptor, project: Project): FunctionData? {
            val classDescriptor = functionDescriptor.containingDeclaration as ClassDescriptor

            var sourceCode = IdeDescriptorRenderers.SOURCE_CODE.render(functionDescriptor)
            if (classDescriptor.kind != ClassKind.INTERFACE && functionDescriptor.modality != Modality.ABSTRACT) {
                val returnType = functionDescriptor.returnType
                if (returnType == null || !KotlinBuiltIns.isUnit(returnType)) {
                    sourceCode += "{ throw UnsupportedOperationException() }"
                }
                else {
                    sourceCode += "{}"
                }
            }

            val targetClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor) as? JetClass ?: return null
            return FunctionData(
                    IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(functionDescriptor),
                    sourceCode,
                    targetClass)
        }

        private fun generateFunctionsToAdd(functionElement: JetNamedFunction): List<FunctionDescriptor> {
            val functionDescriptor = functionElement.resolveToDescriptor() as FunctionDescriptor

            val containingClass = functionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            // TODO: filter out impossible supertypes (for example when argument's type isn't visible in a superclass).
            return getSuperClasses(containingClass)
                    .filterNot { KotlinBuiltIns.isAnyOrNullableAny(it.defaultType) }
                    .map { generateFunctionSignatureForType(functionDescriptor, it) }
        }

        private fun getSuperClasses(classDescriptor: ClassDescriptor): List<ClassDescriptor> {
            val supertypes = classDescriptor.defaultType.supertypes().sortedWith(object : Comparator<JetType> {
                override fun compare(o1: JetType, o2: JetType): Int {
                    return when {
                        o1 == o2 -> 0
                        JetTypeChecker.DEFAULT.isSubtypeOf(o1, o2) -> -1
                        JetTypeChecker.DEFAULT.isSubtypeOf(o2, o1) -> 1
                        else -> o1.toString().compareTo(o2.toString())
                    }
                }
            })

            return supertypes
                    .map { it.constructor.declarationDescriptor as? ClassDescriptor }
                    .filterNotNull()
        }

        private fun generateFunctionSignatureForType(functionDescriptor: FunctionDescriptor, typeDescriptor: ClassDescriptor): FunctionDescriptor {
            // TODO: support for generics.

            val modality = if (typeDescriptor.kind == ClassKind.INTERFACE) Modality.OPEN else typeDescriptor.modality

            return functionDescriptor.copy(
                    typeDescriptor,
                    modality,
                    functionDescriptor.visibility,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    /* copyOverrides = */ false)
        }
    }
}
