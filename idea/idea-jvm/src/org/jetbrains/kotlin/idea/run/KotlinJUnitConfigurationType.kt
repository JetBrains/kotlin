/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import javax.swing.Icon

class KotlinJUnitConfigurationType : ConfigurationType {
    val factory = object : ConfigurationFactory(this) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return KotlinJUnitConfiguration("", project, this)
        }
    }

    companion object {
        val instance: KotlinJUnitConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(KotlinJUnitConfigurationType::class.java)
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)

    override fun getDisplayName() = ExecutionBundle.message("junit.configuration.display.name")
    override fun getConfigurationTypeDescription() = ExecutionBundle.message("junit.configuration.description")
    override fun getIcon(): Icon = AllIcons.RunConfigurations.Junit
    override fun getId() = "KotlinJUnit"
    override fun getTag() = "junit"
    override fun isDumbAware() = false
}

class KotlinJUnitConfiguration(name: String, project: Project, configurationFactory: ConfigurationFactory) :
    JUnitConfiguration(name, project, configurationFactory)