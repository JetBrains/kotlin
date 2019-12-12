package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.IOSSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.MppModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.createPanelWithPopupHandler
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import javax.swing.JComponent

class ModulesEditorToolbarDecorator(
    private val tree: ModulesEditorTree,
    private val moduleCreator: NewModuleCreator,
    private val model: TargetsModel,
    private val getModules: () -> List<Module>,
    private val isMultiplatformProject: () -> Boolean
) {
    private val toolbarDecorator: ToolbarDecorator = ToolbarDecorator.createDecorator(tree).apply {
        setToolbarPosition(ActionToolbarPosition.TOP)
        setAddAction { button ->
            val allModules = getModules().withAllSubModules(includeSourcesets = false)
            val target = tree.selectedSettingItem?.safeAs<Module>()
            val isRootModule = target == null
            val popup = moduleCreator.create(
                target = tree.selectedSettingItem?.safeAs(),
                allowMultiplatform = isRootModule
                        && allModules.none { it.configurator == MppModuleConfigurator },
                allowAndroid = isRootModule
                        && isMultiplatformProject()
                        && allModules.none { it.configurator == AndroidSinglePlatformModuleConfigurator },
                allowIos = isRootModule
                        && isMultiplatformProject()
                        && allModules.none { it.configurator == IOSSinglePlatformModuleConfigurator },
                allModules = allModules,
                createModule = model::add
            )
            popup?.show(button.preferredPopupPoint!!)
        }
        setAddActionUpdater { event ->
            event.presentation.apply {
                isEnabled = when (val selected = tree.selectedSettingItem) {
                    is Module -> selected.configurator.canContainSubModules
                    is Sourceset -> false
                    null -> true
                    else -> false
                }
                text = "Add" + when (tree.selectedSettingItem?.safeAs<Module>()?.kind) {
                    ModuleKind.multiplatform -> " Target"
                    ModuleKind.singleplatform -> " Module"
                    ModuleKind.target -> ""
                    null -> ""
                }
            }
            event.presentation.isEnabled
        }
        setRemoveAction {
            val moduleKindText = selectedModuleKindText ?: "Module"
            if (Messages.showOkCancelDialog(
                    tree,
                    buildString {
                        appendln("Do you want to remove selected $moduleKindText?")
                        if (tree.selectedSettingItem.safeAs<Module>()?.kind != ModuleKind.target) {
                            appendln("This will also remove all submodules.")
                        }
                        appendln()
                        appendln("This action cannot be undone.")
                    },
                    "Remove selected $moduleKindText?",
                    "Remove",
                    "Cancel",
                    AllIcons.General.QuestionDialog
                ) == Messages.OK
            ) {
                model.removeSelected()
            }
        }
        setRemoveActionUpdater { event ->
            event.presentation.apply {
                isEnabled = tree.selectedSettingItem is Module
                text = "Remove" + selectedModuleKindText?.let { " $it" }.orEmpty()
            }
            event.presentation.isEnabled
        }

        setMoveDownAction(null)
        setMoveUpAction(null)
    }

    private val selectedModuleKindText
        get() = tree.selectedSettingItem.safeAs<Module>()?.kindText

    fun createToolPanel(): JComponent = toolbarDecorator
        .createPanelWithPopupHandler(tree)
        .apply {
            border = null
        }
}

private val Module.kindText
    get() = when (kind) {
        ModuleKind.multiplatform -> "Module"
        ModuleKind.singleplatform -> "Module"
        ModuleKind.target -> "Target"
    }