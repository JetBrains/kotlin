package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.ui.layout.panel
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.isSpecificError
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.FocusableComponent

interface ErrorAwareComponent {
    fun findComponentWithError(error: ValidationResult.ValidationError): FocusableComponent?
}

class SettingsList(
    private var settingComponents: List<SettingComponent<*, *>>,
    private val context: Context
) : DynamicComponent(context), ErrorAwareComponent {
    private val ui = BorderLayoutPanel()

    init {
        setSettingComponents(settingComponents)
    }

    override val component get() = ui

    override fun onInit() {
        super.onInit()
        settingComponents.forEach { it.onInit() }
    }

    private fun setSettingComponents(settingComponents: List<SettingComponent<*, *>>) {
        this.settingComponents = settingComponents
        ui.removeAll()
        ui.addToCenter(panel {
            settingComponents.forEach { settingComponent ->
                settingComponent.onInit()
                row(settingComponent.setting.title + ":") {
                    settingComponent.component(growX)
                }
            }
        })
    }


    fun setSettings(settings: List<SettingReference<*, *>>) {
        setSettingComponents(
            settings.map { setting ->
                DefaultSettingComponent.create(setting, context, needLabel = false)
            }
        )
    }

    override fun findComponentWithError(error: ValidationResult.ValidationError) = read {
        settingComponents.firstOrNull { component ->
            val value = component.value ?: return@firstOrNull false
            val result = component.setting.validator.validate(this, value)
            result isSpecificError error
        }
    }
}