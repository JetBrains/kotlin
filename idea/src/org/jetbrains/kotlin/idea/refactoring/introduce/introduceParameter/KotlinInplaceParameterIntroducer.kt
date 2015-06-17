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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetValVar
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractKotlinInplaceIntroducer
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.idea.util.supertypes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.awt.BorderLayout
import java.awt.Color
import java.util.ArrayList
import javax.swing.JCheckBox

public class KotlinInplaceParameterIntroducer(
        val originalDescriptor: IntroduceParameterDescriptor,
        val parameterType: JetType,
        val suggestedNames: Array<out String>,
        project: Project,
        editor: Editor
): AbstractKotlinInplaceIntroducer<JetParameter>(
        originalDescriptor.originalRange.elements.single() as JetExpression,
        originalDescriptor.occurrencesToReplace
                .map { it.elements.single() as JetExpression }
                .filterNotNull()
                .toTypedArray(),
        INTRODUCE_PARAMETER,
        project,
        editor
) {
    companion object {
        private val LOG = Logger.getInstance(javaClass<KotlinInplaceParameterIntroducer>())
    }

    enum class PreviewDecorator {
        FOR_ADD() {
            override val textAttributes: TextAttributes = with(TextAttributes()) {
                setEffectType(EffectType.ROUNDED_BOX)
                setEffectColor(JBColor.RED)
                this
            }
        },

        FOR_REMOVAL() {
            override val textAttributes: TextAttributes = with(TextAttributes()) {
                setEffectType(EffectType.STRIKEOUT)
                setEffectColor(Color.BLACK)
                this
            }
        };

        protected abstract val textAttributes: TextAttributes

        fun applyToRange(range: TextRange, markupModel: MarkupModel) {
            markupModel.addRangeHighlighter(range.getStartOffset(),
                                            range.getEndOffset(),
                                            0,
                                            textAttributes,
                                            HighlighterTargetArea.EXACT_RANGE
            )
        }
    }

    private var descriptor = originalDescriptor
    private var replaceAllCheckBox: JCheckBox? = null

    init {
        initFormComponents {
            addComponent(getPreviewComponent())

            val defaultValueCheckBox = NonFocusableCheckBox("Introduce default value")
            defaultValueCheckBox.setSelected(descriptor.withDefaultValue)
            defaultValueCheckBox.setMnemonic('d')
            defaultValueCheckBox.addActionListener {
                descriptor = descriptor.copy(withDefaultValue = defaultValueCheckBox.isSelected())
                updateTitle(getVariable())
            }
            addComponent(defaultValueCheckBox)

            val occurrenceCount = descriptor.occurrencesToReplace.size()
            if (occurrenceCount > 1) {
                val replaceAllCheckBox = NonFocusableCheckBox("Replace all occurrences ($occurrenceCount)")
                replaceAllCheckBox.setSelected(true)
                replaceAllCheckBox.setMnemonic('R')
                addComponent(replaceAllCheckBox)
                this@KotlinInplaceParameterIntroducer.replaceAllCheckBox = replaceAllCheckBox
            }
        }
    }

    override fun getActionName() = "IntroduceParameter"

    override fun checkLocalScope() = descriptor.callable

    override fun getVariable() = originalDescriptor.callable.getValueParameters().lastOrNull()

    override fun suggestNames(replaceAll: Boolean, variable: JetParameter?) = suggestedNames

    override fun createFieldToStartTemplateOn(replaceAll: Boolean, names: Array<out String>): JetParameter? {
        return runWriteAction {
            with(descriptor) {
                val parameterList = callable.getValueParameterList()
                                    ?: (callable as JetClass).createPrimaryConstructorParameterListIfAbsent()
                val parameter = JetPsiFactory(myProject).createParameter("${newParameterName}: ${newParameterTypeText}")
                parameterList.addParameter(parameter)
            }
        }
    }

    override fun deleteTemplateField(psiField: JetParameter) {
        if (psiField.isValid()) {
            (psiField.getParent() as? JetParameterList)?.removeParameter(psiField)
        }
    }

    override fun isReplaceAllOccurrences() = replaceAllCheckBox?.isSelected() ?: true

    override fun setReplaceAllOccurrences(allOccurrences: Boolean) {
        replaceAllCheckBox?.setSelected(allOccurrences)
    }

    override fun getComponent() = myWholePanel

    override fun updateTitle(addedParameter: JetParameter?, currentName: String?) {
        val templateState = TemplateManagerImpl.getTemplateState(myEditor)
        if (templateState == null || templateState.getTemplate() == null) return

        val currentType = templateState
                .getVariableValue(KotlinInplaceVariableIntroducer.TYPE_REFERENCE_VARIABLE_NAME)
                ?.getText()

        with (descriptor) {
            val rangesToRemove = ArrayList<TextRange>()
            var addedRange: TextRange? = null
            val builder = StringBuilder()

            (callable as? JetFunction)?.getReceiverTypeReference()?.let { receiverTypeRef ->
                builder.append(receiverTypeRef.getText()).append('.')
                if (!descriptor.withDefaultValue && receiverTypeRef in parametersToRemove) {
                    rangesToRemove.add(TextRange(0, builder.length()))
                }
            }

            builder.append(callable.getName())

            val parameters = callable.getValueParameters()
            builder.append("(")
            for (i in parameters.indices) {
                val parameter = parameters[i]

                val parameterText = if (parameter == addedParameter){
                    val parameterName = currentName ?: parameter.getName()
                    val parameterType = currentType ?: parameter.getTypeReference()!!.getText()
                    descriptor = descriptor.copy(newParameterName = parameterName, newParameterTypeText = parameterType)
                    val modifier = if (valVar != JetValVar.None) "${valVar.name} " else ""
                    val defaultValue = if (withDefaultValue) " = ${newArgumentValue.getText()}" else ""

                    "$modifier$parameterName: $parameterType$defaultValue"
                }
                else parameter.getText()

                builder.append(parameterText)

                val range = TextRange(builder.length() - parameterText.length(), builder.length())
                if (parameter == addedParameter) {
                    addedRange = range
                }
                else if (!descriptor.withDefaultValue && parameter in parametersToRemove) {
                    rangesToRemove.add(range)
                }

                if (i < parameters.lastIndex) {
                    builder.append(", ")
                }
            }
            builder.append(")")

            if (addedRange == null) {
                LOG.error("Added parameter not found: ${callable.getElementTextWithContext()}")
            }

            val document = getPreviewEditor().getDocument()
            runWriteAction { document.setText(builder.toString()) }

            val markupModel = DocumentMarkupModel.forDocument(document, myProject, true)
            markupModel.removeAllHighlighters()
            rangesToRemove.forEach { PreviewDecorator.FOR_REMOVAL.applyToRange(it, markupModel) }
            addedRange?.let { PreviewDecorator.FOR_ADD.applyToRange(it, markupModel) }
        }
        revalidate()
    }

    override fun performIntroduce() {
        getDescriptorToRefactor(isReplaceAllOccurrences()).performRefactoring()
    }

    private fun getDescriptorToRefactor(replaceAll: Boolean): IntroduceParameterDescriptor {
        val originalRange = getExpr().toRange()
        return descriptor.copy(
                originalRange = originalRange,
                occurrencesToReplace = if (replaceAll) getOccurrences().map { it.toRange() } else originalRange.singletonList(),
                newArgumentValue = getExpr()
        )
    }

    fun switchToDialogUI() {
        stopIntroduce(myEditor)
        KotlinIntroduceParameterDialog(myProject,
                                       myEditor,
                                       getDescriptorToRefactor(true),
                                       myNameSuggestions.toTypedArray(),
                                       listOf(parameterType) + parameterType.supertypes(),
                                       KotlinIntroduceParameterHelper.Default).show()
    }
}