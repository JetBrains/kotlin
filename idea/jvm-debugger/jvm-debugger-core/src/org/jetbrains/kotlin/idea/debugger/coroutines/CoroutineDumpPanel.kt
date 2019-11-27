/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines

import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.execution.ui.ConsoleView
import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Debugger.ThreadStates.Daemon_sign
import com.intellij.ide.DataManager
import com.intellij.ide.ExporterToTextFile
import com.intellij.ide.ui.UISettings
import com.intellij.notification.NotificationGroup
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.unscramble.AnalyzeStacktraceUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.EmptyIcon
import java.awt.BorderLayout
import java.awt.Color
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent

/**
 * Panel with dump of coroutines
 */
class CoroutineDumpPanel(project: Project, consoleView: ConsoleView, toolbarActions: DefaultActionGroup, val dump: List<CoroutineState>) :
    JPanel(BorderLayout()), DataProvider {
    private var exporterToTextFile: ExporterToTextFile
    private var mergedDump = ArrayList<CoroutineState>()
    val filterField = SearchTextField()
    val filterPanel = JPanel(BorderLayout())
    private val coroutinesList = JBList(DefaultListModel<Any>())

    init {
        mergedDump.addAll(dump)

        filterField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateCoroutinesList()
            }
        })

        filterPanel.apply {
            add(JLabel("Filter:"), BorderLayout.WEST)
            add(filterField)
            isVisible = false
        }

        coroutinesList.apply {
            cellRenderer = CoroutineListCellRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener {
                val index = selectedIndex
                if (index >= 0) {
                    val selection = model.getElementAt(index) as CoroutineState
                    AnalyzeStacktraceUtil.printStacktrace(consoleView, selection.stringStackTrace)
                } else {
                    AnalyzeStacktraceUtil.printStacktrace(consoleView, "")
                }
                repaint()
            }
        }

        exporterToTextFile = MyToFileExporter(project, dump)

        val filterAction = FilterAction().apply {
            registerCustomShortcutSet(
                ActionManager.getInstance().getAction(IdeActions.ACTION_FIND).shortcutSet,
                coroutinesList
            )
        }
        toolbarActions.apply {
            add(filterAction)
            add(CopyToClipboardAction(dump, project))
            add(ActionManager.getInstance().getAction(IdeActions.ACTION_EXPORT_TO_TEXT_FILE))
            add(MergeStackTracesAction())
        }
        add(
            ActionManager.getInstance()
                .createActionToolbar("CoroutinesDump", toolbarActions, false).component,
            BorderLayout.WEST
        )

        val leftPanel = JPanel(BorderLayout()).apply {
            add(filterPanel, BorderLayout.NORTH)
            add(ScrollPaneFactory.createScrollPane(coroutinesList, SideBorder.LEFT or SideBorder.RIGHT), BorderLayout.CENTER)
        }

        val splitter = Splitter(false, 0.3f).apply {
            firstComponent = leftPanel
            secondComponent = consoleView.component
        }
        add(splitter, BorderLayout.CENTER)

        ListSpeedSearch(coroutinesList).comparator = SpeedSearchComparator(false, true)

        updateCoroutinesList()

        val editor = CommonDataKeys.EDITOR.getData(
            DataManager.getInstance()
                .getDataContext(consoleView.preferredFocusableComponent)
        )
        editor?.document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(e: com.intellij.openapi.editor.event.DocumentEvent) {
                val filter = filterField.text
                if (StringUtil.isNotEmpty(filter)) {
                    highlightOccurrences(filter, project, editor)
                }
            }
        }, consoleView)
    }

    private fun updateCoroutinesList() {
        val text = if (filterPanel.isVisible) filterField.text else ""
        val selection = coroutinesList.selectedValue
        val model = coroutinesList.model as DefaultListModel<Any>
        model.clear()
        var selectedIndex = 0
        var index = 0
        val states = if (UISettings.instance.state.mergeEqualStackTraces) mergedDump else dump
        for (state in states) {
            if (StringUtil.containsIgnoreCase(state.stringStackTrace, text) || StringUtil.containsIgnoreCase(state.name, text)) {
                model.addElement(state)
                if (selection === state) {
                    selectedIndex = index
                }
                index++
            }
        }
        if (!model.isEmpty) {
            coroutinesList.selectedIndex = selectedIndex
        }
        coroutinesList.revalidate()
        coroutinesList.repaint()
    }

    internal fun highlightOccurrences(filter: String, project: Project, editor: Editor) {
        val highlightManager = HighlightManager.getInstance(project)
        val colorManager = EditorColorsManager.getInstance()
        val attributes = colorManager.globalScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES)
        val documentText = editor.document.text
        var i = -1
        while (true) {
            val nextOccurrence = StringUtil.indexOfIgnoreCase(documentText, filter, i + 1)
            if (nextOccurrence < 0) {
                break
            }
            i = nextOccurrence
            highlightManager.addOccurrenceHighlight(
                editor, i, i + filter.length, attributes,
                HighlightManager.HIDE_BY_TEXT_CHANGE, null, null
            )
        }
    }

    override fun getData(dataId: String): Any? = if (PlatformDataKeys.EXPORTER_TO_TEXT_FILE.`is`(dataId)) exporterToTextFile else null

    private fun getCoroutineStateIcon(state: CoroutineState): Icon {
        return when (state.state) {
            CoroutineState.State.RUNNING -> LayeredIcon(AllIcons.Actions.Resume, Daemon_sign)
            CoroutineState.State.SUSPENDED -> AllIcons.Actions.Pause
            else -> EmptyIcon.create(6)
        }
    }

    private fun getAttributes(state: CoroutineState): SimpleTextAttributes {
        return when {
            state.isSuspended -> SimpleTextAttributes.GRAY_ATTRIBUTES
            state.isEmptyStackTrace -> SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.GRAY.brighter())
            else -> SimpleTextAttributes.REGULAR_ATTRIBUTES

        }
    }

    private inner class CoroutineListCellRenderer : ColoredListCellRenderer<Any>() {

        override fun customizeCellRenderer(list: JList<*>, value: Any, index: Int, selected: Boolean, hasFocus: Boolean) {
            val state = value as CoroutineState
            icon = getCoroutineStateIcon(state)
            val attrs = getAttributes(state)
            append(state.name + " (", attrs)
            var detail: String? = state.state.name
            if (detail == null) {
                detail = state.state.name
            }
            if (detail.length > 30) {
                detail = detail.substring(0, 30) + "..."
            }
            append(detail, attrs)
            append(")", attrs)
        }
    }

    private inner class FilterAction : ToggleAction(
        "Filter",
        "Show only coroutines containing a specific string",
        AllIcons.General.Filter
    ), DumbAware {

        override fun isSelected(e: AnActionEvent): Boolean {
            return filterPanel.isVisible
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            filterPanel.isVisible = state
            if (state) {
                IdeFocusManager.getInstance(AnAction.getEventProject(e)).requestFocus(filterField, true)
                filterField.selectText()
            }
            updateCoroutinesList()
        }
    }

    private inner class MergeStackTracesAction : ToggleAction(
        "Merge Identical Stacktraces",
        "Group coroutines with identical stacktraces",
        AllIcons.Actions.Collapseall
    ), DumbAware {

        override fun isSelected(e: AnActionEvent): Boolean {
            return UISettings.instance.state.mergeEqualStackTraces
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            UISettings.instance.state.mergeEqualStackTraces = state
            updateCoroutinesList()
        }
    }

    private class CopyToClipboardAction(private val myCoroutinesDump: List<CoroutineState>, private val myProject: Project) :
        DumbAwareAction("Copy to Clipboard", "Copy whole coroutine dump to clipboard", PlatformIcons.COPY_ICON) {

        override fun actionPerformed(e: AnActionEvent) {
            val buf = StringBuilder()
            buf.append("Full coroutine dump").append("\n\n")
            for (state in myCoroutinesDump) {
                buf.append(state.stringStackTrace).append("\n\n")
            }
            CopyPasteManager.getInstance().setContents(StringSelection(buf.toString()))

            group.createNotification(
                "Full coroutine dump was successfully copied to clipboard",
                MessageType.INFO
            ).notify(myProject)
        }

        private val group = NotificationGroup.toolWindowGroup("Analyze coroutine dump", ToolWindowId.RUN, false)
    }

    private class MyToFileExporter(
        private val myProject: Project,
        private val states: List<CoroutineState>
    ) : ExporterToTextFile {

        override fun getReportText() = buildString {
            for (state in states)
                append(state.stringStackTrace).append("\n\n")
        }

        override fun getDefaultFilePath() = (myProject.basePath ?: "") + File.separator + defaultReportFileName

        override fun canExport() = states.isNotEmpty()

        private val defaultReportFileName = "coroutines_report.txt"
    }
}