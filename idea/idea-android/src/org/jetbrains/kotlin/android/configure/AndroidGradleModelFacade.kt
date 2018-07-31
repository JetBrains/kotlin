/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.builder.model.Library
import com.android.tools.idea.gradle.project.sync.idea.data.service.AndroidProjectKeys
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.tooling.model.UnsupportedMethodException
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.kotlin.idea.inspections.gradle.KotlinGradleModelFacade
import org.jetbrains.kotlin.idea.inspections.gradle.findModulesByNames

class AndroidGradleModelFacade : KotlinGradleModelFacade {
    @Suppress("OverridingDeprecatedMember")
    override fun getResolvedKotlinStdlibVersionByModuleData(moduleData: DataNode<*>, libraryIds: List<String>): String? {
        ExternalSystemApiUtil
            .findAllRecursively(moduleData, AndroidProjectKeys.JAVA_MODULE_MODEL).asSequence()
            .flatMap { it.data.jarLibraryDependencies.asSequence() }
            .forEach {
                val libraryName = it.name
                for (libraryId in libraryIds) {
                    val prefix = "$libraryId-"
                    if (libraryName.startsWith(prefix)) return libraryName.substringAfter(prefix)
                }
            }
        return null
    }

    override fun getDependencyModules(ideModule: DataNode<ModuleData>, gradleIdeaProject: IdeaProject): Collection<DataNode<ModuleData>> {
        val ideProject = ideModule.parent as DataNode<ProjectData>
        ExternalSystemApiUtil.find(ideModule, AndroidProjectKeys.JAVA_MODULE_MODEL)?.let { javaModuleModel ->
            val moduleNames = javaModuleModel.data.javaModuleDependencies.map { it.moduleName }.toHashSet()
            return findModulesByNames(moduleNames, gradleIdeaProject, ideProject)
        }
        ExternalSystemApiUtil.find(ideModule, AndroidProjectKeys.ANDROID_MODEL)?.let { androidModel ->
            val libraries = androidModel.data.mainArtifact.dependencies.javaLibraries
            val projects = androidModel.data.mainArtifact.dependencies.projects
            val projectIds = libraries.mapNotNull { it.projectSafe } + projects
            return projectIds.mapNotNullTo(LinkedHashSet()) { projectId ->
                ExternalSystemApiUtil.findFirstRecursively(ideProject) {
                    (it.data as? ModuleData)?.id == projectId
                } as DataNode<ModuleData>?
            }

        }
        return emptyList()
    }
}

// com.android.builder.model.Library.getProject() is not present in 2.1.0
private val Library.projectSafe: String?
        get() = try {
           project
        } catch(e: UnsupportedMethodException) {
          null
        }