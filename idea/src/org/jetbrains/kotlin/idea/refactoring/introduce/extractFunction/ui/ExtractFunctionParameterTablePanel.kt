/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui

import com.intellij.ui.components.JBComboBoxLabel
import com.intellij.ui.components.editors.JBComboBoxTableCellEditorComponent
import com.intellij.util.ui.AbstractTableCellEditor
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.Parameter
import org.jetbrains.kotlin.idea.refactoring.introduce.ui.AbstractParameterTablePanel
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.types.KotlinType
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

open class ExtractFunctionParameterTablePanel : AbstractParameterTablePanel<Parameter, ExtractFunctionParameterTablePanel.ParameterInfo>() {
    companion object {
        const val PARAMETER_TYPE_COLUMN = 2
    }

    class ParameterInfo(
        originalParameter: Parameter,
        val isReceiver: Boolean
    ) : AbstractParameterTablePanel.AbstractParameterInfo<Parameter>(originalParameter) {
        var type = originalParameter.parameterType

        init {
            name = if (isReceiver) "<receiver>" else originalParameter.name
        }

        override fun toParameter() = originalParameter.copy(name, type)
    }

    override fun createTableModel(): TableModelBase = MyTableModel()

    override fun createAdditionalColumns() {
        with(table.columnModel.getColumn(PARAMETER_TYPE_COLUMN)) {
            headerValue = "Type"
            cellRenderer = object : DefaultTableCellRenderer() {
                private val myLabel = JBComboBoxLabel()

                override fun getTableCellRendererComponent(
                    table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    myLabel.text = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(value as KotlinType)
                    myLabel.background = if (isSelected) table.selectionBackground else table.background
                    myLabel.foreground = if (isSelected) table.selectionForeground else table.foreground
                    if (isSelected) {
                        myLabel.setSelectionIcon()
                    } else {
                        myLabel.setRegularIcon()
                    }
                    return myLabel
                }
            }
            cellEditor = object : AbstractTableCellEditor() {
                val myEditorComponent = JBComboBoxTableCellEditorComponent()

                override fun getCellEditorValue() = myEditorComponent.editorValue

                override fun getTableCellEditorComponent(
                    table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
                ): Component {
                    val info = parameterInfos[row]

                    myEditorComponent.setCell(table, row, column)
                    myEditorComponent.setOptions(*info.originalParameter.getParameterTypeCandidates().toTypedArray())
                    myEditorComponent.setDefaultValue(info.type)
                    myEditorComponent.setToString {
                        IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(it as KotlinType)
                    }

                    return myEditorComponent
                }
            }
        }
    }

    fun init(receiver: Parameter?, parameters: List<Parameter>) {
        parameterInfos = parameters.mapTo(
            if (receiver != null) arrayListOf(ParameterInfo(receiver, true)) else arrayListOf()
        ) { ParameterInfo(it, false) }

        super.init()
    }

    private inner class MyTableModel : TableModelBase() {
        override fun getColumnCount() = 3

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            if (columnIndex == PARAMETER_TYPE_COLUMN) return parameterInfos[rowIndex].type
            return super.getValueAt(rowIndex, columnIndex)
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == PARAMETER_TYPE_COLUMN) {
                parameterInfos[rowIndex].type = aValue as KotlinType
                updateSignature()
                return
            }

            super.setValueAt(aValue, rowIndex, columnIndex)
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            val info = parameterInfos[rowIndex]
            return when (columnIndex) {
                PARAMETER_NAME_COLUMN -> super.isCellEditable(rowIndex, columnIndex) && !info.isReceiver
                PARAMETER_TYPE_COLUMN -> isEnabled && info.isEnabled && info.originalParameter.getParameterTypeCandidates().size > 1
                else -> super.isCellEditable(rowIndex, columnIndex)
            }
        }
    }

    val selectedReceiverInfo: ParameterInfo?
        get() = parameterInfos.singleOrNull { it.isEnabled && it.isReceiver }

    val selectedParameterInfos: List<ParameterInfo>
        get() = parameterInfos.filter { it.isEnabled && !it.isReceiver }
}
