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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.core.getOrCreateValueParameterList
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.resolveToHeaderDescriptorIfPossible
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference

class ConvertReceiverToParameterIntention : SelfTargetingOffsetIndependentIntention<KtTypeReference>(KtTypeReference::class.java, "Convert receiver to parameter"), LowPriorityAction {
    override fun isApplicableTo(element: KtTypeReference): Boolean {
        return (element.parent as? KtNamedFunction)?.receiverTypeReference == element
    }

    private fun configureChangeSignature(newName: String? = null): KotlinChangeSignatureConfiguration {
        return object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                return originalDescriptor.modify {
                    it.receiver = null
                    if (newName != null) {
                        it.parameters.first().name = newName
                    }
                }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true
        }
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtTypeReference, editor: Editor?) {
        val function = element.parent as? KtNamedFunction ?: return
        val descriptor = function.resolveToHeaderDescriptorIfPossible() as FunctionDescriptor

        val project = function.project

        if (editor != null && !ApplicationManager.getApplication().isUnitTestMode) {
            val receiverNames = suggestReceiverNames(project, descriptor)
            val defaultReceiverName = receiverNames.first()
            val receiverTypeRef = function.receiverTypeReference!!
            val psiFactory = KtPsiFactory(element)
            val newParameter = psiFactory.createParameter("$defaultReceiverName: Dummy").apply { typeReference!!.replace(receiverTypeRef) }

            project.executeWriteCommand(text) {
                function.setReceiverTypeReference(null)
                val addedParameter = function.getOrCreateValueParameterList().addParameterAfter(newParameter, null)

                with(PsiDocumentManager.getInstance(project)) {
                    commitDocument(editor.document)
                    doPostponedOperationsAndUnblockDocument(editor.document)
                }

                editor.caretModel.moveToOffset(function.startOffset)

                val templateBuilder = TemplateBuilderImpl(function)
                templateBuilder.replaceElement(addedParameter.nameIdentifier!!, ChooseStringExpression(receiverNames))
                TemplateManager.getInstance(project).startTemplate(
                        editor,
                        templateBuilder.buildInlineTemplate(),
                        object: TemplateEditingAdapter() {
                            private fun revertChanges() {
                                runWriteAction {
                                    function.setReceiverTypeReference(addedParameter.typeReference)
                                    function.valueParameterList!!.removeParameter(addedParameter)
                                }
                            }

                            override fun templateFinished(template: Template?, brokenOff: Boolean) {
                                val newName = addedParameter.name
                                revertChanges()
                                if (!brokenOff) {
                                    runChangeSignature(
                                            element.project,
                                            function.resolveToHeaderDescriptorIfPossible() as FunctionDescriptor,
                                            configureChangeSignature(newName),
                                            function.receiverTypeReference!!,
                                            text
                                    )
                                }
                            }

                            override fun templateCancelled(template: Template?) {
                                revertChanges()
                            }
                        }
                )
            }
        }
        else {
            runChangeSignature(element.project, descriptor, configureChangeSignature(), element, text)
        }
    }
}
