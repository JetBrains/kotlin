package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.popup.PopupFactoryImpl
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.fullText
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.asHtml
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.htmlText
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.icon
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import javax.swing.Icon

class CreateModuleOrTargetPopup private constructor(
    private val target: Module?,
    private val allowMultiplatform: Boolean,
    private val allowAndroid: Boolean,
    private val allowIos: Boolean,
    private val createTarget: (TargetConfigurator) -> Unit,
    private val createModule: (ModuleConfigurator) -> Unit
) {

    private fun TargetConfigurator.needToShow(): Boolean {
        val allTargets = target?.subModules ?: return false
        return canCoexistsWith(allTargets.map { it.configurator as TargetConfigurator })
    }

    private fun DisplayableSettingItem.needToShow(): Boolean = when (this) {
        is TargetConfigurator -> needToShow()
        is TargetConfiguratorGroupWithSubItems -> subItems.any { it.needToShow() }
        else -> false
    }

    private inner class ChooseModuleOrMppModuleStep : BaseListPopupStep<ModuleConfigurator>(
        "Module Type",
        buildList {
            if (allowMultiplatform) +MppModuleConfigurator
            +JvmSinglePlatformModuleConfigurator
            if (allowAndroid) +AndroidSinglePlatformModuleConfigurator

            //todo ios support
            //if (allowIos) +IOSSinglePlatformModuleConfigurator
        }
    ) {
        override fun getIconFor(value: ModuleConfigurator): Icon = value.icon
        override fun getTextFor(value: ModuleConfigurator): String = value.htmlText

        override fun onChosen(selectedValue: ModuleConfigurator?, finalChoice: Boolean): PopupStep<*>? =
            when (selectedValue) {
                null -> PopupStep.FINAL_CHOICE
                else -> {
                    createModule(selectedValue)
                    PopupStep.FINAL_CHOICE
                }
            }
    }

    private inner class ChooseTargetTypeStep(
        targetConfiguratorGroup: TargetConfiguratorGroupWithSubItems,
        showTitle: Boolean
    ) : BaseListPopupStep<DisplayableSettingItem>(
        "Target".takeIf { showTitle },
        targetConfiguratorGroup.subItems.filter { it.needToShow() }
    ) {
        override fun getIconFor(value: DisplayableSettingItem): Icon? = when (value) {
            is DisplayableTargetConfiguratorGroup -> value.moduleType.icon
            is ModuleConfigurator -> value.moduleType.icon
            else -> null
        }

        override fun getTextFor(value: DisplayableSettingItem): String = value.htmlText

        override fun onChosen(selectedValue: DisplayableSettingItem?, finalChoice: Boolean): PopupStep<*>? {
            when {
                finalChoice && selectedValue is TargetConfigurator -> createTarget(selectedValue)
                selectedValue is TargetConfiguratorGroupWithSubItems ->
                    return ChooseTargetTypeStep(selectedValue, showTitle = false)
            }
            return PopupStep.FINAL_CHOICE
        }
    }

    private fun create(): ListPopup? = when (target?.kind) {
        ModuleKind.target -> null
        ModuleKind.multiplatform -> ChooseTargetTypeStep(TargetConfigurationGroups.FIRST, showTitle = true)
        else -> when {
            allowMultiplatform || allowAndroid || allowIos -> ChooseModuleOrMppModuleStep()
            else -> {
                createModule(JvmSinglePlatformModuleConfigurator)
                null
            }
        }
    }?.let { PopupFactoryImpl.getInstance().createListPopup(it) }

    companion object {
        fun create(
            target: Module?,
            allowMultiplatform: Boolean,
            allowAndroid: Boolean,
            allowIos: Boolean,
            createTarget: (TargetConfigurator) -> Unit,
            createModule: (ModuleConfigurator) -> Unit
        ) = CreateModuleOrTargetPopup(
            target = target,
            allowMultiplatform = allowMultiplatform,
            allowAndroid = allowAndroid,
            allowIos = allowIos,
            createTarget = createTarget,
            createModule = createModule
        ).create()
    }
}

