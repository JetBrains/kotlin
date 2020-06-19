/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.android.tools.idea.wizard.template.*
import com.jetbrains.konan.KonanLog
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase

object KmmTemplate : Template {
    private fun recipeImpl(moduleData: TemplateData) {
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
            moduleData.name,
            moduleData.packageName,
            projectData.sdkDir?.toPath()
        )

        KmmWizard(description).apply(
            WizardConfiguration.commonServices + WizardConfiguration.productionServices,
            GenerationPhase.ALL

        ).onFailure { errors ->
            val errorMessages = errors.joinToString(separator = "\n") { it.message }
            KonanLog.LOG.error("Failed to generate KMM template: $errorMessages")
        }
    }

    override val category = Category.Application
    override val constraints: Collection<TemplateConstraint> = emptyList()
    override val formFactor: FormFactor = FormFactor.Mobile

    override val minCompileSdk: Int = 0
    override val minSdk: Int = 0

    override val name: String = KotlinNewProjectWizardBundle.message("project.template.mpp.mobile.title")
    override val description: String = KotlinNewProjectWizardBundle.message("project.template.mpp.mobile.description")

    override val recipe: Recipe = { data -> recipeImpl(data) }

    override val revision: Int = 0

    override val uiContexts: Collection<WizardUiContext> = listOf(WizardUiContext.NewProject)
    override val widgets: Collection<Widget<*>> = emptyList()

    override fun thumb() = Thumb(this.javaClass.getResource("/META-INF/kmm-project-logo.png"))
}