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
import com.intellij.openapi.util
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractKotlinInplaceIntroducer
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.JetType
import java.awt.BorderLayout

public class KotlinVariableInplaceIntroducer(
        val addedVariable: JetProperty,
        val originalExpression: JetExpression,
        val occurrencesToReplace: Array<JetExpression>,
        val suggestedNames: Array<out String>,
        val isVar: Boolean,
        val doNotChangeVar: Boolean,
        val expressionType: JetType?,
        val noTypeInference: Boolean,
        project: Project,
        editor: Editor
): AbstractKotlinInplaceIntroducer<JetProperty>(
        originalExpression,
        occurrencesToReplace,
        KotlinIntroduceVariableHandler.INTRODUCE_VARIABLE,
        project,
        editor
) {
    companion object {
    }

    init {
        initFormComponents {
            if (!doNotChangeVar) {
                val varCheckBox = NonFocusableCheckBox("Declare with var")
                varCheckBox.setSelected(isVar)
                varCheckBox.setMnemonic('v')
                varCheckBox.addActionListener {
                    myProject.executeWriteCommand(getCommandName(), getCommandName()) {
                        PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument())

                        val psiFactory = JetPsiFactory(myProject)
                        val keyword = if (varCheckBox.isSelected()) psiFactory.createVarKeyword() else psiFactory.createValKeyword()
                        addedVariable.getValOrVarKeyword().replace(keyword)
                    }
                }
                addComponent(varCheckBox)
            }

            if (expressionType != null && !noTypeInference) {
                val expressionTypeCheckBox = NonFocusableCheckBox("Specify type explicitly")
                expressionTypeCheckBox.setSelected(false)
                expressionTypeCheckBox.setMnemonic('t')
                expressionTypeCheckBox.addActionListener {
                    runWriteCommandAndRestart {
                        if (expressionTypeCheckBox.isSelected()) {
                            val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(expressionType)
                            addedVariable.setTypeReference(JetPsiFactory(myProject).createType(renderedType))
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

    override fun suggestNames(replaceAll: Boolean, variable: JetProperty?) = suggestedNames

    override fun createFieldToStartTemplateOn(replaceAll: Boolean, names: Array<out String>) = addedVariable

    override fun addAdditionalVariables(builder: TemplateBuilderImpl) {
        addedVariable.getTypeReference()?.let {
            builder.replaceElement(it,
                                   "TypeReferenceVariable",
                                   SpecifyTypeExplicitlyIntention.createTypeExpressionForTemplate(expressionType!!),
                                   false)
        }
    }

    override fun buildTemplateAndStart(refs: Collection<PsiReference>,
                                       stringUsages: Collection<util.Pair<PsiElement, TextRange>>,
                                       scope: PsiElement,
                                       containingFile: PsiFile): Boolean {
        myEditor.getCaretModel().moveToOffset(getNameIdentifier()!!.startOffset)

        val result = super.buildTemplateAndStart(refs, stringUsages, scope, containingFile)

        val templateState = TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(myEditor))
        if (templateState != null && addedVariable.getTypeReference() != null) {
            templateState.addTemplateStateListener(SpecifyTypeExplicitlyIntention.createTypeReferencePostprocessor(addedVariable))
        }

        return result
    }

    override fun updateTitle(variable: JetProperty?, value: String?) {
        // No preview to update
    }

    override fun deleteTemplateField(psiField: JetProperty?) {
        // Do not delete introduced variable as it was created outside of in-place refactoring
    }

    override fun isReplaceAllOccurrences() = true

    override fun setReplaceAllOccurrences(allOccurrences: Boolean) {

    }

    override fun getComponent() = myWholePanel

    override fun performIntroduce() {
        val replacement = JetPsiFactory(myProject).createExpression(addedVariable.getName() ?: return)
        getOccurrences().forEach {
            if (it.isValid()) {
                it.replace(replacement)
            }
        }
    }
}