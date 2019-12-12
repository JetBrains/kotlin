/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.ProjectImportingWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.plugins.gradle.action.ImportProjectFromScriptAction
import java.nio.file.Path

class IdeaGradleWizardService(private val project: Project) : ProjectImportingWizardService,
    IdeaWizardService {
    override fun isSuitableFor(buildSystemType: BuildSystemType): Boolean =
        buildSystemType.isGradle

    // We have to call action directly as there is no common way
    // to import Gradle project in all IDEAs from 183 to 193
    override fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>
    ): TaskResult<Unit> = safe {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path.toString())!!
        val dataContext = SimpleDataContext.getSimpleContext(
            mapOf(
                CommonDataKeys.PROJECT.name to project,
                CommonDataKeys.VIRTUAL_FILE.name to virtualFile
            ),
            null
        )
        val action = ImportProjectFromScriptAction()
        val event = AnActionEvent.createFromAnAction(
            action,
            null,
            ActionPlaces.UNKNOWN,
            dataContext
        )
        action.actionPerformed(event)
    }
}