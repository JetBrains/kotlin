/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import org.jetbrains.konan.MobileBundle

class MobileAppRunConfigurationType : ConfigurationTypeBase(
    "KonanMobile",
    MobileBundle.message("run.configuration.name"),
    MobileBundle.message("run.configuration.description"),
    AllIcons.RunConfigurations.Application
) {
    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration =
                MobileAppRunConfiguration(project, this, name)
        })
    }

    val factory: ConfigurationFactory get() = configurationFactories[0]

    companion object {
        val instance: MobileAppRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(MobileAppRunConfigurationType::class.java)
    }
}