/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.model.JavaModuleModel
import com.android.tools.idea.gradle.project.sync.idea.data.service.AndroidProjectKeys
import com.android.tools.idea.io.FilePaths
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.idea.configuration.detectPlatformKindByPlugin
import org.jetbrains.kotlin.idea.framework.detectLibraryKind
import org.jetbrains.kotlin.idea.platform.tooling
import java.io.File

class KotlinAndroidGradleLibraryDataService : AbstractProjectDataService<JavaModuleModel, Void>() {
    override fun getTargetDataKey() = AndroidProjectKeys.JAVA_MODULE_MODEL

    override fun postProcess(
        toImport: MutableCollection<DataNode<JavaModuleModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        for (dataNode in toImport) {
            @Suppress("UNCHECKED_CAST")
            val targetLibraryKind = detectPlatformKindByPlugin(dataNode.parent as DataNode<ModuleData>)?.tooling?.libraryKind
            if (targetLibraryKind != null) {
                for (dep in dataNode.data.jarLibraryDependencies) {
                    val library = modelsProvider.findLibraryByBinaryPath(dep.binaryPath) as LibraryEx? ?: continue
                    if (library.kind == null) {
                        val model = modelsProvider.getModifiableLibraryModel(library) as LibraryEx.ModifiableModelEx
                        detectLibraryKind(model.getFiles(OrderRootType.CLASSES))?.let { model.kind = it }
                    }
                }
            }
        }
    }

    private fun IdeModifiableModelsProvider.findLibraryByBinaryPath(path: File?): Library? {
        if (path == null) return null
        val url = FilePaths.pathToIdeaUrl(path)
        return allLibraries.firstOrNull { url in getModifiableLibraryModel(it).getUrls(OrderRootType.CLASSES) }
    }
}
