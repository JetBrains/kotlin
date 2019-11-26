/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.RunManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.konan.MobileBundle
import org.jetbrains.konan.isAndroid
import org.jetbrains.konan.isApple

fun MobileRunConfiguration.Companion.createDefaults(project: Project, modules: List<Module>) {
    val runManager = RunManager.getInstance(project)
    for (module in modules) {
        val type = MobileAppRunConfigurationType.instance
        val factory = type.factory
        val configuration = factory.createTemplateConfiguration(project, runManager) as MobileRunConfiguration
        configuration.module = module
        val name = when {
            module.isApple -> MobileBundle.message("run.configuration.name.apple")
            module.isAndroid -> MobileBundle.message("run.configuration.name.android")
            else -> throw IllegalStateException()
        }
        configuration.name = if (modules.size > 1) "$name [${module.name}]" else name

        if (runManager.findConfigurationByTypeAndName(type, configuration.name) == null) {
            runManager.addConfiguration(runManager.createConfiguration(configuration, factory))
        }
    }
}