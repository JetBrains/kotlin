package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent
import javax.swing.JComponent
import javax.swing.JPanel

class SettingsList(
    settings: List<SettingReference<*, *>>,
    private val valuesReadingContext: ValuesReadingContext
) : DynamicComponent(valuesReadingContext) {
    private val panel = JPanel(VerticalLayout(5))
    private var settingComponents: List<Component> = emptyList()

    init {
        setSettings(settings)
    }

    override val component: JComponent = panel

    override fun onInit() {
        super.onInit()
        settingComponents.forEach { it.onInit() }
    }

    fun setSettings(settings: List<SettingReference<*, *>>) {
        panel.removeAll()
        settingComponents = settings.map { setting ->
            DefaultSettingComponent.create(setting, valuesReadingContext)
        }
        settingComponents.forEach { setting -> setting.onInit(); panel.add(setting.component) }
    }

}