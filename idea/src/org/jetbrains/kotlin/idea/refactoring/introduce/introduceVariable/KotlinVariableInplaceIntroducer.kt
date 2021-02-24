/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import com.intellij.ui.NonFocusableCheckBox
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractKotlinInplaceIntroducer
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType
import java.util.*
import javax.swing.JCheckBox

class KotlinVariableInplaceIntroducer(
    addedVariable: KtProperty,
    originalExpression: KtExpression?,
    occurrencesToReplace: Array<KtExpression>,
    suggestedNames: Collection<String>,
    val isVar: Boolean,
    val doNotChangeVar: Boolean,
    val expressionType: KotlinType?,
    val noTypeInference: Boolean,
    project: Project,
    editor: Editor,
    private val postProcess: (KtDeclaration) -> Unit
) : AbstractKotlinInplaceIntroducer<KtProperty>(
    addedVariable,
    originalExpression,
    occurrencesToReplace,
    KotlinIntroduceVariableHandler.INTRODUCE_VARIABLE,
    project,
    editor
) {
    private val suggestedNames = suggestedNames.toTypedArray()
    private var expressionTypeCheckBox: JCheckBox? = null
    private val addedVariablePointer = addedVariable.createSmartPointer()
    private val addedVariable get() = addedVariablePointer.element

    init {
        initFormComponents {
            if (!doNotChangeVar) {
                val varCheckBox = NonFocusableCheckBox(KotlinBundle.message("checkbox.text.declare.with.var"))
                varCheckBox.isSelected = isVar
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
                val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(expressionType)
                expressionTypeCheckBox =
                    NonFocusableCheckBox(KotlinBundle.message("checkbox.text.specify.type.explicitly")).apply {
                        isSelected = false
                        addActionListener {
                            runWriteCommandAndRestart {
                                updateVariableName()
                                if (isSelected) {
                                    addedVariable.typeReference = KtPsiFactory(myProject).createType(renderedType)
                                } else {
                                    addedVariable.typeReference = null
                                }
                            }
                        }

                        addComponent(this)
                    }
            }
        }
    }

    override fun getVariable() = addedVariable

    override fun suggestNames(replaceAll: Boolean, variable: KtProperty?) = suggestedNames

    override fun createFieldToStartTemplateOn(replaceAll: Boolean, names: Array<out String>) = addedVariable

    override fun addAdditionalVariables(builder: TemplateBuilderImpl) {
        val variable = addedVariable ?: return
        variable.typeReference?.let {
            val expression = SpecifyTypeExplicitlyIntention.createTypeExpressionForTemplate(expressionType!!, variable) ?: return@let
            builder.replaceElement(it, "TypeReferenceVariable", expression, false)
        }
    }

    override fun buildTemplateAndStart(
        refs: Collection<PsiReference>,
        stringUsages: Collection<Pair<PsiElement, TextRange>>,
        scope: PsiElement,
        containingFile: PsiFile
    ): Boolean {
        myNameSuggestions = myNameSuggestions.mapTo(LinkedHashSet(), String::quoteIfNeeded)

        myEditor.caretModel.moveToOffset(nameIdentifier!!.startOffset)

        val result = super.buildTemplateAndStart(refs, stringUsages, scope, containingFile)
        val templateState = TemplateManagerImpl
            .getTemplateState(com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil.getTopLevelEditor(myEditor))
        val variable = addedVariable
        if (templateState != null && variable?.typeReference != null) {
            templateState.addTemplateStateListener(SpecifyTypeExplicitlyIntention.createTypeReferencePostprocessor(variable))
        }

        return result
    }

    override fun getInitialName() = super.getInitialName().quoteIfNeeded()

    override fun updateTitle(variable: KtProperty?, value: String?) {
        expressionTypeCheckBox?.isEnabled = value == null || value.isIdentifier()
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
        val replacement = KtPsiFactory(myProject).createExpression(newName)

        runWriteAction {
            addedVariable?.setName(newName)
            occurrences.forEach {
                if (it.isValid) {
                    it.replace(replacement)
                }
            }
        }
    }

    override fun moveOffsetAfter(success: Boolean) {
        super.moveOffsetAfter(success)
        if (success) {
            addedVariable?.let { postProcess(it) }
        }
    }
}