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

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.AndroidProjectKeys
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.tooling.model.UnsupportedMethodException
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.kotlin.idea.inspections.gradle.KotlinGradleModelFacade
import org.jetbrains.kotlin.idea.inspections.gradle.findModulesByNames
import com.android.builder.model.Library

class AndroidGradleModelFacade : KotlinGradleModelFacade {
    override fun getResolvedKotlinStdlibVersionByModuleData(moduleData: DataNode<*>, libraryIds: List<String>): String? {
        ExternalSystemApiUtil
                .findAllRecursively(moduleData, AndroidProjectKeys.JAVA_PROJECT).asSequence()
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