package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.ui.components.JBTabbedPane
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent

class SourcesetSettingsComponent(valuesReadingContext: ValuesReadingContext) : DynamicComponent(valuesReadingContext) {
    private val dependenciesComponent = SourcesetDependenciesSettingsComponent(valuesReadingContext).asSubComponent()

    override val component = JBTabbedPane().apply {
        add("Dependencies", dependenciesComponent.component)
    }

    var sourceset: Sourceset? = null
        set(value) {
            field = value
            dependenciesComponent.sourceset = value
        }
}