/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.introduce.ui

import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.TableUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.EditableModel
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

abstract class AbstractParameterTablePanel<Param, UIParam : AbstractParameterTablePanel.AbstractParameterInfo<Param>> : JPanel(BorderLayout()) {
    companion object {
        val CHECKMARK_COLUMN = 0
        val PARAMETER_NAME_COLUMN = 1
    }

    abstract class AbstractParameterInfo<out Param>(val originalParameter: Param) {
        var isEnabled = true
        lateinit var name: String

        abstract fun toParameter(): Param
    }

    protected lateinit var parameterInfos: MutableList<UIParam>

    lateinit var table: JBTable
        private set

    protected lateinit var tableModel: TableModelBase
        private set

    protected open fun createTableModel() = TableModelBase()

    protected open fun createAdditionalColumns() {

    }

    fun init() {
        tableModel = createTableModel()
        table = JBTable(tableModel)

        val defaultEditor = table.getDefaultEditor(Any::class.java) as DefaultCellEditor
        defaultEditor.clickCountToStart = 1

        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.cellSelectionEnabled = true

        with(table.columnModel.getColumn(CHECKMARK_COLUMN)) {
            TableUtil.setupCheckboxColumn(this)
            headerValue = ""
            cellRenderer = object : BooleanTableCellRenderer() {
                override fun getTableCellRendererComponent(
                        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    rendererComponent.isEnabled = this@AbstractParameterTablePanel.isEnabled
                    (rendererComponent as JCheckBox).addActionListener { updateSignature() }
                    return rendererComponent
                }
            }
        }

        table.columnModel.getColumn(PARAMETER_NAME_COLUMN).headerValue = "Name"

        createAdditionalColumns()

        table.preferredScrollableViewportSize = Dimension(250, table.rowHeight * 5)
        table.setShowGrid(false)
        table.intercellSpacing = Dimension(0, 0)

        @NonNls val inputMap = table.inputMap
        @NonNls val actionMap = table.actionMap

        // SPACE: toggle enable/disable
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "enable_disable")
        actionMap.put("enable_disable", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (table.isEditing) return
                val rows = table.selectedRows
                if (rows.size > 0) {
                    var valueToBeSet = false
                    for (row in rows) {
                        if (!parameterInfos[row].isEnabled) {
                            valueToBeSet = true
                            break
                        }
                    }
                    for (row in rows) {
                        parameterInfos[row].isEnabled = valueToBeSet
                    }
                    tableModel.fireTableRowsUpdated(rows[0], rows[rows.size - 1])
                    TableUtil.selectRows(table, rows)

                    updateSignature()
                }
            }
        })

        // make ENTER work when the table has focus
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "invoke_impl")
        actionMap.put("invoke_impl", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                table.cellEditor?.stopCellEditing() ?: onEnterAction()
            }
        })

        // make ESCAPE work when the table has focus
        actionMap.put("doCancel", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                table.cellEditor?.stopCellEditing() ?: onCancelAction()
            }
        })

        val listPanel = ToolbarDecorator.createDecorator(table).disableAddAction().disableRemoveAction().createPanel()
        add(listPanel, BorderLayout.CENTER)
    }

    protected open fun updateSignature() {

    }

    protected open fun onEnterAction() {

    }

    protected open fun onCancelAction() {

    }

    protected open fun isCheckMarkColumnEditable() = true

    protected open inner class TableModelBase : AbstractTableModel(), EditableModel {
        override fun addRow() = throw IllegalAccessError("Not implemented")

        override fun removeRow(index: Int) = throw IllegalAccessError("Not implemented")

        override fun exchangeRows(oldIndex: Int, newIndex: Int) {
            if (oldIndex < 0 || newIndex < 0) return
            if (oldIndex >= parameterInfos.size || newIndex >= parameterInfos.size) return

            val old = parameterInfos[oldIndex]
            parameterInfos[oldIndex] = parameterInfos[newIndex]
            parameterInfos[newIndex] = old

            fireTableRowsUpdated(Math.min(oldIndex, newIndex), Math.max(oldIndex, newIndex))
            updateSignature()
        }

        override fun canExchangeRows(oldIndex: Int, newIndex: Int): Boolean {
            return when {
                oldIndex < 0 || newIndex < 0 -> false
                oldIndex >= parameterInfos.size || newIndex >= parameterInfos.size -> false
                else -> true
            }
        }

        override fun getColumnCount() = 2

        override fun getRowCount() = parameterInfos.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            return when (columnIndex) {
                CHECKMARK_COLUMN -> parameterInfos[rowIndex].isEnabled
                PARAMETER_NAME_COLUMN -> parameterInfos[rowIndex].name
                else -> null
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val info = parameterInfos[rowIndex]
            when (columnIndex) {
                CHECKMARK_COLUMN -> {
                    info.isEnabled = aValue as Boolean
                    fireTableRowsUpdated(rowIndex, rowIndex)
                    table.selectionModel.setSelectionInterval(rowIndex, rowIndex)
                    updateSignature()
                }
                PARAMETER_NAME_COLUMN -> {
                    val name = aValue as String
                    if (KotlinNameSuggester.isIdentifier(name)) {
                        info.name = name
                    }
                    updateSignature()
                }
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            val info = parameterInfos[rowIndex]
            return when (columnIndex) {
                CHECKMARK_COLUMN -> isEnabled && isCheckMarkColumnEditable()
                PARAMETER_NAME_COLUMN -> isEnabled && info.isEnabled
                else -> false
            }
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            if (columnIndex == CHECKMARK_COLUMN) return Boolean::class.java
            return super.getColumnClass(columnIndex)
        }
    }
}
