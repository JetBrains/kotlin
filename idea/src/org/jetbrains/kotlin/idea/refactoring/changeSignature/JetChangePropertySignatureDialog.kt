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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.ui.ComboBoxVisibilityPanel
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.ui.EditorTextField
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.core.refactoring.validateElement
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetTypeCodeFragment
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.properties.Delegates

public class JetChangePropertySignatureDialog(
        project: Project,
        private val methodDescriptor: JetMethodDescriptor,
        private val commandName: String?
): RefactoringDialog(project, true) {
    private val visibilityCombo = JComboBox(
            arrayOf(Visibilities.INTERNAL, Visibilities.PRIVATE, Visibilities.PROTECTED, Visibilities.PUBLIC)
    )
    private val nameField = EditorTextField(methodDescriptor.getName())
    private var returnTypeField: EditorTextField by Delegates.notNull()
    private var receiverTypeCheckBox: JCheckBox? = null
    var receiverTypeLabel: JLabel by Delegates.notNull()
    private var receiverTypeField: EditorTextField by Delegates.notNull()
    var receiverDefaultValueLabel: JLabel? = null
    private var receiverDefaultValueField: EditorTextField? = null

    init {
        setTitle("Change Signature")
        init()
    }

    override fun getPreferredFocusedComponent() = nameField

    override fun createCenterPanel(): JComponent? {
        fun updateReceiverUI() {
            val withReceiver = receiverTypeCheckBox!!.isSelected()
            receiverTypeLabel.setEnabled(withReceiver)
            receiverTypeField.setEnabled(withReceiver)
            receiverDefaultValueLabel?.setEnabled(withReceiver)
            receiverDefaultValueField?.setEnabled(withReceiver)
        }

        val documentManager = PsiDocumentManager.getInstance(myProject)
        val psiFactory = JetPsiFactory(myProject)

        return with(FormBuilder.createFormBuilder()) {
            val baseDeclaration = methodDescriptor.baseDeclaration
            if (!((baseDeclaration as? JetProperty)?.isLocal() ?: false)) {
                visibilityCombo.setSelectedItem(methodDescriptor.getVisibility())
                addLabeledComponent("&Visibility: ", visibilityCombo)
            }

            addLabeledComponent("&Name: ", nameField)

            val returnTypeCodeFragment = psiFactory.createTypeCodeFragment(methodDescriptor.renderOriginalReturnType(),
                                                                           baseDeclaration)
            returnTypeField = EditorTextField(documentManager.getDocument(returnTypeCodeFragment), myProject, JetFileType.INSTANCE)
            addLabeledComponent("&Type: ", returnTypeField)

            if (baseDeclaration is JetProperty) {
                addSeparator()

                val receiverTypeCheckBox = JCheckBox("Extension property: ")
                receiverTypeCheckBox.setMnemonic('x')
                receiverTypeCheckBox.addActionListener { updateReceiverUI() }
                receiverTypeCheckBox.setSelected(methodDescriptor.receiver != null)
                addComponent(receiverTypeCheckBox)
                this@JetChangePropertySignatureDialog.receiverTypeCheckBox = receiverTypeCheckBox

                val receiverTypeCodeFragment = psiFactory.createTypeCodeFragment(methodDescriptor.renderOriginalReceiverType() ?: "",
                                                                                 methodDescriptor.baseDeclaration)
                receiverTypeField = EditorTextField(documentManager.getDocument(receiverTypeCodeFragment), myProject, JetFileType.INSTANCE)
                receiverTypeLabel = JLabel("Receiver type: ")
                receiverTypeLabel.setDisplayedMnemonic('t')
                addLabeledComponent(receiverTypeLabel, receiverTypeField)

                if (methodDescriptor.receiver == null) {
                    val receiverDefaultValueCodeFragment = psiFactory.createExpressionCodeFragment("", methodDescriptor.baseDeclaration)
                    receiverDefaultValueField = EditorTextField(documentManager.getDocument(receiverDefaultValueCodeFragment),
                                                                myProject,
                                                                JetFileType.INSTANCE)
                    receiverDefaultValueLabel = JLabel("Default receiver value: ")
                    receiverDefaultValueLabel!!.setDisplayedMnemonic('D')
                    addLabeledComponent(receiverDefaultValueLabel, receiverDefaultValueField!!)
                }

                updateReceiverUI()
            }

            getPanel()
        }
    }

    private fun getDefaultReceiverValue(): JetExpression? {
        val receiverDefaultValue = receiverDefaultValueField?.getText() ?: ""
        return if (receiverDefaultValue.isNotEmpty()) JetPsiFactory(myProject).createExpression(receiverDefaultValue) else null
    }

    override fun canRun() {
        val psiFactory = JetPsiFactory(myProject)

        psiFactory.createSimpleName(nameField.getText()).validateElement("Invalid name")
        psiFactory.createType(returnTypeField.getText()).validateElement("Invalid return type")
        if (receiverTypeCheckBox?.isSelected() ?: false) {
            psiFactory.createType(receiverTypeField.getText()).validateElement("Invalid receiver type")
        }
        getDefaultReceiverValue()?.validateElement("Invalid default receiver value")
    }

    override fun doAction() {
        val descriptor = (methodDescriptor as? JetMutableMethodDescriptor)?.original ?: methodDescriptor

        val receiver = if (receiverTypeCheckBox?.isSelected() ?: false) {
            descriptor.receiver ?: JetParameterInfo(callableDescriptor = descriptor.baseDescriptor,
                                                    name = "receiver",
                                                    defaultValueForCall = getDefaultReceiverValue())
        } else null
        receiver?.currentTypeText = receiverTypeField.getText()
        val changeInfo = JetChangeInfo(descriptor,
                                       nameField.getText(),
                                       null,
                                       returnTypeField.getText(),
                                       visibilityCombo.getSelectedItem() as Visibility,
                                       emptyList(),
                                       receiver,
                                       descriptor.getMethod())

        invokeRefactoring(JetChangeSignatureProcessor(myProject, changeInfo, commandName ?: getTitle()))
    }

    companion object {
        fun createProcessorForSilentRefactoring(
                project: Project,
                commandName: String,
                descriptor: JetMethodDescriptor
        ): BaseRefactoringProcessor {
            val originalDescriptor = (descriptor as? JetMutableMethodDescriptor)?.original ?: descriptor
            val changeInfo = JetChangeInfo(methodDescriptor = originalDescriptor, context = originalDescriptor.getMethod())
            changeInfo.setNewName(descriptor.getName())
            changeInfo.receiverParameterInfo = descriptor.receiver
            return JetChangeSignatureProcessor(project, changeInfo, commandName)
        }
    }
}