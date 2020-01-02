package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.IOSSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent

class SourcesetDependenciesSettingsComponent(
    valuesReadingContext: ValuesReadingContext
) : DynamicComponent(valuesReadingContext) {
    private val sourcesetDependenciesList = SourcesetDependenciesList()

    private val toolbarDecorator: ToolbarDecorator = ToolbarDecorator.createDecorator(sourcesetDependenciesList).apply {
        setToolbarPosition(ActionToolbarPosition.RIGHT)
        setAddAction {
            val dialog = ChooseModuleDialog(allModulesExcludingCurrent, component)
            if (dialog.showAndGet()) {
                dialog.selectedModule?.let(sourcesetDependenciesList::addDependency)
            }
        }
        setRemoveAction {
            sourcesetDependenciesList.removeSelected()
        }
        setMoveDownAction(null)
        setMoveUpAction(null)
    }

    var sourceset: Sourceset? = null
        set(value) {
            field = value
            sourcesetDependenciesList.sourceset = value
        }


    private val allModulesExcludingCurrent: List<Module>
        get() {
            val currentModule = sourceset?.parent

            return KotlinPlugin::modules.value!!
                .withAllSubModules()
                .filter { module ->
                    if (sourceset in module.sourcesets) return@filter false
                    if (currentModule?.kind == ModuleKind.target && currentModule in module.subModules)
                        return@filter false

                    if (module.configurator == AndroidSinglePlatformModuleConfigurator
                        || module.configurator == IOSSinglePlatformModuleConfigurator
                    ) return@filter false
                    true
                }
        }

    override val component = panel {
        add(toolbarDecorator.createPanelWithPopupHandler(sourcesetDependenciesList), BorderLayout.CENTER)
    }
}

private class ChooseModuleDialog(modules: List<Module>, parent: JComponent) : DialogWrapper(parent, false) {
    private val list = ImmutableSingleSelectableListWithIcon(
        values = modules,
        emptyMessage = "There are no dependencies to add",
        renderValue = { value ->
            renderModule(value)
        }
    ).bordered(needTopEmptyBorder = false, needBottomEmptyBorder = false)

    override fun createCenterPanel() = panel {
        preferredSize = Dimension(preferredSize.width, 300)
        add(
            label("Please select the modules to add as dependencies:")
                .bordered(
                    needLineBorder = false,
                    needInnerEmptyBorder = false,
                    needTopEmptyBorder = false
                ),
            BorderLayout.NORTH
        )
        add(list, BorderLayout.CENTER)
    }

    val selectedModule: Module?
        get() = list.selectedValue

    init {
        title = "Choose Modules"
        init()
    }
}

private class SourcesetDependenciesList : AbstractSingleSelectableListWithIcon<Module>() {
    override fun ColoredListCellRenderer<Module>.render(value: Module) {
        renderModule(value)
    }

    var sourceset: Sourceset? = null
        set(value) {
            field = value
            updateValues(
                value?.dependencies?.mapNotNull { it.safeAs<ModuleBasedSourcesetDependency>()?.module } ?: return
            )
            updateUI()
        }


    fun addDependency(dependency: Module) {
        model.addElement(dependency)
        sourceset?.let { it.dependencies += ModuleBasedSourcesetDependency(dependency) }
    }

    fun removeSelected() {
        val index = selectedIndex
        model.removeElementAt(selectedIndex)
        sourceset?.let { it.dependencies = it.dependencies.toMutableList().also { it.removeAt(index) } }
        if (model.size() > 0) {
            selectedIndex = index.coerceAtMost(model.size - 1)
        }
    }
}

private fun ColoredListCellRenderer<Module>.renderModule(module: Module) {
    append(module.path.asString())
    append(" ")
    append("(${module.greyText})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    icon = module.icon
}