package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.templates.settings
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ErrorAwareComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingsList
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel

class TemplatesComponent(
    ideContext: IdeContext,
    uiEditorUsagesStats: UiEditorUsageStats
) : DynamicComponent(ideContext), ErrorAwareComponent {
    private val chooseTemplateComponent: ChooseTemplateComponent =
        ChooseTemplateComponent(ideContext) { template ->
            uiEditorUsagesStats.moduleTemplatesSet++
            module?.template = template
            switchState(template)
        }.asSubComponent()

    private val templateSettingsComponent = TemplateSettingsComponent(ideContext) {
        if (MessagesEx.showOkCancelDialog(
                component,
                KotlinNewProjectWizardBundle.message("editor.remove.template.description"),
                KotlinNewProjectWizardBundle.message("editor.remove.template.title"),
                KotlinNewProjectWizardBundle.message("editor.remove.button.remove"),
                KotlinNewProjectWizardBundle.message("editor.remove.button.cancel"),
                null
            ) == Messages.OK
        ) {
            uiEditorUsagesStats.moduleTemplatesRemoved++
            module?.template = null
            switchState(null)
        }
    }.asSubComponent()

    override fun findComponentWithError(error: ValidationResult.ValidationError): SettingComponent<*, *>? =
        templateSettingsComponent.findComponentWithError(error)

    private fun switchState(selectedTemplate: Template?) {
        panel.removeAll()
        when (selectedTemplate) {
            null -> panel.add(chooseTemplateComponent.component, BorderLayout.CENTER)
            else -> {
                if (module != null) {
                    templateSettingsComponent.setTemplate(module!!, selectedTemplate)
                }
                panel.add(templateSettingsComponent.component, BorderLayout.CENTER)
            }
        }
        panel.updateUI()
    }

    val panel = panel {
        add(chooseTemplateComponent.component, BorderLayout.CENTER)
    }

    override val component: JComponent = panel

    var module: Module? = null
        set(value) {
            field = value
            switchState(value?.template)
            chooseTemplateComponent.selectedModule = value
        }
}

class ChooseTemplateComponent(
    ideContext: IdeContext,
    private val onTemplateChosen: (Template) -> Unit
) : DynamicComponent(ideContext) {
    private enum class State(val text: String) {
        MODULE_SELECTED_AND_TEMPLATES_AVAILABLE(KotlinNewProjectWizardBundle.message("editor.template.can.configure.template")),
        MODULE_SELECTED_AND_NO_TEMPLATES_AVAILABLE(KotlinNewProjectWizardBundle.message("editor.template.no.templates.available")),
        NO_MODULE_SELECTED(KotlinNewProjectWizardBundle.message("editor.template.select.module"))
    }

    override val component: JPanel = object : JPanel() {
        override fun paint(g: Graphics?) {
            super.paint(g)
            statusText.paint(this, g)
        }
    }

    private val statusText = object : StatusText(component) {
        override fun isStatusVisible() = true
    }

    private val allTemplates
        get() = read {
            TemplatesPlugin::templates.propertyValue
        }

    var selectedModule: Module? = null
        set(value) {
            field = value
            onStateUpdated()
        }

    init {
        onStateUpdated()
    }

    private val availableTemplates
        get() = allTemplates.values.filter { template ->
            selectedModule!!.configurator.moduleType in template.moduleTypes
                    && template.isApplicableTo(selectedModule!!)
        }

    private val state: State
        get() = when {
            selectedModule == null -> State.NO_MODULE_SELECTED
            availableTemplates.isEmpty() -> State.MODULE_SELECTED_AND_NO_TEMPLATES_AVAILABLE
            else -> State.MODULE_SELECTED_AND_TEMPLATES_AVAILABLE
        }

    private fun onStateUpdated() {
        statusText.clear()
        statusText.appendText(state.text)
        if (state == State.MODULE_SELECTED_AND_TEMPLATES_AVAILABLE) {
            statusText.appendSecondaryText(KotlinNewProjectWizardBundle.message("editor.configure"), SimpleTextAttributes.LINK_ATTRIBUTES) {
                showDialog()
            }
        }
    }


    private fun showDialog() {
        val availableTemplates = availableTemplates
        if (availableTemplates.isEmpty()) {
            MessagesEx.error(null, KotlinNewProjectWizardBundle.message("editor.template.no.templates.available"))
        } else {
            val dialog = TemplateListDialog(availableTemplates, component)
            if (dialog.showAndGet()) {
                onTemplateChosen(dialog.selectedTemplate ?: return)
            }
        }
    }
}

