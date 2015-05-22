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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.ui.NameSuggestionsField
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.ui.TitledSeparator
import com.intellij.usageView.BaseUsageViewDescriptor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.core.refactoring.isMultiLine
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.KotlinExtractFunctionDialog
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.KotlinParameterTablePanel
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.types.JetType
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.Collections
import java.util.LinkedHashMap
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

public class KotlinIntroduceParameterDialog private (
        project: Project,
        val editor: Editor,
        val descriptor: IntroduceParameterDescriptor,
        val lambdaExtractionDescriptor: ExtractableCodeDescriptor?,
        nameSuggestions: Array<String>,
        val typeSuggestions: List<JetType>,
        val helper: KotlinIntroduceParameterHelper
): RefactoringDialog(project, true) {
    constructor(
            project: Project,
            editor: Editor,
            descriptor: IntroduceParameterDescriptor,
            nameSuggestions: Array<String>,
            typeSuggestions: List<JetType>,
            helper: KotlinIntroduceParameterHelper
    ): this(project, editor, descriptor, null, nameSuggestions, typeSuggestions, helper)

    constructor(project: Project,
                editor: Editor,
                introduceParameterDescriptor: IntroduceParameterDescriptor,
                lambdaExtractionDescriptor: ExtractableCodeDescriptor,
                helper: KotlinIntroduceParameterHelper
    ) : this(
            project,
            editor,
            introduceParameterDescriptor,
            lambdaExtractionDescriptor,
            lambdaExtractionDescriptor.suggestedNames.copyToArray(),
            listOf(lambdaExtractionDescriptor.controlFlow.outputValueBoxer.returnType),
            helper
    )

    private val typeNameSuggestions = typeSuggestions
            .map { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) }
            .copyToArray()

    private val nameField = NameSuggestionsField(nameSuggestions, project, JetFileType.INSTANCE)
    private val typeField = NameSuggestionsField(typeNameSuggestions, project, JetFileType.INSTANCE)
    private var replaceAllCheckBox: JCheckBox? = null
    private var defaultValueCheckBox: JCheckBox? = null
    private val removeParamsCheckBoxes = LinkedHashMap<JCheckBox, JetParameter>(descriptor.parametersToRemove.size())
    private var parameterTablePanel: KotlinParameterTablePanel? = null

    init {
        setTitle(if (lambdaExtractionDescriptor != null) INTRODUCE_LAMBDA_PARAMETER else INTRODUCE_PARAMETER)
        init()

        nameField.addDataChangedListener { validateButtons() }
        typeField.addDataChangedListener { validateButtons() }
    }

    override fun getPreferredFocusedComponent() = nameField.getFocusableComponent()

    private fun updateRemoveParamCheckBoxes() {
        val enableParamRemove = (replaceAllCheckBox?.isSelected() ?: true) && (!defaultValueCheckBox!!.isSelected())
        removeParamsCheckBoxes.keySet().forEach {
            it.setEnabled(enableParamRemove)
            it.setSelected(enableParamRemove)
        }
    }

    override fun createNorthPanel(): JComponent? {
        val gbConstraints = GridBagConstraints()

        val panel = JPanel(GridBagLayout())

        gbConstraints.anchor = GridBagConstraints.WEST
        gbConstraints.fill = GridBagConstraints.NONE
        gbConstraints.gridx = 0

        gbConstraints.insets = Insets(4, 4, 4, 0)
        gbConstraints.gridwidth = 1
        gbConstraints.weightx = 0.0
        gbConstraints.weighty = 0.0
        gbConstraints.gridy = 0
        val nameLabel = JLabel("Parameter name: ")
        nameLabel.setDisplayedMnemonic('n')
        nameLabel.setLabelFor(nameField)
        panel.add(nameLabel, gbConstraints)

        gbConstraints.insets = Insets(4, 4, 4, 8)
        gbConstraints.gridx++
        gbConstraints.weightx = 1.0
        gbConstraints.fill = GridBagConstraints.BOTH
        panel.add(nameField, gbConstraints)

        gbConstraints.insets = Insets(4, 4, 4, 8)
        gbConstraints.gridwidth = 1
        gbConstraints.weightx = 0.0
        gbConstraints.gridx = 0
        gbConstraints.gridy++
        gbConstraints.fill = GridBagConstraints.NONE
        val typeLabel = JLabel(if (lambdaExtractionDescriptor != null) "Lambda return type" else "Parameter type: ")
        typeLabel.setDisplayedMnemonic('t')
        typeLabel.setLabelFor(typeField)
        panel.add(typeLabel, gbConstraints)

        gbConstraints.gridx++
        gbConstraints.insets = Insets(4, 4, 4, 8)
        gbConstraints.weightx = 1.0
        gbConstraints.fill = GridBagConstraints.BOTH
        panel.add(typeField, gbConstraints)

        if (lambdaExtractionDescriptor != null
            && (lambdaExtractionDescriptor.parameters.isNotEmpty() || lambdaExtractionDescriptor.receiverParameter != null)) {
            val parameterTablePanel = object : KotlinParameterTablePanel() {
                override fun onEnterAction() {
                    doOKAction()
                }

                override fun onCancelAction() {
                    doCancelAction()
                }
            }
            parameterTablePanel.init(lambdaExtractionDescriptor!!.receiverParameter, lambdaExtractionDescriptor.parameters)

            gbConstraints.insets = Insets(4, 4, 4, 8)
            gbConstraints.gridwidth = 1
            gbConstraints.weightx = 0.0
            gbConstraints.gridx = 0
            gbConstraints.gridy++
            gbConstraints.fill = GridBagConstraints.NONE
            val parametersLabel = JLabel("Lambda parameters: ")
            parametersLabel.setDisplayedMnemonic('p')
            parametersLabel.setLabelFor(parameterTablePanel)
            panel.add(parametersLabel, gbConstraints)

            gbConstraints.gridx++
            gbConstraints.insets = Insets(4, 4, 4, 8)
            gbConstraints.weightx = 1.0
            gbConstraints.fill = GridBagConstraints.BOTH
            panel.add(parameterTablePanel, gbConstraints)

            this.parameterTablePanel = parameterTablePanel
        }

        gbConstraints.fill = GridBagConstraints.HORIZONTAL
        gbConstraints.gridx = 0
        gbConstraints.insets = Insets(4, 0, 4, 8)
        gbConstraints.gridwidth = 2
        gbConstraints.gridy++

        val defaultValueCheckBox = NonFocusableCheckBox("Introduce default value")
        defaultValueCheckBox.setSelected(descriptor.withDefaultValue)
        defaultValueCheckBox.setMnemonic('d')
        defaultValueCheckBox.addActionListener { updateRemoveParamCheckBoxes() }
        panel.add(defaultValueCheckBox, gbConstraints)

        this.defaultValueCheckBox = defaultValueCheckBox

        val occurrenceCount = descriptor.occurrencesToReplace.size()

        if (occurrenceCount > 1) {
            gbConstraints.gridy++
            val replaceAllCheckBox = NonFocusableCheckBox("Replace all occurrences ($occurrenceCount)")
            replaceAllCheckBox.setSelected(true)
            replaceAllCheckBox.setMnemonic('R')
            replaceAllCheckBox.addActionListener { updateRemoveParamCheckBoxes() }
            panel.add(replaceAllCheckBox, gbConstraints)
            this.replaceAllCheckBox = replaceAllCheckBox
        }

        if (replaceAllCheckBox != null) {
            gbConstraints.insets = Insets(0, 16, 4, 8)
        }

        for (parameter in descriptor.parametersToRemove) {
            val cb = NonFocusableCheckBox("Remove parameter '${parameter.getName()}' no longer used")

            removeParamsCheckBoxes[cb] = parameter
            cb.setSelected(true)
            gbConstraints.gridy++
            panel.add(cb, gbConstraints)
        }

        return panel
    }

    override fun createCenterPanel() = null

    override fun canRun() {
        fun validateElement(e: PsiElement, errorMessage: String) {
            try {
                AnalyzingUtils.checkForSyntacticErrors(e)
            }
            catch(e: Exception) {
                throw ConfigurationException(errorMessage)
            }
        }

        val psiFactory = JetPsiFactory(myProject)
        validateElement(psiFactory.createType(nameField.getEnteredName()), "Invalid parameter name")
        validateElement(psiFactory.createType(typeField.getEnteredName()), "Invalid parameter type")
    }

    override fun doAction() {
        performRefactoring()
    }

    public fun performRefactoring() {
        invokeRefactoring(
                object : BaseRefactoringProcessor(myProject) {
                    override fun findUsages() = UsageInfo.EMPTY_ARRAY

                    override fun performRefactoring(usages: Array<out UsageInfo>) {
                        fun createLambdaForArgument(function: JetFunction): JetExpression {
                            val statement = (function.getBodyExpression() as JetBlockExpression).getStatements().single()
                            val space = if (statement.isMultiLine()) "\n" else " "
                            val parameters = function.getValueParameters()
                            val parametersText = if (parameters.isNotEmpty()) {
                                " " + parameters.map { it.getName() }.joinToString() + " ->"
                            } else ""
                            val text = "{$parametersText$space${statement.getText()}$space}"

                            return JetPsiFactory(myProject).createExpression(text)
                        }

                        val chosenName = nameField.getEnteredName()
                        var chosenType = typeField.getEnteredName()
                        var newArgumentValue = descriptor.newArgumentValue
                        var newReplacer = descriptor.occurrenceReplacer

                        lambdaExtractionDescriptor?.let { oldDescriptor ->
                            val newDescriptor = KotlinExtractFunctionDialog.createNewDescriptor(
                                    oldDescriptor,
                                    chosenName,
                                    "",
                                    parameterTablePanel?.getReceiverInfo(),
                                    parameterTablePanel?.getParameterInfos() ?: listOf()
                            )
                            val options = ExtractionGeneratorOptions.DEFAULT.copy(
                                    target = ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION,
                                    allowExpressionBody = false
                            )
                            with (ExtractionGeneratorConfiguration(newDescriptor, options).generateDeclaration()) {
                                val function = declaration as JetFunction
                                val receiverType = function.getReceiverTypeReference()?.getText()
                                val parameterTypes = function
                                        .getValueParameters()
                                        .map { it.getTypeReference()!!.getText() }
                                        .joinToString()
                                val returnType = function.getTypeReference()?.getText() ?: "Unit"

                                chosenType = (receiverType?.let { "$it." } ?: "") + "($parameterTypes) -> $returnType"
                                newArgumentValue = createLambdaForArgument(function)
                                newReplacer = { }

                                processDuplicates(duplicateReplacers, myProject, editor)
                            }
                        }

                        val descriptorToRefactor = descriptor.copy(
                                newParameterName = chosenName,
                                newParameterTypeText = chosenType,
                                newArgumentValue = newArgumentValue,
                                withDefaultValue = defaultValueCheckBox!!.isSelected(),
                                occurrencesToReplace = with(descriptor) {
                                    if (replaceAllCheckBox?.isSelected() ?: true) {
                                        occurrencesToReplace
                                    }
                                    else {
                                        Collections.singletonList(originalOccurrence)
                                    }
                                },
                                parametersToRemove = removeParamsCheckBoxes.filter { it.key.isEnabled() && it.key.isSelected() }.map { it.value },
                                occurrenceReplacer = newReplacer
                        )

                        helper.configure(descriptorToRefactor).performRefactoring()
                    }

                    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>) = BaseUsageViewDescriptor()

                    override fun getCommandName() = if (lambdaExtractionDescriptor != null) INTRODUCE_LAMBDA_PARAMETER else INTRODUCE_PARAMETER
                }
        )
    }
}
