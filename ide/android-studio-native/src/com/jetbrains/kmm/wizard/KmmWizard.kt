/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.computeM
import org.jetbrains.kotlin.tools.projectWizard.core.entity.PipelineTask
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.AndroidPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.MultiplatformMobileApplicationProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.Wizard
import java.nio.file.Path

class ProjectDescription(
    suggestedProjectName: String?,
    val projectDir: Path,
    val androidModuleName: String,
    private val packageName: String,
    val androidSdkPath: Path?
) {
    val projectName = suggestedProjectName ?: "Application"

    val artifactId: String
        get() {
            return projectName
        }

    val groupId: String
        get() {
            val id = packageName.removeSuffix("." + projectName.toLowerCase())
            return if (id.isEmpty()) "me.user" else id
        }
}

class KmmWizard(
    private val description: ProjectDescription,
    isUnitTestMode: Boolean = false
) : Wizard(
    WizardConfiguration.pluginsCreator,
    WizardConfiguration.servicesManager,
    isUnitTestMode
) {
    override fun apply(
        services: List<WizardService>,
        phases: Set<GenerationPhase>,
        onTaskExecuting: (PipelineTask) -> Unit
    ): TaskResult<Unit> = computeM {
        context.writeSettings {
            StructurePlugin::groupId.reference.setValue(description.groupId)
            StructurePlugin::artifactId.reference.setValue(description.artifactId)
            StructurePlugin::name.reference.setValue(description.projectName)
            StructurePlugin::projectPath.reference.setValue(description.projectDir)

            if (description.androidSdkPath != null) {
                AndroidPlugin::androidSdkPath.reference.setValue(description.androidSdkPath)
            }

            BuildSystemPlugin::type.reference.setValue(BuildSystemType.GradleKotlinDsl)

            applyProjectTemplate(MultiplatformMobileApplicationProjectTemplate)

            KotlinPlugin::modules.reference.settingValue.forEach { module ->
                if (module.configurator == AndroidSinglePlatformModuleConfigurator) {
                    module.name = description.androidModuleName
                }
            }
        }

        super.apply(services, phases,onTaskExecuting)
    }
}