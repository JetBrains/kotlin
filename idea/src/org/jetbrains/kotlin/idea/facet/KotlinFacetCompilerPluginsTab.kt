/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetEditorValidator
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.facet.ui.ValidationResult
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import org.jetbrains.kotlin.compiler.plugin.CliOptionValue
import org.jetbrains.kotlin.compiler.plugin.parsePluginOption
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class KotlinFacetCompilerPluginsTab(
        private val configuration: KotlinFacetConfiguration,
        private val validatorsManager: FacetValidatorsManager
) : FacetEditorTab() {
    companion object {
        fun parsePluginOptions(configuration: KotlinFacetConfiguration) =
                configuration.settings.compilerArguments?.pluginOptions?.mapNotNull(::parsePluginOption) ?: emptyList()
    }

    class PluginInfo(val id: String, var options: List<String>)

    private inner class PluginTableModel : AbstractTableModel() {
        var isModified = false
            private set(value) {
                if (value != field) {
                    validatorsManager.validate()
                }
            }

        val pluginInfos: List<PluginInfo> = ArrayList<PluginInfo>().apply {
            parsePluginOptions(configuration)
                    .sortedWith(
                            Comparator<CliOptionValue> { o1, o2 ->
                                var result = o1.pluginId.compareTo(o2.pluginId)
                                if (result == 0) {
                                    result = o1.optionName.compareTo(o2.optionName)
                                }
                                if (result == 0) {
                                    result = o1.value.compareTo(o2.value)
                                }
                                result
                            }
                    )
                    .groupBy({ it.pluginId })
                    .mapTo(this) { PluginInfo(it.key, it.value.map { "${it.optionName}=${it.value}" }) }
            sortBy { it.id }
        }

        override fun getColumnCount() = 2

        override fun getColumnName(column: Int) = when (column) {
            0 -> "Plugin"
            else -> "Options"
        }

        override fun getColumnClass(columnIndex: Int) = String::class.java

        override fun getRowCount() = pluginInfos.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val pluginInfo = pluginInfos[rowIndex]
            return when (columnIndex) {
                0 -> pluginInfo.id
                else -> pluginInfo.options.joinToString("\n")
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true
    }

    private class OptionRendererAndEditor : AbstractCellEditor(), TableCellEditor, TableCellRenderer {
        private val textPane = JTextPane().apply {
            isEditable = false
        }

        private fun setupComponent(table: JTable, value: Any?): Component {
            return textPane.apply {
                font = table.font
                text = value as? String ?: ""
                background = table.background
                foreground = table.foreground
            }
        }

        override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            return setupComponent(table, value)
        }

        override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
            return setupComponent(table, value)
        }

        override fun getCellEditorValue() = textPane.text!!
    }

    private inner class OptionValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val invalidOptions = optionsByTable.filter { parsePluginOption(it) == null }
            if (invalidOptions.isNotEmpty()) {
                val message = buildString {
                    append("Following options are not correct: <br/>")
                    invalidOptions.joinTo(this, "<br/>") { "<strong>$it</strong>" }
                }
                return ValidationResult(message)
            }

            return ValidationResult.OK
        }
    }

    private var table: JBTable? = null

    private val tableModel: PluginTableModel?
        get() = table?.model as? PluginTableModel

    private val optionsByTable: List<String>
        get() = tableModel?.pluginInfos?.flatMap { pluginInfo -> pluginInfo.options.map { "plugin:${pluginInfo.id}:$it" } } ?: emptyList()

    init {
        validatorsManager.registerValidator(OptionValidator())
    }

    override fun getDisplayName() = "Compiler Plugins"

    override fun createComponent(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(8, 0, 4, 0)
        val tableModel = PluginTableModel()
        table = object : JBTable(tableModel) {
            override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
                val component = super.prepareRenderer(renderer, row, column)
                val rendererWidth = component.preferredSize.width
                with(getColumnModel().getColumn(column)) {
                    preferredWidth = Math.max(rendererWidth + intercellSpacing.width, preferredWidth)
                }
                return component
            }
        }
        table!!.setDefaultRenderer(String::class.java, OptionRendererAndEditor())
        table!!.setDefaultEditor(String::class.java, OptionRendererAndEditor())
        table!!.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        val scrollPane = ScrollPaneFactory.createScrollPane(table)
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    override fun isModified() = tableModel?.isModified ?: false

    override fun reset() {
        table?.model = PluginTableModel()
    }

    override fun apply() {
        configuration.settings.compilerArguments!!.pluginOptions = optionsByTable.toTypedArray()
    }

    override fun disposeUIResources() {
    }
}