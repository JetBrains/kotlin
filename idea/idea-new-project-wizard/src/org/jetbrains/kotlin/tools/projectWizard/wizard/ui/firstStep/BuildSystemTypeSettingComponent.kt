package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.icons.AllIcons
import icons.GradleIcons
import icons.MavenIcons
import org.jetbrains.kotlin.tools.projectWizard.core.entity.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.DropDownComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout


class BuildSystemTypeSettingComponent(
    valuesReadingContext: ValuesReadingContext
) : SettingComponent<BuildSystemType, DropDownSettingType<BuildSystemType>>(
    BuildSystemPlugin::type.reference,
    valuesReadingContext
) {
    override val validationIndicator: ValidationIndicator? = null
    private val listComponent = DropDownComponent(
        valuesReadingContext,
        setting.type.values,
        labelText = "Build System",
        validator = setting.validator,
        iconProvider = BuildSystemType::icon,
        onAnyValueUpdate = { value = it }
    ).asSubComponent()

    override val component = panel {
        add(listComponent.component, BorderLayout.CENTER)
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == KotlinPlugin::projectKind.reference) {
            listComponent.component.updateUI()
        }
        listComponent.validate()
    }
}

@Suppress("DEPRECATION")
private val BuildSystemType.icon
    get() = when (this) {
        BuildSystemType.GradleKotlinDsl -> GradleIcons.Gradle
        BuildSystemType.GradleGroovyDsl -> GradleIcons.Gradle
        BuildSystemType.Maven -> MavenIcons.MavenLogo
        BuildSystemType.Jps -> AllIcons.Nodes.Module
    }