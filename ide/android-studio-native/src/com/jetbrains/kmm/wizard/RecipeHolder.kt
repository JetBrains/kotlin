/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.android.tools.idea.wizard.template.*
import com.jetbrains.konan.KonanLog
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase

open class RecipeHolder {
    protected fun recipeImpl(moduleData: TemplateData) {
        if (moduleData !is ModuleTemplateData) return

        val projectData = moduleData.projectTemplateData

        val androidDir = moduleData.rootDir
        val projectDir = projectData.rootDir

        val settingsGradle = projectDir.resolve("settings.gradle")

        // recipes are called twice:
        // - for the first time FindReferencesRecipeExecutor does some reconnaissance
        // - for the second time actual rendering happens
        if (!settingsGradle.exists()) return

        // ModuleTemplateData does not have a link to project name, it is private in RecipeExecutor
        val providedProjectName = scanForProjectName(settingsGradle.readLines())

        safelyRemove(androidDir)
        cleanUpRootDir(projectDir)

        val description = ProjectDescription(
            providedProjectName,
            projectDir.toPath(),
            moduleData.packageName,
            projectData.sdkDir?.toPath(),
            androidAppName.value,
            iosAppName.value,
            sharedName.value,
            sharedTests.value
        )

        KmmWizard(description).apply(
            WizardConfiguration.commonServices + WizardConfiguration.productionServices,
            GenerationPhase.ALL

        ).onFailure { errors ->
            val errorMessages = errors.joinToString(separator = "\n") { it.message }
            KonanLog.LOG.error("Failed to generate KMM template: $errorMessages")
        }
    }

    protected val androidAppName: StringParameter = stringParameter {
        name = "Android Application Name"
        default = "androidApp"
        help = "The name of the module responsible for Android application"
        suggest = { default }
    }

    protected val iosAppName: StringParameter = stringParameter {
        name = "iOS Application Name"
        default = "iosApp"
        help = "The name of the Xcode project"
        suggest = { default }
    }

    protected val sharedName: StringParameter = stringParameter {
        name = "Shared Module Name"
        default = "shared"
        help = "The name of the common module"
        suggest = { default }
    }

    protected val sharedTests = booleanParameter {
        name = "Add sample tests for Shared Module"
        default = false
        help = "Adds test stubs into the common module"
    }
}