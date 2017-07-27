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
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiCodeFragment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor.Kind
import org.jetbrains.kotlin.idea.refactoring.validateElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.event.ItemEvent
import java.util.*
import javax.swing.*

class KotlinChangeSignatureDialog(
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

        val valOrVar = if (myMethod.kind === Kind.PRIMARY_CONSTRUCTOR) {
            when (item.parameter.valOrVar) {
                KotlinValVar.None -> "    "
                KotlinValVar.Val -> "val "
                KotlinValVar.Var -> "var "
            }
        }
        else {
            ""
        }

        val parameterName = getPresentationName(item)
        val typeText = item.typeCodeFragment.text
        val defaultValue = item.defaultValueCodeFragment.text
        val separator = StringUtil.repeatSymbol(' ', getParamNamesMaxLength() - parameterName.length + 1)
        var text = "$valOrVar$parameterName:$separator$typeText"

        if (StringUtil.isNotEmpty(defaultValue)) {
            text += " // default value = $defaultValue"
        }

        val field = object : EditorTextField(" $text", project, fileType) {
            override fun shouldHaveBorder() = false
        }

        val plainFont  = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
        field.font = Font(plainFont.fontName, plainFont.style, 12)

        if (selected && focused) {
            panel.background = UIUtil.getTableSelectionBackground()
            field.setAsRendererWithSelection(UIUtil.getTableSelectionBackground(), UIUtil.getTableSelectionForeground())
        }
        else {
            panel.background = UIUtil.getTableBackground()
            if (selected && !focused) {
                panel.border = DottedBorder(UIUtil.getTableForeground())
            }
        }
        panel.add(field, BorderLayout.WEST)

        return panel
    }

    private fun getPresentationName(item: ParameterTableModelItemBase<KotlinParameterInfo>): String {
        val parameter = item.parameter
        return if (parameter == parametersTableModel.receiver) "<receiver>" else parameter.name
    }

    private fun getColumnTextMaxLength(nameFunction: Function1<ParameterTableModelItemBase<KotlinParameterInfo>, String?>) =
            parametersTableModel.items.map { nameFunction(it)?.length ?: 0 }.max() ?: 0

    private fun getParamNamesMaxLength() = getColumnTextMaxLength { getPresentationName(it) }

    private fun getTypesMaxLength() = getColumnTextMaxLength { it.typeCodeFragment?.text }

    private fun getDefaultValuesMaxLength() = getColumnTextMaxLength { it.defaultValueCodeFragment?.text }

    override fun isListTableViewSupported() = true

    override fun isEmptyRow(row: ParameterTableModelItemBase<KotlinParameterInfo>): Boolean {
        if (!row.parameter.name.isEmpty()) return false
        if (!row.parameter.typeText.isEmpty()) return false
        return true
    }

    override fun createCallerChooser(title: String, treeToReuse: Tree?, callback: Consumer<Set<PsiElement>>) =
            KotlinCallerChooser(myMethod.method, myProject, title, treeToReuse, callback)

    // Forbid receiver propagation
    override fun mayPropagateParameters() =
            parameters.any { it.isNewParameter && it != parametersTableModel.receiver }

    override fun getTableEditor(table: JTable, item: ParameterTableModelItemBase<KotlinParameterInfo>): JBTableRowEditor? {
        return object : JBTableRowEditor() {
            private val components = ArrayList<JComponent>()
            private val nameEditor = EditorTextField(item.parameter.name, project, fileType)

            private fun updateNameEditor() {
                nameEditor.isEnabled = item.parameter != parametersTableModel.receiver
            }

            private fun isDefaultColumnEnabled() =
                    item.parameter.isNewParameter && item.parameter != myMethod.receiver

            override fun prepareEditor(table: JTable, row: Int) {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                var column = 0

                for (columnInfo in parametersTableModel.columnInfos) {
                    val panel = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 4, 2, true, false))
                    val editor: EditorTextField?
                    val component: JComponent
                    val columnFinal = column

                    if (KotlinCallableParameterTableModel.isTypeColumn(columnInfo)) {
                        val document = PsiDocumentManager.getInstance(project).getDocument(item.typeCodeFragment)
                        editor = EditorTextField(document, project, fileType)
                        component = editor
                    }
                    else if (KotlinCallableParameterTableModel.isNameColumn(columnInfo)) {
                        editor = nameEditor
                        component = editor
                        updateNameEditor()
                    }
                    else if (KotlinCallableParameterTableModel.isDefaultValueColumn(columnInfo) && isDefaultColumnEnabled()) {
                        val document = PsiDocumentManager.getInstance(project).getDocument(item.defaultValueCodeFragment)
                        editor = EditorTextField(document, project, fileType)
                        component = editor
                    }
                    else if (KotlinPrimaryConstructorParameterTableModel.isValVarColumn(columnInfo)) {
                        val comboBox = JComboBox(KotlinValVar.values())
                        comboBox.selectedItem = item.parameter.valOrVar
                        comboBox.addItemListener {
                            parametersTableModel.setValueAtWithoutUpdate(it.item, row, columnFinal)
                            updateSignature()
                        }
                        component = comboBox
                        editor = null
                    }
                    else if (KotlinFunctionParameterTableModel.isReceiverColumn(columnInfo)) {
                        val checkBox = JCheckBox()
                        checkBox.isSelected = parametersTableModel.receiver == item.parameter
                        checkBox.addItemListener {
                            val newReceiver = if (it.stateChange == ItemEvent.SELECTED) item.parameter else null
                            (parametersTableModel as KotlinFunctionParameterTableModel).receiver = newReceiver
                            updateSignature()
                            updateNameEditor()
                        }
                        component = checkBox
                        editor = null
                    }
                    else
                        continue

                    val label = JBLabel(columnInfo.name, UIUtil.ComponentStyle.SMALL)
                    panel.add(label)

                    if (editor != null) {
                        editor.addDocumentListener(
                                object : DocumentAdapter() {
                                    override fun documentChanged(e: DocumentEvent?) {
                                        fireDocumentChanged(e, columnFinal)
                                    }
                                }
                        )
                        editor.setPreferredWidth(table.width / parametersTableModel.columnCount)
                    }

                    components.add(component)
                    panel.add(component)
                    add(panel)
                    IJSwingUtilities.adjustComponentsOnMac(label, component)
                    column++
                }
            }

            override fun getValue(): JBTableRow {
                return JBTableRow { column ->
                    val columnInfo = parametersTableModel.columnInfos[column]

                    when {
                        KotlinPrimaryConstructorParameterTableModel.isValVarColumn(columnInfo) ->
                            (components[column] as @Suppress("NO_TYPE_ARGUMENTS_ON_RHS") JComboBox).selectedItem
                        KotlinCallableParameterTableModel.isTypeColumn(columnInfo) ->
                            item.typeCodeFragment
                        KotlinCallableParameterTableModel.isNameColumn(columnInfo) ->
                            (components[column] as EditorTextField).text
                        KotlinCallableParameterTableModel.isDefaultValueColumn(columnInfo) ->
                            item.defaultValueCodeFragment
                        else ->
                            null
                    }
                }
            }

            private fun getColumnWidth(letters: Int): Int {
                var font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
                font = Font(font.fontName, font.style, 12)
                return letters * Toolkit.getDefaultToolkit().getFontMetrics(font).stringWidth("W")
            }

            private fun getEditorIndex(x: Int): Int {
                @Suppress("NAME_SHADOWING") var x = x

                val columnLetters = if (isDefaultColumnEnabled())
                    intArrayOf(4, getParamNamesMaxLength(), getTypesMaxLength(), getDefaultValuesMaxLength())
                else
                    intArrayOf(4, getParamNamesMaxLength(), getTypesMaxLength())

                var columnIndex = 0
                for (i in (if (myMethod.kind === Kind.PRIMARY_CONSTRUCTOR) 0 else 1)..columnLetters.size - 1) {
                    val width = getColumnWidth(columnLetters[i])

                    if (x <= width)
                        return columnIndex

                    columnIndex++
                    x -= width
                }

                return columnIndex - 1
            }

            override fun getPreferredFocusedComponent(): JComponent {
                val me = mouseEvent
                val index = when {
                    me != null -> getEditorIndex(me.point.getX().toInt())
                    myMethod.kind === Kind.PRIMARY_CONSTRUCTOR -> 1
                    else -> 0
                }
                val component = components[index]
                return if (component is EditorTextField) component.focusTarget else component
            }

            override fun getFocusableComponents(): Array<JComponent> {
                return Array(components.size) {
                    val component = components[it]
                    (component as? EditorTextField)?.focusTarget ?: component
                }
            }
        }
    }

    override fun calculateSignature(): String {
        val changeInfo = evaluateChangeInfo(parametersTableModel,
                                            myReturnTypeCodeFragment,
                                            getMethodDescriptor(),
                                            visibility,
                                            methodName,
                                            myDefaultValueContext,
                                            true)
        return changeInfo.getNewSignature(getMethodDescriptor().originalPrimaryCallable)
    }

    override fun createVisibilityControl() = ComboBoxVisibilityPanel(
            arrayOf(Visibilities.INTERNAL, Visibilities.PRIVATE, Visibilities.PROTECTED, Visibilities.PUBLIC)
    )

    override fun updateSignatureAlarmFired() {
        super.updateSignatureAlarmFired()
        validateButtons()
    }

    override fun validateAndCommitData(): String? {
        if (myMethod.canChangeReturnType() == MethodDescriptor.ReadWriteOption.ReadWrite &&
            myReturnTypeCodeFragment.getTypeInfo(true, false).type == null) {
            if (Messages.showOkCancelDialog(
                    myProject,
                    "Return type '${myReturnTypeCodeFragment!!.text}' cannot be resolved.\nContinue?",
                    RefactoringBundle.message("changeSignature.refactoring.name"),
                    Messages.getWarningIcon()
            ) != Messages.OK) {
                return ChangeSignatureDialogBase.EXIT_SILENTLY
            }
        }

        for (item in parametersTableModel.items) {
            if (item.typeCodeFragment.getTypeInfo(true, false).type == null) {
                val paramText = if (item.parameter != parametersTableModel.receiver) "parameter '${item.parameter.name}'" else "receiver"
                if (Messages.showOkCancelDialog(
                        myProject,
                        "Type '${item.typeCodeFragment.text}' for $paramText cannot be resolved.\nContinue?",
                        RefactoringBundle.message("changeSignature.refactoring.name"),
                        Messages.getWarningIcon()
                ) != Messages.OK) {
                    return ChangeSignatureDialogBase.EXIT_SILENTLY
                }
            }
        }
        return null
    }

    override fun canRun() {
        if (myNamePanel.isVisible && myMethod.canChangeName() && !KotlinNameSuggester.isIdentifier(methodName)) {
            throw ConfigurationException(KotlinRefactoringBundle.message("function.name.is.invalid"))
        }

        if (myMethod.canChangeReturnType() === MethodDescriptor.ReadWriteOption.ReadWrite) {
            (myReturnTypeCodeFragment as? KtTypeCodeFragment)
                    ?.validateElement(KotlinRefactoringBundle.message("return.type.is.invalid"))
        }

        for (item in parametersTableModel.items) {
            val parameterName = item.parameter.name

            if (item.parameter != parametersTableModel.receiver && !KotlinNameSuggester.isIdentifier(parameterName)) {
                throw ConfigurationException(KotlinRefactoringBundle.message("parameter.name.is.invalid", parameterName))
            }

            (item.typeCodeFragment as? KtTypeCodeFragment)
                    ?.validateElement(KotlinRefactoringBundle.message("parameter.type.is.invalid", item.typeCodeFragment.text))
        }
    }

    override fun createRefactoringProcessor(): BaseRefactoringProcessor {
        val changeInfo = evaluateChangeInfo(parametersTableModel,
                                            myReturnTypeCodeFragment,
                                            getMethodDescriptor(),
                                            visibility,
                                            methodName,
                                            myDefaultValueContext,
                                            false)
        changeInfo.primaryPropagationTargets = myMethodsToPropagateParameters ?: emptyList()
        return KotlinChangeSignatureProcessor(myProject, changeInfo, commandName ?: title)
    }

    private fun getMethodDescriptor(): KotlinMethodDescriptor = myMethod

    override fun getSelectedIdx(): Int {
        return myMethod.parameters.withIndex().firstOrNull { it.value.isNewParameter }?.index
               ?: super.getSelectedIdx()
    }

    companion object {
        private fun createParametersInfoModel(descriptor: KotlinMethodDescriptor, defaultValueContext: PsiElement): KotlinCallableParameterTableModel {
            val typeContext = getTypeCodeFragmentContext(defaultValueContext)
            return when (descriptor.kind) {
                KotlinMethodDescriptor.Kind.FUNCTION -> KotlinFunctionParameterTableModel(descriptor, typeContext, defaultValueContext)
                KotlinMethodDescriptor.Kind.PRIMARY_CONSTRUCTOR -> KotlinPrimaryConstructorParameterTableModel(descriptor, typeContext, defaultValueContext)
                KotlinMethodDescriptor.Kind.SECONDARY_CONSTRUCTOR -> KotlinSecondaryConstructorParameterTableModel(descriptor, typeContext, defaultValueContext)
            }
        }

        fun getTypeCodeFragmentContext(startFrom: PsiElement): KtElement {
            return startFrom.parentsWithSelf.mapNotNull {
                when {
                    it is KtNamedFunction -> it.bodyExpression ?: it.valueParameterList
                    it is KtPropertyAccessor -> it.bodyExpression
                    it is KtDeclaration && KtPsiUtil.isLocal(it) -> null
                    it is KtConstructor<*> -> it
                    it is KtClassOrObject -> it
                    it is KtFile -> it
                    else -> null
                }
            }.first()
        }

        private fun createReturnTypeCodeFragment(project: Project, method: KotlinMethodDescriptor) =
                KtPsiFactory(project).createTypeCodeFragment(method.returnTypeInfo.render(), getTypeCodeFragmentContext(method.baseDeclaration))

        fun createRefactoringProcessorForSilentChangeSignature(project: Project,
                                                                      commandName: String,
                                                                      method: KotlinMethodDescriptor,
                                                                      defaultValueContext: PsiElement): BaseRefactoringProcessor {
            val parameterTableModel = createParametersInfoModel(method, defaultValueContext)
            parameterTableModel.setParameterInfos(method.parameters)
            val changeInfo = evaluateChangeInfo(parameterTableModel,
                                                createReturnTypeCodeFragment(project, method),
                                                method,
                                                method.visibility,
                                                method.name,
                                                defaultValueContext,
                                                false)
            return KotlinChangeSignatureProcessor(project, changeInfo, commandName)
        }

        fun PsiCodeFragment?.getTypeInfo(isCovariant: Boolean, forPreview: Boolean): KotlinTypeInfo {
            if (this !is KtTypeCodeFragment) return KotlinTypeInfo(isCovariant)

            val typeRef = getContentElement()
            val type = typeRef?.analyze(BodyResolveMode.PARTIAL)?.get(BindingContext.TYPE, typeRef)
            return when {
                type != null && !type.isError -> KotlinTypeInfo(isCovariant, type, if (forPreview) typeRef.text else null)
                typeRef != null -> KotlinTypeInfo(isCovariant, null, typeRef.text)
                else -> KotlinTypeInfo(isCovariant)
            }
        }

        private fun evaluateChangeInfo(parametersModel: KotlinCallableParameterTableModel,
                                       returnTypeCodeFragment: PsiCodeFragment?,
                                       methodDescriptor: KotlinMethodDescriptor,
                                       visibility: Visibility?,
                                       methodName: String,
                                       defaultValueContext: PsiElement,
                                       forPreview: Boolean): KotlinChangeInfo {
            val parameters = parametersModel.items.map { parameter ->
                val parameterInfo = parameter.parameter

                parameterInfo.currentTypeInfo = parameter.typeCodeFragment.getTypeInfo(false, forPreview)

                val codeFragment = parameter.defaultValueCodeFragment as KtExpressionCodeFragment
                val oldDefaultValue = parameterInfo.defaultValueForCall
                if (codeFragment.text != (if (oldDefaultValue != null) oldDefaultValue.text else "")) {
                    parameterInfo.defaultValueForCall = codeFragment.getContentElement()
                }

                parameterInfo
            }

            return KotlinChangeInfo(methodDescriptor.original,
                                    methodName,
                                    returnTypeCodeFragment.getTypeInfo(true, forPreview),
                                    visibility ?: Visibilities.DEFAULT_VISIBILITY,
                                    parameters,
                                    parametersModel.receiver,
                                    defaultValueContext)
        }
    }
}
