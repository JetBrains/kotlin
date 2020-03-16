package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.ui.layout.panel
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.FocusableComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.TitledComponent

class TitledComponentsList(
    private var components: List<TitledComponent>,
    private val context: Context
) : DynamicComponent(context) {
    private val ui = BorderLayoutPanel()

    init {
        ui.addToCenter(createComponentsPanel(components))
    }

    override val component get() = ui

    override fun navigateTo(error: ValidationResult.ValidationError) {
        components.forEach { it.navigateTo(error) }
    }

    override fun onInit() {
        super.onInit()
        components.forEach { it.onInit() }
    }

    fun setComponents(newComponents: List<TitledComponent>) {
        this.components = newComponents
        ui.removeAll()
        newComponents.forEach(TitledComponent::onInit)
        ui.addToCenter(createComponentsPanel(newComponents))
    }


    fun setSettings(settings: List<SettingReference<*, *>>) {
        setComponents(
            settings.map { setting ->
                DefaultSettingComponent.create(setting, context, needLabel = false)
            }
        )
    }

    companion object {
        private fun createComponentsPanel(components: List<TitledComponent>) = panel {
            components.forEach { component ->
                row(component.title?.let { "$it:" }.orEmpty()) {
                    component.component(growX)
                }
            }
        }
    }
}