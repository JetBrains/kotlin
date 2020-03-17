package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardBundle
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
                allowMultiplatform = isMultiplatformProject()
                        && isRootModule
                        && allModules.none { it.configurator == MppModuleConfigurator },
                allowSinglepaltformJs = isMultiplatformProject()
                        && isRootModule
                        && allModules.none { it.configurator == JsSingleplatformModuleConfigurator },
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
                text = when (tree.selectedSettingItem?.safeAs<Module>()?.kind) {
                    ModuleKind.multiplatform -> KotlinNewProjectWizardBundle.message("editor.add.target")
                    ModuleKind.singleplatformJvm -> KotlinNewProjectWizardBundle.message("editor.add.generic.module")
                    ModuleKind.singleplatformJs -> KotlinNewProjectWizardBundle.message("editor.add.javascript.module")
                    ModuleKind.target -> KotlinNewProjectWizardBundle.message("editor.add")
                    null -> KotlinNewProjectWizardBundle.message("editor.add")
                }
            }
            event.presentation.isEnabled
        }
        setRemoveAction {
            val title = when (selectedModuleKind) {
                ModuleKind.target -> KotlinNewProjectWizardBundle.message("editor.remove.target.title")
                else -> KotlinNewProjectWizardBundle.message("editor.remove.module.title")
            }

            val description = when (selectedModuleKind) {
                ModuleKind.target -> KotlinNewProjectWizardBundle.message("editor.remove.target.description")
                else -> KotlinNewProjectWizardBundle.message("editor.remove.module.description")
            }

            if (Messages.showOkCancelDialog(
                    tree,
                    buildString {
                        appendln(description)
                        if (tree.selectedSettingItem.safeAs<Module>()?.kind != ModuleKind.target) {
                            appendln(KotlinNewProjectWizardBundle.message("editor.remove.all.submodules"))
                        }
                        appendln()
                        appendln(KotlinNewProjectWizardBundle.message("editor.cant.undone.action"))
                    },
                    title,
                    KotlinNewProjectWizardBundle.message("editor.remove.button.remove"),
                    KotlinNewProjectWizardBundle.message("editor.remove.button.cancel"),
                    AllIcons.General.QuestionDialog
                ) == Messages.OK
            ) {
                model.removeSelected()
            }
        }
        setRemoveActionUpdater { event ->
            event.presentation.apply {
                isEnabled = tree.selectedSettingItem is Module
                text = when (selectedModuleKind) {
                    ModuleKind.target -> KotlinNewProjectWizardBundle.message("editor.remove.target.toolbutton")
                    else -> KotlinNewProjectWizardBundle.message("editor.remove.module.toolbutton")
                }
            }
            event.presentation.isEnabled
        }

        setMoveDownAction(null)
        setMoveUpAction(null)
    }

    private val selectedModuleKind
        get() = tree.selectedSettingItem.safeAs<Module>()?.kind

    fun createToolPanel(): JComponent = toolbarDecorator
        .createPanelWithPopupHandler(tree)
        .apply {
            border = null
        }
}