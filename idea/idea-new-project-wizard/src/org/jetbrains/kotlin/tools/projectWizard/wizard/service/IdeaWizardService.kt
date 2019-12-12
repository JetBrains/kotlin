/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService

interface IdeaWizardService : WizardService

object IdeaServices {
    val PROJECT_INDEPENDENT: List<IdeaWizardService> = listOf(
        IdeaFileSystemWizardService(),
        IdeaBuildSystemAvailabilityWizardService()
    )

    fun createScopeDependent(project: Project, model: ModifiableModuleModel) = listOf(
        IdeaGradleWizardService(project),
        IdeaMavenWizardService(project),
        IdeaJpsWizardService(project, model)
    )
}


