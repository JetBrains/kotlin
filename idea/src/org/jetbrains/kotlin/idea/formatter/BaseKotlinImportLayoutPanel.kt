/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.table.JBTable
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultCellEditor
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

open class BaseKotlinImportLayoutPanel(title: String): JPanel(BorderLayout()) {
    val packageTable = KotlinPackageEntryTable()
    val layoutTable = createTableForPackageEntries(packageTable)

    init {
        border = IdeBorderFactory.createTitledBorder(
            title,
            false,
            JBUI.emptyInsets()
        )
    }

    protected fun addPackage() {
        var row = layoutTable.selectedRow + 1
        if (row < 0) {
            row = packageTable.getEntryCount()
        }
        val entry = KotlinPackageEntry("", true)
        packageTable.insertEntryAt(entry, row)
        refreshTableModel(row)
    }

    protected fun removePackage() {
        var row = layoutTable.selectedRow
        if (row < 0) return

        val entry = packageTable.getEntryAt(row)
        if (entry == KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY || entry == KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY) {
            return
        }

        TableUtil.stopEditing(layoutTable)
        packageTable.removeEntryAt(row)

        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsDeleted(row, row)

        if (row >= packageTable.getEntryCount()) {
            row--
        }

        if (row >= 0) {
            layoutTable.setRowSelectionInterval(row, row)
        }
    }

    protected fun movePackageUp() {
        val row = layoutTable.selectedRow
        if (row < 1) return

        TableUtil.stopEditing(layoutTable)
        val entry = packageTable.getEntryAt(row)
        val previousEntry = packageTable.getEntryAt(row - 1)
        packageTable.setEntryAt(entry, row - 1)
        packageTable.setEntryAt(previousEntry, row)

        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsUpdated(row - 1, row)
        layoutTable.setRowSelectionInterval(row - 1, row - 1)
    }

    protected fun movePackageDown() {
        val row = layoutTable.selectedRow
        if (row >= packageTable.getEntryCount() - 1) return

        TableUtil.stopEditing(layoutTable)
        val entry = packageTable.getEntryAt(row)
        val nextEntry = packageTable.getEntryAt(row + 1)
        packageTable.setEntryAt(entry, row + 1)
        packageTable.setEntryAt(nextEntry, row)

        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsUpdated(row, row + 1)
        layoutTable.setRowSelectionInterval(row + 1, row + 1)
    }

    private fun refreshTableModel(row: Int) {
        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsInserted(row, row)
        layoutTable.setRowSelectionInterval(row, row)
        TableUtil.editCellAt(layoutTable, row, 0)
        val editorComp = layoutTable.editorComponent
        if (editorComp != null) {
            IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown { IdeFocusManager.getGlobalInstance().requestFocus(editorComp, true) }
        }
    }

    protected fun resizeColumns() {
        val packageRenderer: ColoredTableCellRenderer = object : ColoredTableCellRenderer() {
            override fun customizeCellRenderer(
                table: JTable,
                value: Any?,
                selected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ) {
                val entry = packageTable.getEntryAt(row)
                val attributes = KotlinHighlightingColors.KEYWORD.defaultAttributes
                append("import", SimpleTextAttributes.fromTextAttributes(attributes))
                append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)

                when (entry) {
                    KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY -> append(
                        "all other imports",
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    )

                    KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY -> append(
                        "all alias imports",
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    )

                    else -> append(
                        "${entry.packageName}.*",
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    )
                }
            }
        }

        layoutTable.columnModel.apply {
            getColumn(0).cellRenderer = packageRenderer
            getColumn(1).cellRenderer = BooleanTableCellRenderer()

            fixColumnWidthToHeader(1)
        }
    }

    private fun fixColumnWidthToHeader(columnIndex: Int) {
        with (layoutTable) {
            val column = columnModel.getColumn(columnIndex)
            val width = 15 + tableHeader.getFontMetrics(tableHeader.font).stringWidth(getColumnName(columnIndex))

            column.minWidth = width
            column.maxWidth = width
        }
    }
}

class KotlinStarImportLayoutPanel: BaseKotlinImportLayoutPanel(ApplicationBundle.message("title.packages.to.use.import.with")) {
    init {
        val importLayoutPanel = ToolbarDecorator.createDecorator(layoutTable)
            .setAddAction { addPackage() }
            .setRemoveAction { removePackage() }
            .setButtonComparator(
                "Add",
                "Remove"
            ).setPreferredSize(Dimension(-1, 100))
            .createPanel()

        add(importLayoutPanel, BorderLayout.CENTER)
        resizeColumns()
    }
}

