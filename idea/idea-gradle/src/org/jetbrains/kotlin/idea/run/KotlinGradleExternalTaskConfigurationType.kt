/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

class KotlinGradleExternalTaskConfigurationType : AbstractExternalSystemTaskConfigurationType(GradleConstants.SYSTEM_ID) {
    companion object {
        val instance: KotlinGradleExternalTaskConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(KotlinGradleExternalTaskConfigurationType::class.java)
    }

    override fun getId() = "Kotlin" + externalSystemId.readableName + "RunConfiguration"

    @Suppress("SpellCheckingInspection")
    override fun getHelpTopic() = "reference.dialogs.rundebug.GradleRunConfiguration"

    override fun doCreateConfiguration(
        externalSystemId: ProjectSystemId,
        project: Project,
        factory: ConfigurationFactory,
        name: String
    ): ExternalSystemRunConfiguration {
        return KotlinGradleRunConfiguration(project, factory, name)
    }
}

class KotlinGradleRunConfiguration(project: Project?, factory: ConfigurationFactory?, name: String?) :
    GradleRunConfiguration(project, factory, name)