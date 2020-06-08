/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.service.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.AndroidPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.RunConfigurationsPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.KotlinDslPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin

object WizardConfiguration {
    val commonServices: List<WizardService> = listOf(
        KmmBuildSystemAvailabilityWizardService(),
        KmmFileSystemWizardService(),
        KmmKotlinVersionProviderService(),
        SettingSavingWizardServiceImpl(),
        VelocityTemplateEngineServiceImpl(),
        DummyFileFormattingService(),
        RunConfigurationsServiceImpl()
    )

    val productionServices: List<WizardService> = listOf(
        ProjectImportingWizardServiceImpl()
    )

    val servicesManager = ServicesManager(commonServices) { services ->
        services.firstOrNull()
    }

    val pluginsCreator = { context: Context ->
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
}