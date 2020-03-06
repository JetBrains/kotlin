package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.icons.AllIcons
import icons.GradleIcons
import icons.OpenapiIcons
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.DropDownComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.UIComponentDelegatingSettingComponent


class BuildSystemTypeSettingComponent(
    ideContext: IdeContext
) : UIComponentDelegatingSettingComponent<BuildSystemType, DropDownSettingType<BuildSystemType>>(
    BuildSystemPlugin::type.reference,
    ideContext
) {
    override val uiComponent: DropDownComponent<BuildSystemType> = DropDownComponent(
        ideContext,
        setting.type.values,
        labelText = "Build System",
        filter = { value -> read { setting.type.filter(this, reference, value) } },
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
        BuildSystemType.GradleKotlinDsl -> KotlinIcons.GRADLE_SCRIPT
        BuildSystemType.GradleGroovyDsl -> GradleIcons.Gradle
        BuildSystemType.Maven -> OpenapiIcons.RepositoryLibraryLogo
        BuildSystemType.Jps -> AllIcons.Nodes.Module
    }