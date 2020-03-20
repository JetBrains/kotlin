package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.ui.components.JBTextField
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.getConfiguratorSettings
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module.Companion.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.isRootModule
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.templates.settings
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.TitledComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.DropDownComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.TextFieldComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.TitledComponentsList
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.createSettingComponent
import javax.swing.JComponent

class ModuleSettingsComponent(
    private val context: Context,
    uiEditorUsagesStats: UiEditorUsageStats
) : DynamicComponent(context) {
    private val settingsList = TitledComponentsList(emptyList(), context).asSubComponent()
    private val moduleDependenciesComponent = ModuleDependenciesComponent(context)

    override val component: JComponent = settingsList.component

    var module: Module? = null
        set(value) {
            field = value
            if (value != null) {
                updateModule(value)
            }
        }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateModule(module: Module) {
        moduleDependenciesComponent.module = module
        val moduleSettingComponents = buildList {
            add(ModuleNameComponent(context, module))
            createTemplatesListComponentForModule(module)?.let(::add)
            addAll(module.getConfiguratorSettings().map { it.createSettingComponent(context) })
            module.template?.let { template ->
                addAll(template.settings(module).map { it.createSettingComponent(context) })
            }
            add(moduleDependenciesComponent)
        }

        settingsList.setComponents(moduleSettingComponents)
    }

    private fun createTemplatesListComponentForModule(module: Module) =
        read { availableTemplatesFor(module) }.takeIf { it.isNotEmpty() }?.let { templates ->
            ModuleTemplateComponent(context, module, templates) {
                updateModule(module)
                component.updateUI()
            }
        }
}

private class ModuleNameComponent(context: Context, private val module: Module) : TitledComponent(context) {
    private val textField = TextFieldComponent(
        context,
        labelText = null,
        initialValue = module.name,
        validator = validateModuleName
    ) { value ->
        module.name = value
        context.write { eventManager.fireListeners(null) }
    }.asSubComponent()

    override val component: JComponent
        get() = textField.component

    override val title: String = "Name"

    override fun onInit() {
        super.onInit()
        val isSingleRootMode = read { KotlinPlugin::modules.settingValue }.size == 1
        if (isSingleRootMode && module.isRootModule) {
            textField.disable("[The same as the project name]")
        }
    }

    companion object {
        private val validateModuleName =
            StringValidators.shouldNotBeBlank("Module name") and
                    StringValidators.shouldBeValidIdentifier("Module Name", ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES)
    }
}

private class ModuleTemplateComponent(
    context: Context,
    module: Module,
    templates: List<Template>,
    onTemplateChanged: () -> Unit
) : TitledComponent(context) {
    @OptIn(ExperimentalStdlibApi::class)
    private val dropDown = DropDownComponent(
        context,
        initialValues = buildList {
            add(NoneTemplate)
            addAll(templates)
        },
        initiallySelectedValue = module.template ?: NoneTemplate,
        labelText = null,
    ) { value ->
        module.template = value.takeIf { it != NoneTemplate }
        onTemplateChanged()
    }.asSubComponent()

    override val component: JComponent
        get() = dropDown.component

    override val title: String = "Template"

    private object NoneTemplate : Template() {
        override val title = "None"
        override val htmlDescription: String = ""
        override val moduleTypes: Set<ModuleType> = ModuleType.ALL
        override val id: String = "none"
    }
}

fun Reader.availableTemplatesFor(module: Module) =
    TemplatesPlugin::templates.propertyValue.values.filter { template ->
        module.configurator.moduleType in template.moduleTypes && template.isApplicableTo(module)
    }