class KotlinImportOrderLayoutPanel: BaseKotlinImportLayoutPanel(ApplicationBundle.message("title.import.layout")) {
    private val cbImportAliasesSeparately = JBCheckBox("Import aliases separately")

    init {
        add(cbImportAliasesSeparately, BorderLayout.NORTH)

        val importLayoutPanel = ToolbarDecorator.createDecorator(layoutTable)
            .addExtraAction(
                object: DumbAwareActionButton(ApplicationBundle.message("button.add.package"), IconUtil.getAddPackageIcon()) {
                    override fun actionPerformed(event: AnActionEvent) {
                        addPackage()
                    }

                    override fun getShortcut(): ShortcutSet {
                        return CommonShortcuts.getNewForDialogs()
                    }
                }
            )
            .setRemoveAction { removePackage() }
            .setMoveUpAction { movePackageUp() }
            .setMoveDownAction { movePackageDown() }
            .setRemoveActionUpdater {
                val selectedRow = layoutTable.selectedRow
                val entry = if (selectedRow in 0 until packageTable.getEntryCount()) packageTable.getEntryAt(selectedRow) else null

                entry != null && entry != KotlinPackageEntry.ALL_OTHER_IMPORTS_ENTRY && entry != KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
            }.setButtonComparator(
                ApplicationBundle.message("button.add.package"),
                "Remove",
                "Up",
                "Down"
            ).setPreferredSize(Dimension(-1, 100))
            .createPanel()

        add(importLayoutPanel, BorderLayout.CENTER)
        resizeColumns()

        cbImportAliasesSeparately.addItemListener { _ ->
            if (areImportAliasesEnabled()) {
                if (packageTable.getEntries().none { it == KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY }) {
                    packageTable.addEntry(KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY)
                    val row = packageTable.getEntryCount() - 1
                    val model = layoutTable.model as AbstractTableModel
                    model.fireTableRowsInserted(row, row)
                    layoutTable.setRowSelectionInterval(row, row)
                }
            } else {
                val entryIndex = packageTable.getEntries().indexOfFirst { it == KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY }

                if (entryIndex != -1) {
                    val currentIndex = layoutTable.selectedRow
                    packageTable.removeEntryAt(entryIndex)
                    val model = layoutTable.model as AbstractTableModel
                    model.fireTableRowsDeleted(entryIndex, entryIndex)

                    if (currentIndex < entryIndex) {
                        layoutTable.setRowSelectionInterval(currentIndex, currentIndex)
                    } else if (entryIndex > 0) {
                        layoutTable.setRowSelectionInterval(entryIndex - 1, entryIndex - 1)
                    }
                }
            }
        }
    }

    fun recomputeAliasesCheckbox() {
        cbImportAliasesSeparately.isSelected = packageTable.getEntries().any { it == KotlinPackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY }
    }

    private fun areImportAliasesEnabled(): Boolean {
        return cbImportAliasesSeparately.isSelected
    }
}

fun createTableForPackageEntries(packageTable: KotlinPackageEntryTable): JBTable {
    val names = arrayOf(ApplicationBundle.message("listbox.import.package"), ApplicationBundle.message("listbox.import.with.subpackages"))

    val dataModel = object : AbstractTableModel() {
        override fun getColumnCount(): Int {
            return names.size
        }

        override fun getRowCount(): Int {
            return packageTable.getEntryCount()
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val entry = packageTable.getEntryAt(rowIndex)
            if (!isCellEditable(rowIndex, columnIndex)) return null

            return when (columnIndex) {
                0 -> entry.packageName
                1 -> entry.withSubpackages
                else -> throw IllegalArgumentException(columnIndex.toString())
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            val entry = packageTable.getEntryAt(rowIndex)
            return !entry.isSpecial
        }

        override fun getColumnName(column: Int): String {
            return names[column]
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> String::class.java
                1 -> Boolean::class.javaObjectType
                else -> throw IllegalArgumentException(columnIndex.toString())
            }
        }

        override fun setValueAt(value: Any, rowIndex: Int, columnIndex: Int) {
            val entry = packageTable.getEntryAt(rowIndex)

            val newEntry = when (columnIndex) {
                0 -> KotlinPackageEntry((value as String).trim(), entry.withSubpackages)
                1 -> KotlinPackageEntry(entry.packageName, value.toString().toBoolean())
                else -> throw IllegalArgumentException(columnIndex.toString())
            }

            packageTable.setEntryAt(newEntry, rowIndex)
        }
    }

    // Create the table
    val result = JBTable(dataModel)
    result.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

    val editor = result.getDefaultEditor(String::class.java)
    if (editor is DefaultCellEditor) editor.clickCountToStart = 1

    return result
}

