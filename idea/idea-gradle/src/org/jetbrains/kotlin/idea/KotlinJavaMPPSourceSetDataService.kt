/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.kotlinSourceSet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData

class KotlinJavaMPPSourceSetDataService : AbstractProjectDataService<GradleSourceSetData, Void>() {
    override fun getTargetDataKey() = GradleSourceSetData.KEY

    override fun postProcess(
        toImport: MutableCollection<DataNode<GradleSourceSetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val projectNode = toImport.firstOrNull()?.let { ExternalSystemApiUtil.findParent(it, ProjectKeys.PROJECT) } ?: return
        val targetsByUrl = ExternalSystemApiUtil
            .findAllRecursively(projectNode, KotlinTargetData.KEY)
            .groupBy { targetNode -> targetNode.data.archiveFile?.let { VfsUtil.getUrlForLibraryRoot(it) } }
        for (nodeToImport in toImport) {
            if (nodeToImport.kotlinSourceSet != null) continue
            val isTestSourceSet = nodeToImport.data.id.endsWith(":test")
            val moduleData = nodeToImport.data
            val module = modelsProvider.findIdeModule(moduleData) ?: continue
            val rootModel = modelsProvider.getModifiableRootModel(module)
            val libraryEntries = rootModel.orderEntries.filterIsInstance<LibraryOrderEntry>()
            libraryEntries.forEach { libraryEntry ->
                val library = libraryEntry.library ?: return@forEach
                val libraryModel = modelsProvider.getModifiableLibraryModel(library)
                val classesUrl = libraryModel.getUrls(OrderRootType.CLASSES).singleOrNull() ?: return@forEach
                val targetNode = targetsByUrl[classesUrl]?.singleOrNull() ?: return@forEach
                val groupingModuleNode = ExternalSystemApiUtil.findParent(targetNode, ProjectKeys.MODULE) ?: return@forEach
                val compilationNodes = ExternalSystemApiUtil
                    .getChildren(groupingModuleNode, GradleSourceSetData.KEY)
                    .filter { it.data.id in targetNode.data.moduleIds }
                for (compilationNode in compilationNodes) {
                    val compilationModule = modelsProvider.findIdeModule(compilationNode.data) ?: continue
                    val compilationInfo = compilationNode.kotlinSourceSet ?: continue
                    if (!isTestSourceSet && compilationInfo.isTestModule) continue
                    val compilationRootModel = modelsProvider.getModifiableRootModel(compilationModule)
                    addModuleDependencyIfNeeded(rootModel, compilationModule, isTestSourceSet)
                    compilationRootModel.getModuleDependencies(isTestSourceSet).forEach { transitiveDependee ->
                        addModuleDependencyIfNeeded(rootModel, transitiveDependee, isTestSourceSet)
                    }
                }
                rootModel.removeOrderEntry(libraryEntry)
            }
        }
    }
}