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

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameHelper
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.ui.NameSuggestionsField
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.usageView.BaseUsageViewDescriptor
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.types.JetType
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

public class KotlinIntroduceParameterDialog(
        project: Project,
        val descriptor: IntroduceParameterDescriptor,
        nameSuggestions: Array<String>,
        val typeSuggestions: List<JetType>
): RefactoringDialog(project, true) {
    private val typeNameSuggestions = typeSuggestions
            .map { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) }
            .copyToArray()

    private val nameField = NameSuggestionsField(nameSuggestions, project, JetFileType.INSTANCE)
    private val typeField = NameSuggestionsField(typeNameSuggestions, project, JetFileType.INSTANCE)
    private var replaceAllCheckBox: JCheckBox? = null
    private val removeParamsCheckBoxes = LinkedHashMap<JCheckBox, JetParameter>(descriptor.parametersToRemove.size())

    init {
        setTitle(INTRODUCE_PARAMETER)
        init()

        nameField.addDataChangedListener { validateButtons() }
        typeField.addDataChangedListener { validateButtons() }
    }

    override fun getPreferredFocusedComponent() = nameField.getFocusableComponent()

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
        val nameLabel = JLabel("Parameter name:")
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
        gbConstraints.gridy = 1
        gbConstraints.fill = GridBagConstraints.NONE
        val typeLabel = JLabel("Parameter type: ")
        typeLabel.setDisplayedMnemonic('t')
        typeLabel.setLabelFor(typeField)
        panel.add(typeLabel, gbConstraints)

        gbConstraints.gridx++
        gbConstraints.insets = Insets(4, 4, 4, 8)
        gbConstraints.weightx = 1.0
        gbConstraints.fill = GridBagConstraints.BOTH
        panel.add(typeField, gbConstraints)

        gbConstraints.gridx = 0
        gbConstraints.insets = Insets(4, 0, 4, 8)
        gbConstraints.gridwidth = 2
        val occurrenceCount = descriptor.occurrencesToReplace.size()
        if (occurrenceCount > 1) {
            gbConstraints.gridy++
            val replaceAllCheckBox = NonFocusableCheckBox("Replace all occurrences ($occurrenceCount)")
            replaceAllCheckBox.setSelected(true)
            replaceAllCheckBox.setMnemonic('R')
            replaceAllCheckBox.addActionListener {
                val enableParamRemove = replaceAllCheckBox.isSelected()
                removeParamsCheckBoxes.keySet().forEach {
                    it.setEnabled(enableParamRemove)
                    it.setSelected(enableParamRemove)
                }
            }
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
        val descriptorToRefactor = descriptor.copy(
                addedParameter = JetPsiFactory(myProject).createParameter("${nameField.getEnteredName()}: ${typeField.getEnteredName()}"),
                occurrencesToReplace = with(descriptor) {
                    if (replaceAllCheckBox?.isSelected() ?: true) {
                        occurrencesToReplace
                    }
                    else {
                        Collections.singletonList(originalOccurrence)
                    }
                }
        )
        invokeRefactoring(
                object: BaseRefactoringProcessor(myProject) {
                    override fun findUsages() = UsageInfo.EMPTY_ARRAY

                    override fun performRefactoring(usages: Array<out UsageInfo>) {
                        descriptorToRefactor.performRefactoring(
                                parametersToRemove =
                                removeParamsCheckBoxes.filter { it.key.isEnabled() && it.key.isSelected() }.map { it.value }
                        )
                    }

                    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>) = BaseUsageViewDescriptor()

                    override fun getCommandName() = INTRODUCE_PARAMETER
                }
        )
    }
}
