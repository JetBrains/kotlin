package com.jetbrains.mobile.execution

import com.intellij.execution.RunManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.mobile.MobileBundle

fun MobileRunConfigurationBase.Companion.createDefaults(project: Project, modules: List<Module>, device: Device) {
    if (modules.isEmpty()) return
    if (!SystemInfo.isMac && device is AppleDevice) return

    val runManager = RunManager.getInstance(project)
    val type = MobileAppRunConfigurationType.instance
    val factory = type.factory
    for (module in modules) {
        val configuration = factory.createTemplateConfiguration(project, runManager) as MobileRunConfigurationBase
        configuration.module = module
        configuration.executionTargetNames = listOf(device.displayName)
        val name = when (device) {
            is AppleDevice -> MobileBundle.message("run.configuration.name.apple")
            is AndroidDevice -> MobileBundle.message("run.configuration.name.android")
            else -> throw IllegalStateException()
        }
        configuration.name = if (modules.size > 1) "$name [${module.name}]" else name

        if (runManager.findConfigurationByTypeAndName(type, configuration.name) == null) {
            runManager.addConfiguration(runManager.createConfiguration(configuration, factory))
        }
    }
    if (runManager.selectedConfiguration == null) {
        runManager.getConfigurationSettingsList(type).firstOrNull()?.let {
            runManager.selectedConfiguration = it
        }
    }
}