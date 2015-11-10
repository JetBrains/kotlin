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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiCodeFragment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.changeSignature.ChangeSignatureDialogBase
import com.intellij.refactoring.changeSignature.MethodDescriptor
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase
import com.intellij.refactoring.ui.ComboBoxVisibilityPanel
import com.intellij.ui.DottedBorder
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Consumer
import com.intellij.util.IJSwingUtilities
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.table.JBTableRow
import com.intellij.util.ui.table.JBTableRowEditor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor.Kind
import org.jetbrains.kotlin.psi.KtExpressionCodeFragment
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeCodeFragment
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.util.*
import javax.swing.*

public class KotlinChangeSignatureDialog(
        project: Project,
        methodDescriptor: KotlinMethodDescriptor,
        context: PsiElement,
        private val commandName: String?
) : ChangeSignatureDialogBase<
        KotlinParameterInfo,
        PsiElement,
        Visibility,
        KotlinMethodDescriptor,
        ParameterTableModelItemBase<KotlinParameterInfo>,
        KotlinCallableParameterTableModel>(project, methodDescriptor, false, context) {
    override fun getFileType() = KotlinFileType.INSTANCE

    override fun createParametersInfoModel(descriptor: KotlinMethodDescriptor) = createParametersInfoModel(descriptor, myDefaultValueContext)

    override fun createReturnTypeCodeFragment() = createReturnTypeCodeFragment(myProject, myMethod)

    private val parametersTableModel: KotlinCallableParameterTableModel get() = super.myParametersTableModel
    
    override fun getRowPresentation(item: ParameterTableModelItemBase<KotlinParameterInfo>, selected: Boolean, focused: Boolean): JComponent? {
        val panel = JPanel(BorderLayout())

        val valOrVar: String
        if (myMethod.kind === Kind.PRIMARY_CONSTRUCTOR) {
            valOrVar = when (item.parameter.valOrVar) {
                KotlinValVar.None -> "    "
                KotlinValVar.Val -> "val "
                KotlinValVar.Var -> "var "
            }
        }
        else {
            valOrVar = ""
        }

        val parameterName = getPresentationName(item)
        val typeText = item.typeCodeFragment.getText()
        val defaultValue = item.defaultValueCodeFragment.getText()
        val separator = StringUtil.repeatSymbol(' ', getParamNamesMaxLength() - parameterName.length() + 1)
        var text = "$valOrVar$parameterName:$separator$typeText"

        if (StringUtil.isNotEmpty(defaultValue)) {
            text += " // default value = $defaultValue"
        }

        val field = object : EditorTextField(" $text", getProject(), getFileType()) {
            override fun shouldHaveBorder() = false
        }

        val plainFont  = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN)
        field.setFont(Font(plainFont.getFontName(), plainFont.getStyle(), 12))

        if (selected && focused) {
            panel.setBackground(UIUtil.getTableSelectionBackground())
            field.setAsRendererWithSelection(UIUtil.getTableSelectionBackground(), UIUtil.getTableSelectionForeground())
        }
        else {
            panel.setBackground(UIUtil.getTableBackground())
            if (selected && !focused) {
                panel.setBorder(DottedBorder(UIUtil.getTableForeground()))
            }
        }
        panel.add(field, BorderLayout.WEST)

        return panel
    }

    private fun getPresentationName(item: ParameterTableModelItemBase<KotlinParameterInfo>): String {
        val parameter = item.parameter
        return if (parameter == parametersTableModel.getReceiver()) "<receiver>" else parameter.getName()
    }

    private fun getColumnTextMaxLength(nameFunction: Function1<ParameterTableModelItemBase<KotlinParameterInfo>, String?>) =
            parametersTableModel.getItems().map { nameFunction(it)?.length() ?: 0 }.max() ?: 0

    private fun getParamNamesMaxLength() = getColumnTextMaxLength { getPresentationName(it) }

    private fun getTypesMaxLength() = getColumnTextMaxLength { it.typeCodeFragment?.getText() }

    private fun getDefaultValuesMaxLength() = getColumnTextMaxLength { it.defaultValueCodeFragment?.getText() }

    override fun isListTableViewSupported() = true

    override fun isEmptyRow(row: ParameterTableModelItemBase<KotlinParameterInfo>): Boolean {
        if (!row.parameter.getName().isNullOrEmpty()) return false
        if (!row.parameter.getTypeText().isNullOrEmpty()) return false
        return true
    }

    override fun createCallerChooser(title: String, treeToReuse: Tree?, callback: Consumer<Set<PsiElement>>) =
            KotlinCallerChooser(myMethod.getMethod(), myProject, title, treeToReuse, callback)

    // Forbid receiver propagation
    override fun mayPropagateParameters() =
            getParameters().any { it.isNewParameter && it != parametersTableModel.getReceiver() }

    override fun getTableEditor(table: JTable, item: ParameterTableModelItemBase<KotlinParameterInfo>): JBTableRowEditor? {
        return object : JBTableRowEditor() {
            private val components = ArrayList<JComponent>()
            private val nameEditor = EditorTextField(item.parameter.getName(), getProject(), getFileType())

            private fun updateNameEditor() {
                nameEditor.setEnabled(item.parameter != parametersTableModel.getReceiver())
            }

            private fun isDefaultColumnEnabled() =
                    item.parameter.isNewParameter && item.parameter != myMethod.receiver

            override fun prepareEditor(table: JTable, row: Int) {
                setLayout(BoxLayout(this, BoxLayout.X_AXIS))
                var column = 0

                for (columnInfo in parametersTableModel.getColumnInfos()) {
                    val panel = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 4, 2, true, false))
                    val editor: EditorTextField?
                    val component: JComponent
                    val columnFinal = column

                    if (KotlinCallableParameterTableModel.isTypeColumn(columnInfo)) {
                        val document = PsiDocumentManager.getInstance(getProject()).getDocument(item.typeCodeFragment)
                        editor = EditorTextField(document, getProject(), getFileType())
                        component = editor
                    }
                    else if (KotlinCallableParameterTableModel.isNameColumn(columnInfo)) {
                        editor = nameEditor
                        component = editor
                        updateNameEditor()
                    }
                    else if (KotlinCallableParameterTableModel.isDefaultValueColumn(columnInfo) && isDefaultColumnEnabled()) {
                        val document = PsiDocumentManager.getInstance(getProject()).getDocument(item.defaultValueCodeFragment)
                        editor = EditorTextField(document, getProject(), getFileType())
                        component = editor
                    }
                    else if (KotlinPrimaryConstructorParameterTableModel.isValVarColumn(columnInfo)) {
                        val comboBox = JComboBox(KotlinValVar.values())
                        comboBox.setSelectedItem(item.parameter.valOrVar)
                        comboBox.addItemListener(object : ItemListener {
                            override fun itemStateChanged(e: ItemEvent) {
                                parametersTableModel.setValueAtWithoutUpdate(e.getItem(), row, columnFinal)
                                updateSignature()
                            }
                        })
                        component = comboBox
                        editor = null
                    }
                    else if (KotlinFunctionParameterTableModel.isReceiverColumn(columnInfo)) {
                        val checkBox = JCheckBox()
                        checkBox.setSelected(parametersTableModel.getReceiver() == item.parameter)
                        checkBox.addItemListener {
                            val newReceiver = if (it.getStateChange() == ItemEvent.SELECTED) item.parameter else null
                            (parametersTableModel as KotlinFunctionParameterTableModel).setReceiver(newReceiver)
                            updateSignature()
                            updateNameEditor()
                        }
                        component = checkBox
                        editor = null
                    }
                    else
                        continue

                    val label = JBLabel(columnInfo.getName(), UIUtil.ComponentStyle.SMALL)
                    panel.add(label)

                    if (editor != null) {
                        editor.addDocumentListener(
                                object : DocumentAdapter() {
                                    override fun documentChanged(e: DocumentEvent?) {
                                        fireDocumentChanged(e, columnFinal)
                                    }
                                }
                        )
                        editor.setPreferredWidth(table.getWidth() / parametersTableModel.getColumnCount())
                    }

                    components.add(component)
                    panel.add(component)
                    add(panel)
                    IJSwingUtilities.adjustComponentsOnMac(label, component)
                    column++
                }
            }

            override fun getValue(): JBTableRow {
                return object : JBTableRow {
                    override fun getValueAt(column: Int): Any? {
                        val columnInfo = parametersTableModel.getColumnInfos()[column]

                        if (KotlinPrimaryConstructorParameterTableModel.isValVarColumn(columnInfo))
                            return (components.get(column) as @Suppress("NO_TYPE_ARGUMENTS_ON_RHS") JComboBox).getSelectedItem()
                        else if (KotlinCallableParameterTableModel.isTypeColumn(columnInfo))
                            return item.typeCodeFragment
                        else if (KotlinCallableParameterTableModel.isNameColumn(columnInfo))
                            return (components.get(column) as EditorTextField).getText()
                        else if (KotlinCallableParameterTableModel.isDefaultValueColumn(columnInfo))
                            return item.defaultValueCodeFragment
                        else
                            return null
                    }
                }
            }

            private fun getColumnWidth(letters: Int): Int {
                var font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN)
                font = Font(font.getFontName(), font.getStyle(), 12)
                return letters * Toolkit.getDefaultToolkit().getFontMetrics(font).stringWidth("W")
            }

            private fun getEditorIndex(x: Int): Int {
                @Suppress("NAME_SHADOWING") var x = x

                val columnLetters = if (isDefaultColumnEnabled())
                    intArrayOf(4, getParamNamesMaxLength(), getTypesMaxLength(), getDefaultValuesMaxLength())
                else
                    intArrayOf(4, getParamNamesMaxLength(), getTypesMaxLength())

                var columnIndex = 0
                for (i in (if (myMethod.kind === Kind.PRIMARY_CONSTRUCTOR) 0 else 1)..columnLetters.size() - 1) {
                    val width = getColumnWidth(columnLetters[i])

                    if (x <= width)
                        return columnIndex

                    columnIndex++
                    x -= width
                }

                return columnIndex - 1
            }

            override fun getPreferredFocusedComponent(): JComponent {
                val me = getMouseEvent()
                val index = if (me != null)
                    getEditorIndex(me.getPoint().getX().toInt())
                else if (myMethod.kind === Kind.PRIMARY_CONSTRUCTOR) 1 else 0
                val component = components.get(index)
                return if (component is EditorTextField) component.getFocusTarget() else component
            }

            override fun getFocusableComponents(): Array<JComponent> {
                return Array(components.size()) {
                    val component = components.get(it)
                    (component as? EditorTextField)?.getFocusTarget() ?: component
                }
            }
        }
    }

    override fun calculateSignature(): String {
        val changeInfo = evaluateChangeInfo(parametersTableModel,
                                            myReturnTypeCodeFragment,
                                            getMethodDescriptor(),
                                            getVisibility(),
                                            getMethodName(),
                                            myDefaultValueContext)
        return changeInfo.getNewSignature(getMethodDescriptor().originalPrimaryCallable)
    }

    override fun createVisibilityControl() = ComboBoxVisibilityPanel(
            arrayOf(Visibilities.INTERNAL, Visibilities.PRIVATE, Visibilities.PROTECTED, Visibilities.PUBLIC)
    )

    override fun updateSignatureAlarmFired() {
        super.updateSignatureAlarmFired()
        validateButtons()
    }

    override fun validateAndCommitData() = null

    override fun canRun() {
        if (myNamePanel.isVisible()
            && myMethod.canChangeName()
            && !JavaPsiFacade.getInstance(myProject).getNameHelper().isIdentifier(getMethodName())) {
            throw ConfigurationException(KotlinRefactoringBundle.message("function.name.is.invalid"))
        }

        if (myMethod.canChangeReturnType() === MethodDescriptor.ReadWriteOption.ReadWrite && !hasTypeReference(myReturnTypeCodeFragment)) {
            throw ConfigurationException(KotlinRefactoringBundle.message("return.type.is.invalid"))
        }

        val parameterInfos = parametersTableModel.getItems()

        for (item in parameterInfos) {
            val parameterName = item.parameter.getName()

            if (item.parameter != parametersTableModel.getReceiver()
                && !JavaPsiFacade.getInstance(myProject).getNameHelper().isIdentifier(parameterName)) {
                throw ConfigurationException(KotlinRefactoringBundle.message("parameter.name.is.invalid", parameterName))
            }

            if (!hasTypeReference(item.typeCodeFragment)) {
                throw ConfigurationException(KotlinRefactoringBundle.message("parameter.type.is.invalid", item.typeCodeFragment.getText()))
            }
        }
    }

    override fun createRefactoringProcessor(): BaseRefactoringProcessor {
        val changeInfo = evaluateChangeInfo(parametersTableModel,
                                            myReturnTypeCodeFragment,
                                            getMethodDescriptor(),
                                            getVisibility(),
                                            getMethodName(),
                                            myDefaultValueContext)
        changeInfo.primaryPropagationTargets = myMethodsToPropagateParameters ?: emptyList();
        return KotlinChangeSignatureProcessor(myProject, changeInfo, commandName ?: getTitle())
    }

    public fun getMethodDescriptor(): KotlinMethodDescriptor = myMethod

    override fun getSelectedIdx(): Int {
        return myMethod.getParameters().withIndex().firstOrNull { it.value.isNewParameter }?.index
               ?: super.getSelectedIdx()
    }

    companion object {
        private fun createParametersInfoModel(descriptor: KotlinMethodDescriptor, defaultValueContext: PsiElement): KotlinCallableParameterTableModel {
            return when (descriptor.kind) {
                KotlinMethodDescriptor.Kind.FUNCTION -> KotlinFunctionParameterTableModel(descriptor, defaultValueContext)
                KotlinMethodDescriptor.Kind.PRIMARY_CONSTRUCTOR -> KotlinPrimaryConstructorParameterTableModel(descriptor, defaultValueContext)
                KotlinMethodDescriptor.Kind.SECONDARY_CONSTRUCTOR -> KotlinSecondaryConstructorParameterTableModel(descriptor, defaultValueContext)
            }
        }

        private fun createReturnTypeCodeFragment(project: Project, method: KotlinMethodDescriptor) =
                KtPsiFactory(project).createTypeCodeFragment(method.renderOriginalReturnType(), method.baseDeclaration)

        public fun createRefactoringProcessorForSilentChangeSignature(project: Project,
                                                                      commandName: String,
                                                                      method: KotlinMethodDescriptor,
                                                                      defaultValueContext: PsiElement): BaseRefactoringProcessor {
            val parameterTableModel = createParametersInfoModel(method, defaultValueContext)
            parameterTableModel.setParameterInfos(method.getParameters())
            val changeInfo = evaluateChangeInfo(parameterTableModel,
                                                createReturnTypeCodeFragment(project, method),
                                                method,
                                                method.getVisibility(),
                                                method.getName(),
                                                defaultValueContext)
            return KotlinChangeSignatureProcessor(project, changeInfo, commandName)
        }

        private fun evaluateChangeInfo(parametersModel: KotlinCallableParameterTableModel,
                                       returnTypeCodeFragment: PsiCodeFragment?,
                                       methodDescriptor: KotlinMethodDescriptor,
                                       visibility: Visibility?,
                                       methodName: String,
                                       defaultValueContext: PsiElement): KotlinChangeInfo {
            val parameters = parametersModel.getItems().map { parameter ->
                val parameterInfo = parameter.parameter

                parameterInfo.currentTypeText = parameter.typeCodeFragment.getText().trim()
                val codeFragment = parameter.defaultValueCodeFragment as KtExpressionCodeFragment
                val oldDefaultValue = parameterInfo.defaultValueForCall
                if (codeFragment.getText() != (if (oldDefaultValue != null) oldDefaultValue.getText() else "")) {
                    parameterInfo.defaultValueForCall = codeFragment.getContentElement()
                }

                parameterInfo
            }

            val returnTypeText = if (returnTypeCodeFragment != null) returnTypeCodeFragment.getText().trim() else ""
            //TODO return the actual type
            val returnType = if (hasTypeReference(returnTypeCodeFragment)) methodDescriptor.baseDescriptor.builtIns.anyType else null
            return KotlinChangeInfo(methodDescriptor.original,
                                    methodName,
                                    returnType,
                                    returnTypeText,
                                 visibility ?: Visibilities.DEFAULT_VISIBILITY,
                                    parameters,
                                    parametersModel.getReceiver(),
                                    defaultValueContext)
        }

        private fun hasTypeReference(codeFragment: PsiCodeFragment?): Boolean
                = (codeFragment as? KtTypeCodeFragment)?.hasTypeReference() ?: false
    }
}