private class TemplateListDialog(
    values: List<Template>,
    parent: JComponent
) : DialogWrapper(parent, false) {
    private val templateDescriptionComponent = TemplateDescriptionComponent(
        needRemoveButton = false
    ).apply {
        component.bordered(needTopEmptyBorder = false, needBottomEmptyBorder = false)
    }
    private val templatesList = ImmutableSingleSelectableListWithIcon(
        values = values,
        renderValue = { value ->
            icon = when {
                value.moduleTypes.size == 1 -> value.moduleTypes.single().icon
                else -> KotlinIcons.MPP
            }
            append(value.title)
        },
        onValueSelected = { templateDescriptor ->
            templateDescriptionComponent.updateSelectedTemplate(templateDescriptor)
        }
    ).bordered(needTopEmptyBorder = false, needBottomEmptyBorder = false)

    val selectedTemplate: Template?
        get() = templatesList.selectedValue

    init {
        title = KotlinNewProjectWizardBundle.message("editor.template.choose")
        init()
        if (values.isNotEmpty()) {
            templateDescriptionComponent.updateSelectedTemplate(values.first())
        }
    }

    override fun createCenterPanel() = panel(BorderLayout()) {
        preferredSize = Dimension(700, 300)
        val splitter = JBSplitter(false, .3f).apply {
            firstComponent = templatesList
            secondComponent = templateDescriptionComponent.component
            orientation = false
        }
        add(splitter, BorderLayout.CENTER)
    }
}

class TemplateDescriptionComponent(
    needRemoveButton: Boolean = false,
    private val nonDefaultBackgroundColor: Color? = null,
    onRemoveButtonClicked: () -> Unit = {}
) : Component() {
    private val removeButton = if (needRemoveButton) {
        hyperlinkLabel(KotlinNewProjectWizardBundle.message("editor.template.remove"), onClick = onRemoveButtonClicked)
    } else null

    private val titleLabel = label("", bold = true) {
        font = UIUtil.getListFont().let { font ->
            Font(font.family, Font.BOLD, (font.size * 1.3).toInt())
        }
    }

    private val title = panel {
        nonDefaultBackgroundColor?.let { background = it }
        add(titleLabel, BorderLayout.CENTER)
        removeButton?.let { add(it, BorderLayout.EAST) }
    }

    private val descriptionLabel = DescriptionPanel()

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            nonDefaultBackgroundColor?.let { background = it }
            add(title, BorderLayout.NORTH)
            add(descriptionLabel, BorderLayout.CENTER)
        }
    }

    private fun setComponentsVisibility(isVisible: Boolean) {
        titleLabel.isVisible = isVisible
        descriptionLabel.isVisible = isVisible
    }

    fun updateSelectedTemplate(template: Template?) {
        setComponentsVisibility(template != null)
        if (template != null) {
            titleLabel.text = template.title
            descriptionLabel.updateText(template.htmlDescription)
        }
    }
}

private class TemplateSettingsComponent(
    ideContext: IdeContext,
    removeTemplate: () -> Unit
) : DynamicComponent(ideContext), ErrorAwareComponent {
    private val templateDescriptionComponent = TemplateDescriptionComponent(
        needRemoveButton = true,
        nonDefaultBackgroundColor = UIUtil.getEditorPaneBackground(),
        onRemoveButtonClicked = removeTemplate
    ).apply {
        component.bordered(needTopEmptyBorder = false, needBottomEmptyBorder = false)
    }

    private val settings = SettingsList(emptyList(), ideContext).apply {
        component.bordered()
    }

    override fun findComponentWithError(error: ValidationResult.ValidationError): SettingComponent<*, *>? =
        settings.findComponentWithError(error)


    fun setTemplate(module: Module, selectedTemplate: Template) {
        settings.setSettings(selectedTemplate.settings(module))
        templateDescriptionComponent.updateSelectedTemplate(selectedTemplate)
    }

    override val component: JComponent = splitterFor(
        templateDescriptionComponent.component,
        settings.component,
        vertical = true
    ).apply {
        (this as JBSplitter).proportion = .7f
    }
}

