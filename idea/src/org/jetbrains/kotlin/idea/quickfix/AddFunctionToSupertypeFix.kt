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
import com.intellij.codeInsight.intention.LowPriorityAction
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.TemplateKind
import org.jetbrains.kotlin.idea.core.getFunctionBodyTextFromTemplate
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.supertypes

class AddFunctionToSupertypeFix private constructor(
    element: KtNamedFunction,
    private val functions: List<FunctionData>
) : KotlinQuickFixAction<KtNamedFunction>(element), LowPriorityAction {

    init {
        assert(functions.isNotEmpty())
    }

    private class FunctionData(
        val signaturePreview: String,
        val sourceCode: String,
        val targetClass: KtClass
    )

    override fun getText(): String {
        val single = functions.singleOrNull()
        return if (single != null)
            actionName(single)
        else
            KotlinBundle.message("fix.add.function.supertype.text")
    }

    override fun getFamilyName() = KotlinBundle.message("fix.add.function.supertype.family")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (functions.size == 1 || editor == null || !editor.component.isShowing) {
                addFunction(functions.first(), project)
            } else {
                JBPopupFactory.getInstance().createListPopup(createFunctionPopup(project)).showInBestPositionFor(editor)
            }
        }
    }

    private fun addFunction(functionData: FunctionData, project: Project) {
        project.executeWriteCommand(KotlinBundle.message("fix.add.function.supertype.progress")) {
            val classBody = functionData.targetClass.getOrCreateBody()

            val functionElement = KtPsiFactory(project).createFunction(functionData.sourceCode)
            val insertedFunctionElement = classBody.addBefore(functionElement, classBody.rBrace) as KtNamedFunction

            ShortenReferences.DEFAULT.process(insertedFunctionElement)
            val modifierToken = insertedFunctionElement.modalityModifier()?.node?.elementType as? KtModifierKeywordToken
                ?: return@executeWriteCommand
            if (insertedFunctionElement.implicitModality() == modifierToken) {
                RemoveModifierFix(insertedFunctionElement, modifierToken, true).invoke()
            }
        }
    }

    private fun createFunctionPopup(project: Project): ListPopupStep<*> {
        return object : BaseListPopupStep<FunctionData>(KotlinBundle.message("fix.add.function.supertype.choose.type"), functions) {
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

    private fun actionName(functionData: FunctionData): String {
        return KotlinBundle.message(
            "fix.add.function.supertype.add.to",
            functionData.signaturePreview, functionData.targetClass.name.toString()
        )
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = diagnostic.psiElement as? KtNamedFunction ?: return null

            val descriptors = generateFunctionsToAdd(function)
            if (descriptors.isEmpty()) return null

            val project = diagnostic.psiFile!!.project
            val functionData = descriptors.mapNotNull { createFunctionData(it, project) }
            if (functionData.isEmpty()) return null

            return AddFunctionToSupertypeFix(function, functionData)
        }

        private fun createFunctionData(functionDescriptor: FunctionDescriptor, project: Project): FunctionData? {
            val classDescriptor = functionDescriptor.containingDeclaration as ClassDescriptor

            var sourceCode = IdeDescriptorRenderers.SOURCE_CODE.render(functionDescriptor)
            if (classDescriptor.kind != ClassKind.INTERFACE && functionDescriptor.modality != Modality.ABSTRACT) {
                val returnType = functionDescriptor.returnType
                sourceCode += if (returnType == null || !KotlinBuiltIns.isUnit(returnType)) {
                    val bodyText = getFunctionBodyTextFromTemplate(
                        project,
                        TemplateKind.FUNCTION,
                        functionDescriptor.name.asString(),
                        functionDescriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: "Unit",
                        classDescriptor.importableFqName
                    )
                    "{\n$bodyText\n}"
                } else {
                    "{}"
                }
            }

            val targetClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor) as? KtClass ?: return null
            return FunctionData(
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.render(functionDescriptor),
                sourceCode,
                targetClass
            )
        }

        private fun generateFunctionsToAdd(functionElement: KtNamedFunction): List<FunctionDescriptor> {
            val functionDescriptor = functionElement.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return emptyList()

            val containingClass = functionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            // TODO: filter out impossible supertypes (for example when argument's type isn't visible in a superclass).
            return getSuperClasses(containingClass)
                .asSequence()
                .filterNot { KotlinBuiltIns.isAnyOrNullableAny(it.defaultType) }
                .map { generateFunctionSignatureForType(functionDescriptor, it) }
                .toList()
        }

        private fun MutableList<KotlinType>.sortSubtypesFirst(): List<KotlinType> {
            val typeChecker = KotlinTypeChecker.DEFAULT
            for (i in 1 until size) {
                val currentType = this[i]
                for (j in 0 until i) {
                    if (typeChecker.isSubtypeOf(currentType, this[j])) {
                        this.removeAt(i)
                        this.add(j, currentType)
                        break
                    }
                }
            }
            return this
        }

        private fun getSuperClasses(classDescriptor: ClassDescriptor): List<ClassDescriptor> {
            val supertypes = classDescriptor.defaultType.supertypes().toMutableList().sortSubtypesFirst()
            return supertypes.mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
        }

        private fun generateFunctionSignatureForType(
            functionDescriptor: FunctionDescriptor,
            typeDescriptor: ClassDescriptor
        ): FunctionDescriptor {
            // TODO: support for generics.

            val modality = if (typeDescriptor.kind == ClassKind.INTERFACE || typeDescriptor.modality == Modality.SEALED) {
                Modality.ABSTRACT
            } else {
                typeDescriptor.modality
            }

            return functionDescriptor.copy(
                typeDescriptor,
                modality,
                functionDescriptor.visibility,
                CallableMemberDescriptor.Kind.DECLARATION,
                /* copyOverrides = */ false
            )
        }
    }
}
