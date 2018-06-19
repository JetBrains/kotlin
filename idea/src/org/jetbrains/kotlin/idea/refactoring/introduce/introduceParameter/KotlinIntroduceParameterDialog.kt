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

import com.intellij.openapi.command.impl.FinishMarkAction
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.refactoring.ui.NameSuggestionsField
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.ui.NonFocusableCheckBox
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.ExtractFunctionParameterTablePanel
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.KotlinExtractFunctionDialog
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.isMultiLine
import org.jetbrains.kotlin.idea.refactoring.runRefactoringWithPostprocessing
import org.jetbrains.kotlin.idea.refactoring.validateElement
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.types.KotlinType
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.*
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class KotlinIntroduceParameterDialog private constructor(
        project: Project,
        val editor: Editor,
        val descriptor: IntroduceParameterDescriptor,
        val lambdaExtractionDescriptor: ExtractableCodeDescriptor?,
        nameSuggestions: Array<String>,
        typeSuggestions: List<KotlinType>,
        val helper: KotlinIntroduceParameterHelper
): RefactoringDialog(project, true) {
    constructor(
            project: Project,
            editor: Editor,
            descriptor: IntroduceParameterDescriptor,
            nameSuggestions: Array<String>,
            typeSuggestions: List<KotlinType>,
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
            lambdaExtractionDescriptor.suggestedNames.toTypedArray(),
            listOf(lambdaExtractionDescriptor.returnType),
            helper
    )

    private val typeNameSuggestions = typeSuggestions
            .map { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(it) }
            .toTypedArray()

    private val nameField = NameSuggestionsField(nameSuggestions, project, KotlinFileType.INSTANCE)
    private val typeField = NameSuggestionsField(typeNameSuggestions, project, KotlinFileType.INSTANCE)
    private var replaceAllCheckBox: JCheckBox? = null
    private var defaultValueCheckBox: JCheckBox? = null
    private val removeParamsCheckBoxes = LinkedHashMap<JCheckBox, KtElement>(descriptor.parametersToRemove.size)
    private var parameterTablePanel: ExtractFunctionParameterTablePanel? = null
    private val commandName = if (lambdaExtractionDescriptor != null) INTRODUCE_LAMBDA_PARAMETER else INTRODUCE_PARAMETER

    init {
        title = commandName
        init()

        nameField.addDataChangedListener { validateButtons() }
        typeField.addDataChangedListener { validateButtons() }
    }

    override fun getPreferredFocusedComponent() = nameField.focusableComponent

    private fun updateRemoveParamCheckBoxes() {
        val enableParamRemove = (replaceAllCheckBox?.isSelected ?: true) && (!defaultValueCheckBox!!.isSelected)
        removeParamsCheckBoxes.keys.forEach {
            it.isEnabled = enableParamRemove
            it.isSelected = enableParamRemove
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
        nameLabel.labelFor = nameField
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
        typeLabel.labelFor = typeField
        panel.add(typeLabel, gbConstraints)

        gbConstraints.gridx++
        gbConstraints.insets = Insets(4, 4, 4, 8)
        gbConstraints.weightx = 1.0
        gbConstraints.fill = GridBagConstraints.BOTH
        panel.add(typeField, gbConstraints)

        if (lambdaExtractionDescriptor != null
            && (lambdaExtractionDescriptor.parameters.isNotEmpty() || lambdaExtractionDescriptor.receiverParameter != null)) {
            val parameterTablePanel = object : ExtractFunctionParameterTablePanel() {
                override fun onEnterAction() {
                    doOKAction()
                }

                override fun onCancelAction() {
                    doCancelAction()
                }
            }
            parameterTablePanel.init(lambdaExtractionDescriptor.receiverParameter, lambdaExtractionDescriptor.parameters)

            gbConstraints.insets = Insets(4, 4, 4, 8)
            gbConstraints.gridwidth = 1
            gbConstraints.weightx = 0.0
            gbConstraints.gridx = 0
            gbConstraints.gridy++
            gbConstraints.fill = GridBagConstraints.NONE
            val parametersLabel = JLabel("Lambda parameters: ")
            parametersLabel.setDisplayedMnemonic('p')
            parametersLabel.labelFor = parameterTablePanel
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
        defaultValueCheckBox.isSelected = descriptor.withDefaultValue
        defaultValueCheckBox.setMnemonic('d')
        defaultValueCheckBox.addActionListener { updateRemoveParamCheckBoxes() }
        panel.add(defaultValueCheckBox, gbConstraints)

        this.defaultValueCheckBox = defaultValueCheckBox

        val occurrenceCount = descriptor.occurrencesToReplace.size

        if (occurrenceCount > 1) {
            gbConstraints.gridy++
            val replaceAllCheckBox = NonFocusableCheckBox("Replace all occurrences ($occurrenceCount)")
            replaceAllCheckBox.isSelected = true
            replaceAllCheckBox.setMnemonic('R')
            replaceAllCheckBox.addActionListener { updateRemoveParamCheckBoxes() }
            panel.add(replaceAllCheckBox, gbConstraints)
            this.replaceAllCheckBox = replaceAllCheckBox
        }

        if (replaceAllCheckBox != null) {
            gbConstraints.insets = Insets(0, 16, 4, 8)
        }

        for (parameter in descriptor.parametersToRemove) {
            val removeWhat = if (parameter is KtParameter) "parameter '${parameter.name}'" else "receiver"
            val cb = NonFocusableCheckBox("Remove $removeWhat no longer used")

            removeParamsCheckBoxes[cb] = parameter
            cb.isSelected = true
            gbConstraints.gridy++
            panel.add(cb, gbConstraints)
        }

        return panel
    }

    override fun createCenterPanel() = null

    override fun canRun() {
        val psiFactory = KtPsiFactory(myProject)
        psiFactory.createExpressionIfPossible(nameField.enteredName.quoteIfNeeded()).validateElement("Invalid parameter name")
        psiFactory.createTypeIfPossible(typeField.enteredName).validateElement("Invalid parameter type")
    }

    override fun doAction() {
        performRefactoring()
    }

    fun performRefactoring() {
        close(DialogWrapper.OK_EXIT_CODE)

        project.executeCommand(commandName) {
            fun createLambdaForArgument(function: KtFunction): KtExpression {
                val statement = (function.bodyExpression as KtBlockExpression).statements.single()
                val space = if (statement.isMultiLine()) "\n" else " "
                val parameters = function.valueParameters
                val parametersText = if (parameters.isNotEmpty()) {
                    " " + parameters.map { it.name }.joinToString() + " ->"
                } else ""
                val text = "{$parametersText$space${statement.text}$space}"

                return KtPsiFactory(myProject).createExpression(text)
            }

            val chosenName = nameField.enteredName.quoteIfNeeded()
            var chosenType = typeField.enteredName
            var newArgumentValue = descriptor.newArgumentValue
            var newReplacer = descriptor.occurrenceReplacer

            val startMarkAction = StartMarkAction.start(editor, myProject, this@KotlinIntroduceParameterDialog.commandName)

            lambdaExtractionDescriptor?.let { oldDescriptor ->
                val newDescriptor = KotlinExtractFunctionDialog.createNewDescriptor(
                        oldDescriptor,
                        chosenName,
                        null,
                        parameterTablePanel?.selectedReceiverInfo,
                        parameterTablePanel?.selectedParameterInfos ?: listOf(),
                        null
                )
                val options = ExtractionGeneratorOptions.DEFAULT.copy(
                        target = ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION,
                        allowExpressionBody = false
                )
                runWriteAction {
                    with (ExtractionGeneratorConfiguration(newDescriptor, options).generateDeclaration()) {
                        val function = declaration as KtFunction
                        val receiverType = function.receiverTypeReference?.text
                        val parameterTypes = function
                            .valueParameters.joinToString { it.typeReference!!.text }
                        val returnType = function.typeReference?.text ?: "Unit"

                        chosenType = (receiverType?.let { "$it." } ?: "") + "($parameterTypes) -> $returnType"
                        if (KtTokens.SUSPEND_KEYWORD in newDescriptor.modifiers) {
                            chosenType = "${KtTokens.SUSPEND_KEYWORD} $chosenType"
                        }
                        newArgumentValue = createLambdaForArgument(function)
                        newReplacer = { }

                        processDuplicates(duplicateReplacers, myProject, editor)
                    }
                }
            }

            val descriptorToRefactor = descriptor.copy(
                    newParameterName = chosenName,
                    newParameterTypeText = chosenType,
                    argumentValue = newArgumentValue,
                    withDefaultValue = defaultValueCheckBox!!.isSelected,
                    occurrencesToReplace = with(descriptor) {
                        if (replaceAllCheckBox?.isSelected ?: true) {
                            occurrencesToReplace
                        }
                        else {
                            Collections.singletonList(originalOccurrence)
                        }
                    },
                    parametersToRemove = removeParamsCheckBoxes.filter { it.key.isEnabled && it.key.isSelected }.map { it.value },
                    occurrenceReplacer = newReplacer
            )

            val introduceParameter = { helper.configure(descriptorToRefactor).performRefactoring() }
            introduceParameter.runRefactoringWithPostprocessing(myProject, INTRODUCE_PARAMETER_REFACTORING_ID) {
                FinishMarkAction.finish(myProject, editor, startMarkAction)
            }
        }
    }
}
