package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.icon
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class ModulesEditorTree(
    private val onSelected: (DisplayableSettingItem?) -> Unit,
    private val addModule: (JComponent) -> Unit
) : Tree(DefaultMutableTreeNode(PROJECT_USER_OBJECT)) {
    val model: DefaultTreeModel get() = super.getModel() as DefaultTreeModel

    @Suppress("ClassName")
    object PROJECT_USER_OBJECT

    fun reload() {
        val openPaths = (0 until rowCount).mapNotNull { row ->
            getPathForRow(row).takeIf { isExpanded(it) }
        }
        model.reload()
        openPaths.forEach(::expandPath)
    }

    val selectedSettingItem
        get() = selectedNode?.userObject?.safeAs<DisplayableSettingItem>()

    val selectedNode
        get() = getSelectedNodes(DefaultMutableTreeNode::class.java) {
            it.userObject is DisplayableSettingItem
        }.singleOrNull()


    init {
        getSelectionModel().selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        setShowsRootHandles(true)

        addTreeSelectionListener {
            onSelected(selectedSettingItem)
        }

        emptyText.clear()
        emptyText.appendText("No modules created")
        emptyText.appendSecondaryText(
            "Add a module to the project",
            SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
        ) {
            addModule(this)
        }

        setCellRenderer(object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                if (value?.safeAs<DefaultMutableTreeNode>()?.userObject == PROJECT_USER_OBJECT) {
                    icon = AllIcons.Nodes.Project
                    append("Project")
                    return
                }
                val setting = (value as? DefaultMutableTreeNode)?.userObject as? DisplayableSettingItem
                    ?: return
                icon = when (setting) {
                    is Module -> setting.icon
                    is Sourceset -> AllIcons.Nodes.Module
                    else -> null
                }
                append(setting.text)
                setting.greyText?.let { greyText ->
                    append(" ")
                    append("($greyText)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        })
    }
}