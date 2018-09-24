/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import org.jetbrains.idea.maven.importing.MavenImporter
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectChanges
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask
import org.jetbrains.idea.maven.project.MavenProjectsTree
import org.jetbrains.kotlin.idea.formatter.ProjectCodeStyleImporter
import org.jetbrains.kotlin.idea.maven.KotlinMavenImporter.Companion.KOTLIN_PLUGIN_ARTIFACT_ID
import org.jetbrains.kotlin.idea.maven.KotlinMavenImporter.Companion.KOTLIN_PLUGIN_GROUP_ID

class KotlinCodeStyleMavenImporter : MavenImporter(KOTLIN_PLUGIN_GROUP_ID, KOTLIN_PLUGIN_ARTIFACT_ID) {
    companion object {
        private const val KOTLIN_CODE_STYLE_MAVEN_SETTING = "kotlin.code.style"

        fun getCodeStyleString(mavenProject: MavenProject): String? {
            return mavenProject.properties.getProperty(KOTLIN_CODE_STYLE_MAVEN_SETTING)
        }
    }

    override fun isApplicable(mavenProject: MavenProject): Boolean {
        return getCodeStyleString(mavenProject) != null
    }

    override fun preProcess(
        module: Module?,
        mavenProject: MavenProject?,
        changes: MavenProjectChanges?,
        modifiableModelsProvider: IdeModifiableModelsProvider?
    ) {
        // Do nothing
    }

    override fun process(
        modifiableModelsProvider: IdeModifiableModelsProvider?,
        module: Module,
        rootModel: MavenRootModelAdapter?,
        mavenModel: MavenProjectsTree,
        mavenProject: MavenProject,
        changes: MavenProjectChanges?,
        mavenProjectToModuleName: MutableMap<MavenProject, String>?,
        postTasks: MutableList<MavenProjectsProcessorTask>
    ) {
        if (mavenProject !in mavenModel.rootProjects) return

        ProjectCodeStyleImporter.apply(module.project, getCodeStyleString(mavenProject))
    }
}