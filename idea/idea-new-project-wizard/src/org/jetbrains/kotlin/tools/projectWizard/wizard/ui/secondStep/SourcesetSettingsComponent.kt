package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep

import com.intellij.ui.components.JBTabbedPane
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.DynamicComponent

class SourcesetSettingsComponent(ideContext: IdeContext) : DynamicComponent(ideContext) {
    private val dependenciesComponent = SourcesetDependenciesSettingsComponent(ideContext).asSubComponent()

    override val component = JBTabbedPane().apply {
        add(KotlinNewProjectWizardBundle.message("editor.dependencies"), dependenciesComponent.component)
    }

    var sourceset: Sourceset? = null
        set(value) {
            field = value
            dependenciesComponent.sourceset = value
        }
}