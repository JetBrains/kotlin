package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import org.jetbrains.kotlin.tools.projectWizard.core.ignore
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.ListModel
import javax.swing.ListSelectionModel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

abstract class AbstractSingleSelectableListWithIcon<V>(initialValues: List<V> = emptyList()) : JBList<V>() {
    protected abstract fun ColoredListCellRenderer<V>.render(value: V)
    protected open fun onSelected(value: V?) {}

    protected val model: DefaultListModel<V>
        get() = super.getModel() as DefaultListModel<V>

    fun updateValues(newValues: List<V>) {
        setModel(createDefaultListModel(newValues))
        if (newValues.isNotEmpty()) {
            selectedIndex = 0
        }
    }

    init {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        updateValues(initialValues)

        selectionModel.addListSelectionListener { e ->
            if (e.valueIsAdjusting) return@addListSelectionListener
            val selectedValue = when (selectedIndex) {
                -1 -> null
                else -> this.model.getElementAt(selectedIndex)
            }
            onSelected(selectedValue)
        }

        cellRenderer = object : ColoredListCellRenderer<V>() {
            override fun customizeCellRenderer(
                list: JList<out V>,
                value: V?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                render(value ?: return)
            }
        }
    }
}

class ImmutableSingleSelectableListWithIcon<V>(
    values: List<V>,
    private val renderValue: ColoredListCellRenderer<V>.(V) -> Unit,
    emptyMessage: String? = null,
    private val onValueSelected: (V?) -> Unit = {}
) : AbstractSingleSelectableListWithIcon<V>(values) {
    override fun ColoredListCellRenderer<V>.render(value: V) = renderValue(value)
    override fun onSelected(value: V?) = onValueSelected(value)

    init {
        emptyMessage?.let { text ->
            emptyText.text = text
        }
    }
}
