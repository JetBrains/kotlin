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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.ui.NonFocusableCheckBox
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractKotlinInplaceIntroducer
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType

class KotlinVariableInplaceIntroducer(
        val addedVariable: KtProperty,
        val originalExpression: KtExpression?,
        val occurrencesToReplace: Array<KtExpression>,
        suggestedNames: Collection<String>,
        val isVar: Boolean,
        val doNotChangeVar: Boolean,
        val expressionType: KotlinType?,
        val noTypeInference: Boolean,
        project: Project,
        editor: Editor,
        private val postProcess: (KtDeclaration) -> Unit
): AbstractKotlinInplaceIntroducer<KtProperty>(
        addedVariable,
        originalExpression,
        occurrencesToReplace,
        KotlinIntroduceVariableHandler.INTRODUCE_VARIABLE,
        project,
        editor
) {
    private val suggestedNames = suggestedNames.toTypedArray()

    init {
        initFormComponents {
            if (!doNotChangeVar) {
                val varCheckBox = NonFocusableCheckBox("Declare with var")
                varCheckBox.isSelected = isVar
                varCheckBox.setMnemonic('v')
                varCheckBox.addActionListener {
                    myProject.executeWriteCommand(commandName, commandName) {
                        PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.document)

                        val psiFactory = KtPsiFactory(myProject)
                        val keyword = if (varCheckBox.isSelected) psiFactory.createVarKeyword() else psiFactory.createValKeyword()
                        addedVariable.valOrVarKeyword.replace(keyword)
                    }
                }
                addComponent(varCheckBox)
            }

            if (expressionType != null && !noTypeInference) {
                val expressionTypeCheckBox = NonFocusableCheckBox("Specify type explicitly")
                expressionTypeCheckBox.isSelected = false
                expressionTypeCheckBox.setMnemonic('t')
                expressionTypeCheckBox.addActionListener {
                    runWriteCommandAndRestart {
                        if (expressionTypeCheckBox.isSelected) {
                            val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(expressionType)
                            addedVariable.setTypeReference(KtPsiFactory(myProject).createType(renderedType))
                        }
                        else {
                            addedVariable.setTypeReference(null)
                        }
                    }
                }
                addComponent(expressionTypeCheckBox)
            }
        }
    }

    override fun getVariable() = addedVariable

    override fun suggestNames(replaceAll: Boolean, variable: KtProperty?) = suggestedNames

    override fun createFieldToStartTemplateOn(replaceAll: Boolean, names: Array<out String>) = addedVariable

    override fun addAdditionalVariables(builder: TemplateBuilderImpl) {
        addedVariable.typeReference?.let {
            builder.replaceElement(it,
                                   "TypeReferenceVariable",
                                   SpecifyTypeExplicitlyIntention.createTypeExpressionForTemplate(expressionType!!, addedVariable),
                                   false)
        }
    }

    override fun buildTemplateAndStart(refs: Collection<PsiReference>,
                                       stringUsages: Collection<Pair<PsiElement, TextRange>>,
                                       scope: PsiElement,
                                       containingFile: PsiFile): Boolean {
        myEditor.caretModel.moveToOffset(nameIdentifier!!.startOffset)

        val result = super.buildTemplateAndStart(refs, stringUsages, scope, containingFile)

        val templateState = TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(myEditor))
        if (templateState != null && addedVariable.typeReference != null) {
            templateState.addTemplateStateListener(SpecifyTypeExplicitlyIntention.createTypeReferencePostprocessor(addedVariable))
        }

        return result
    }

    override fun updateTitle(variable: KtProperty?, value: String?) {
        // No preview to update
    }

    override fun deleteTemplateField(psiField: KtProperty?) {
        // Do not delete introduced variable as it was created outside of in-place refactoring
    }

    override fun isReplaceAllOccurrences() = true

    override fun setReplaceAllOccurrences(allOccurrences: Boolean) {

    }

    override fun getComponent() = myWholePanel

    override fun performIntroduce() {
        val newName = inputName ?: return
        addedVariable.setName(newName)
        val replacement = KtPsiFactory(myProject).createExpression(newName)
        occurrences.forEach {
            if (it.isValid) {
                it.replace(replacement)
            }
        }
    }

    override fun moveOffsetAfter(success: Boolean) {
        super.moveOffsetAfter(success)
        if (success) {
            postProcess(addedVariable)
        }
    }
}