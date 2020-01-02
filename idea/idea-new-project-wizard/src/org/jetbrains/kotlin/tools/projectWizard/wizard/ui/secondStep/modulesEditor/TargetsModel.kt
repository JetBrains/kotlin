package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import kotlin.reflect.KMutableProperty0

class TargetsModel(
    private val tree: ModulesEditorTree,
    private val value: KMutableProperty0<List<Module>?>
) {
    private val root get() = tree.model.root as DefaultMutableTreeNode

    private fun addToTheTree(module: Module, modifyValue: Boolean, parent: DefaultMutableTreeNode = root) {
        val pathsToExpand = mutableListOf<TreePath>()

        fun add(moduleToAdd: Module, parentToAdd: DefaultMutableTreeNode) {
            DefaultMutableTreeNode(moduleToAdd).apply {
                val userObject = parentToAdd.userObject
                when {
                    userObject is Module && userObject.kind == ModuleKind.singleplatform -> {
                        val indexOfLastModule = parent.children()
                            .toList()
                            .indexOfLast {
                                it.safeAs<DefaultMutableTreeNode>()?.userObject is Module
                            }
                        if (indexOfLastModule == -1) parent.insert(this, 0)
                        else parent.insert(this, indexOfLastModule)
                    }
                    else -> parentToAdd.add(this)
                }
                pathsToExpand += TreePath(path)

                moduleToAdd.subModules.forEach { subModule ->
                    add(subModule, this)
                }
                moduleToAdd.sourcesets.forEach { sourceset ->
                    val sourcesetNode = DefaultMutableTreeNode(sourceset)
                    add(sourcesetNode)
                    pathsToExpand += TreePath(sourcesetNode.path)
                }
            }
        }

        add(module, parent)

        if (modifyValue) {
            when (val parentModule = parent.userObject) {
                ModulesEditorTree.PROJECT_USER_OBJECT -> value.set(value.get().orEmpty() + module)
                is Module -> parentModule.subModules += module
            }
        }
        tree.reload()
        pathsToExpand.forEach(tree::expandPath)
    }

    fun add(module: Module) {
        addToTheTree(module, modifyValue = true, parent = tree.selectedNode ?: root)
    }

    fun update() {
        root.removeAllChildren()
        value.get()?.forEach { addToTheTree(it, modifyValue = false) }
        if (value.get()?.isEmpty() == true) {
            tree.reload()
        }
    }

    fun removeSelected() {
        val selectedNode = tree.selectedNode?.takeIf { it.userObject is Module } ?: return
        when (val parent = selectedNode.parent.safeAs<DefaultMutableTreeNode>()?.userObject) {
            ModulesEditorTree.PROJECT_USER_OBJECT -> {
                val index = selectedNode.parent.getIndex(selectedNode)
                selectedNode.removeFromParent()
                value.set(value.get()?.toMutableList().also { it?.removeAt(index) })
            }
            is Module -> {
                parent.subModules = parent.subModules.filterNot { it === selectedNode.userObject }
                selectedNode.removeFromParent()
            }
        }
        tree.reload()
    }
}