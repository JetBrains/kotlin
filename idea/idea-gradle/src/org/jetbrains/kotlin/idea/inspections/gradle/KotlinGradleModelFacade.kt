/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections.gradle

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.tooling.model.idea.IdeaProject

class DefaultGradleModelFacade : KotlinGradleModelFacade {
    override fun getResolvedVersionByModuleData(moduleData: DataNode<*>, groupId: String, libraryIds: List<String>): String? {
        for (libraryDependencyData in ExternalSystemApiUtil.findAllRecursively(moduleData, ProjectKeys.LIBRARY_DEPENDENCY)) {
            for (libraryId in libraryIds) {
                val libraryNameMarker = "$groupId:$libraryId:"
                if (libraryDependencyData.data.externalName.startsWith(libraryNameMarker)) {
                    return libraryDependencyData.data.externalName.substringAfter(libraryNameMarker)
                }
            }
        }
        return null
    }

    override fun getDependencyModules(ideModule: DataNode<ModuleData>, gradleIdeaProject: IdeaProject): Collection<DataNode<ModuleData>> {
        val ideProject = ideModule.parent as DataNode<ProjectData>
        val dependencyModuleNames =
            ExternalSystemApiUtil.getChildren(ideModule, ProjectKeys.MODULE_DEPENDENCY).map { it.data.target.externalName }.toHashSet()
        return findModulesByNames(dependencyModuleNames, gradleIdeaProject, ideProject)
    }
}

fun DataNode<*>.getResolvedVersionByModuleData(groupId: String, libraryIds: List<String>): String? {
    return KotlinGradleModelFacade.EP_NAME.extensions.asSequence()
        .mapNotNull { it.getResolvedVersionByModuleData(this, groupId, libraryIds) }
        .firstOrNull()
}

fun getDependencyModules(moduleData: DataNode<ModuleData>, gradleIdeaProject: IdeaProject): Collection<DataNode<ModuleData>> {
    for (modelFacade in Extensions.getExtensions(KotlinGradleModelFacade.EP_NAME)) {
        val dependencies = modelFacade.getDependencyModules(moduleData, gradleIdeaProject)
        if (dependencies.isNotEmpty()) {
            return dependencies
        }
    }
    return emptyList()
}

fun findModulesByNames(
    dependencyModuleNames: Set<String>,
    gradleIdeaProject: IdeaProject,
    ideProject: DataNode<ProjectData>
): LinkedHashSet<DataNode<ModuleData>> {
    val modules = ExternalSystemApiUtil.getChildren(ideProject, ProjectKeys.MODULE)
    return modules.filterTo(LinkedHashSet()) { it.data.externalName in dependencyModuleNames }
}
