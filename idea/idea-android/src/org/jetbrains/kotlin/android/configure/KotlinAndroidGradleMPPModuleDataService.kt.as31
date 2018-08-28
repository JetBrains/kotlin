/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.gradle.util.ContentEntries.findParentContentEntry
import com.android.tools.idea.io.FilePaths
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.service.project.manage.ContentRootDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.util.containers.stream
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.gradle.KotlinCompilation
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.idea.configuration.kotlinSourceSet
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File

class KotlinAndroidGradleMPPModuleDataService : AbstractProjectDataService<ModuleData, Void>() {
    override fun getTargetDataKey() = ProjectKeys.MODULE

    override fun postProcess(
        toImport: MutableCollection<DataNode<ModuleData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val contentRootsToImport = ArrayList<DataNode<ContentRootData>>()

        for (nodeToImport in toImport) {
            val projectNode = ExternalSystemApiUtil.findParent(nodeToImport, ProjectKeys.PROJECT) ?: continue

            for (childNode in ExternalSystemApiUtil.getChildren(nodeToImport, GradleSourceSetData.KEY)) {
                val sourceSet = childNode.kotlinSourceSet?.kotlinModule ?: continue
                if (sourceSet.platform == KotlinPlatform.ANDROID) continue
                contentRootsToImport += ExternalSystemApiUtil.getChildren(childNode, ProjectKeys.CONTENT_ROOT)
            }

            val moduleData = nodeToImport.data
            val module = modelsProvider.findIdeModule(moduleData) ?: continue
            val androidModel = AndroidModuleModel.get(module) ?: continue
            val variantName = androidModel.selectedVariant.name
            val sourceSetInfo = nodeToImport.kotlinAndroidSourceSets?.firstOrNull { it.kotlinModule.name == variantName } ?: continue
            val compilation = sourceSetInfo.kotlinModule as? KotlinCompilation ?: continue
            val rootModel = modelsProvider.getModifiableRootModel(module)
            for (sourceSet in compilation.sourceSets) {
                if (sourceSet.platform == KotlinPlatform.ANDROID) {
                    val sourceType = if (sourceSet.isTestModule) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
                    val resourceType = if (sourceSet.isTestModule) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE
                    sourceSet.sourceDirs.forEach { addSourceRoot(it, sourceType, rootModel) }
                    sourceSet.resourceDirs.forEach { addSourceRoot(it, resourceType, rootModel) }
                } else {
                    val sourceSetId = sourceSetInfo.sourceSetIdsByName[sourceSet.name] ?: continue
                    val sourceSetData = ExternalSystemApiUtil.findFirstRecursively(projectNode) {
                        (it.data as? ModuleData)?.id == sourceSetId
                    }?.data as? ModuleData ?: continue
                    val sourceSetModule = modelsProvider.findIdeModule(sourceSetData) ?: continue
                    rootModel.addModuleOrderEntry(sourceSetModule)
                }
            }
        }

        ContentRootDataService().importData(contentRootsToImport, projectData, project, modelsProvider)
    }

    private fun addSourceRoot(
        sourceRoot: File,
        type: JpsModuleSourceRootType<*>,
        rootModel: ModifiableRootModel
    ) {
        val parent = findParentContentEntry(sourceRoot, rootModel.contentEntries.stream()) ?: return
        val url = FilePaths.pathToIdeaUrl(sourceRoot)
        parent.addSourceFolder(url, type)
    }
}