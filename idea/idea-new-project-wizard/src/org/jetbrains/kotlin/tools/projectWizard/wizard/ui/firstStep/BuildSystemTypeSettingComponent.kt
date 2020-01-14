package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.icons.AllIcons
import icons.GradleIcons
import icons.OpenapiIcons
import org.jetbrains.kotlin.tools.projectWizard.core.entity.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.DropDownComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.valueForSetting
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.UIComponentDelegatingSettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout


class BuildSystemTypeSettingComponent(
    private val valuesReadingContext: ValuesReadingContext
) : UIComponentDelegatingSettingComponent<BuildSystemType, DropDownSettingType<BuildSystemType>>(
    BuildSystemPlugin::type.reference,
    valuesReadingContext
) {
    override val uiComponent: DropDownComponent<BuildSystemType> = DropDownComponent(
        valuesReadingContext,
        setting.type.values,
        labelText = "Build System",
        filter = { value -> setting.type.filter(valuesReadingContext, reference, value) },
        validator = setting.validator,
        iconProvider = BuildSystemType::icon,
        onValueUpdate = { value = it }
    ).asSubComponent()


    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == KotlinPlugin::projectKind.reference) {
            uiComponent.component.updateUI()
        }
        value?.let(uiComponent::validate)
    }
}

@Suppress("DEPRECATION")
private val BuildSystemType.icon
    get() = when (this) {
        BuildSystemType.GradleKotlinDsl -> GradleIcons.Gradle
        BuildSystemType.GradleGroovyDsl -> GradleIcons.Gradle
        BuildSystemType.Maven -> OpenapiIcons.RepositoryLibraryLogo
        BuildSystemType.Jps -> AllIcons.Nodes.Module
    }