/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.AndroidPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.RunConfigurationsPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.KotlinDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.MultiplatformMobileApplicationProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.Wizard
import java.nio.file.Path

val KMM_SERVICES: List<WizardService> = listOf(
    KmmBuildSystemAvailabilityWizardService(),
    KmmFileSystemWizardService(),
    KmmKotlinVersionProviderService(),
    SettingSavingWizardServiceImpl(),
    VelocityTemplateEngineServiceImpl(),
    DummyFileFormattingService(),
    RunConfigurationsServiceImpl(),
    ProjectImportingWizardServiceImpl()
)

val KMM_PLUGINS_CREATOR = { context: Context ->
    listOf(
        StructurePlugin(context),

        KotlinDslPlugin(context),

        KotlinPlugin(context),
        TemplatesPlugin(context),
        ProjectTemplatesPlugin(context),
        RunConfigurationsPlugin(context),
        AndroidPlugin(context),
    )
}

class KmmWizard : Wizard(
    KMM_PLUGINS_CREATOR,
    ServicesManager(KMM_SERVICES) { services -> services.firstOrNull() },
    false
) {
    private val DEFAULT_GROUP_ID = "me.user"
    private val DEFAULT_APPLICATION_NAME = "Application"
    private val DEFAULT_ANDROID_SDK_PATH = "Please specify your Android SDK path"

    private fun suggestGroupId(projectName: String?, packageName: String): String {
        val id = packageName.removeSuffix("." + projectName?.toLowerCase())
        return if (id.isEmpty()) DEFAULT_GROUP_ID else id
    }

    fun generate(
        projectName: String?,
        projectDir: Path,
        androidModuleName: String,
        packageName: String,
        androidSdkPath: Path?
    ): TaskResult<Unit> {
        context.writeSettings {
            StructurePlugin::groupId.reference.setValue(suggestGroupId(projectName, packageName))
            StructurePlugin::artifactId.reference.setValue(projectName ?: DEFAULT_APPLICATION_NAME)
            StructurePlugin::name.reference.setValue(projectName ?: DEFAULT_APPLICATION_NAME)
            StructurePlugin::projectPath.reference.setValue(projectDir)
            AndroidPlugin::androidSdkPath.reference.setValue(androidSdkPath ?: DEFAULT_ANDROID_SDK_PATH)
            BuildSystemPlugin::type.reference.setValue(BuildSystemType.GradleKotlinDsl)

            applyProjectTemplate(MultiplatformMobileApplicationProjectTemplate)

            KotlinPlugin::modules.reference.settingValue.forEach { module ->
                if (module.configurator == AndroidSinglePlatformModuleConfigurator) {
                    module.name = androidModuleName
                }
            }
        }

        return apply(
            services = org.jetbrains.kotlin.tools.projectWizard.core.buildList {
                +KMM_SERVICES
            },
            phases = GenerationPhase.ALL
        )
    }
}