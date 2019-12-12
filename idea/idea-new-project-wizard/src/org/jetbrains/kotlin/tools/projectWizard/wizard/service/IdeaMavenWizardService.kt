/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.ProjectImportingWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import java.nio.file.Path

class IdeaMavenWizardService(private val project: Project) : ProjectImportingWizardService,
    IdeaWizardService {
    override fun isSuitableFor(buildSystemType: BuildSystemType): Boolean =
        buildSystemType == BuildSystemType.Maven

    override fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>
    ): TaskResult<Unit> = safe {
        val mavenProjectManager = MavenProjectsManager.getInstance(project)

        val rootFile = LocalFileSystem.getInstance().findFileByPath(path.toString())!!
        mavenProjectManager.addManagedFilesOrUnignore(rootFile.findAllPomFiles())
    }

    private fun VirtualFile.findAllPomFiles(): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()

        fun VirtualFile.find() {
            when {
                !isDirectory && name == "pom.xml" -> result += this
                isDirectory -> children.forEach(VirtualFile::find)
            }
        }

        find()
        return result
    }
}