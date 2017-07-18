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

import com.android.tools.idea.gradle.project.model.JavaModuleModel
import com.android.tools.idea.gradle.project.sync.idea.data.service.AndroidProjectKeys
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import org.jetbrains.kotlin.idea.configuration.detectPlatformByPlugin
import org.jetbrains.kotlin.idea.framework.libraryKind

class KotlinAndroidGradleLibraryDataService : AbstractProjectDataService<JavaModuleModel, Void>() {
    override fun getTargetDataKey() = AndroidProjectKeys.JAVA_MODULE_MODEL

    override fun postProcess(
            toImport: MutableCollection<DataNode<JavaModuleModel>>,
            projectData: ProjectData?,
            project: Project,
            modelsProvider: IdeModifiableModelsProvider
    ) {
        for (dataNode in toImport) {
            val targetLibraryKind = detectPlatformByPlugin(dataNode.parent as DataNode<ModuleData>)?.libraryKind
            if (targetLibraryKind != null) {
                for (dep in dataNode.data.jarLibraryDependencies) {
                    val library = modelsProvider.getLibraryByName(dep.name) as LibraryEx? ?: continue
                    if (library.kind == null) {
                        (modelsProvider.getModifiableLibraryModel(library) as LibraryEx.ModifiableModelEx).kind = targetLibraryKind
                    }
                }
            }
        }
    }
}
