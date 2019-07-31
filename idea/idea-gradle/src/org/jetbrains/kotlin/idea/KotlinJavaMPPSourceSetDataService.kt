/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.ModuleOrderEntryImpl
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.caches.project.isTestModule
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.kotlinSourceSet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData

class KotlinJavaMPPSourceSetDataService : AbstractProjectDataService<GradleSourceSetData, Void>() {
    override fun getTargetDataKey() = GradleSourceSetData.KEY

    private fun isTestModuleById(id: String, toImport: Collection<DataNode<GradleSourceSetData>>): Boolean =
        toImport.firstOrNull { it.data.internalName == id }?.kotlinSourceSet?.isTestModule ?: false

    override fun postProcess(
        toImport: MutableCollection<DataNode<GradleSourceSetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val testKotlinModules =
            toImport.filter { it.kotlinSourceSet?.isTestModule ?: false }.map { modelsProvider.findIdeModule(it.data) }.toSet()
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

            val moduleEntries = rootModel.orderEntries.filterIsInstance<ModuleOrderEntry>()
            moduleEntries.filter { isTestModuleById(it.moduleName, toImport) }.forEach {moduleOrderEntry ->
                (moduleOrderEntry as? ModuleOrderEntryImpl)?.isProductionOnTestDependency = true
            }
            val libraryEntries = rootModel.orderEntries.filterIsInstance<LibraryOrderEntry>()
            libraryEntries.forEach { libraryEntry ->
                //TODO check that this code is nessecary any more. In general case all dependencies on MPP are already resolved into module dependencies
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
                    addModuleDependencyIfNeeded(
                        rootModel,
                        compilationModule,
                        isTestSourceSet,
                        compilationNode.kotlinSourceSet?.isTestModule ?: false
                    )
                    compilationRootModel.getModuleDependencies(isTestSourceSet).forEach { transitiveDependee ->
                        addModuleDependencyIfNeeded(
                            rootModel,
                            transitiveDependee,
                            isTestSourceSet,
                            testKotlinModules.contains(transitiveDependee)
                        )
                    }
                }
                rootModel.removeOrderEntry(libraryEntry)
            }
        }
    }
}