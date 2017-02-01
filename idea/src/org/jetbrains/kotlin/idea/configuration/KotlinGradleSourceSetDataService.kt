/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CoroutineSupport
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.idea.facet.*
import org.jetbrains.kotlin.idea.inspections.gradle.findAll
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.idea.inspections.gradle.getResolvedKotlinStdlibVersionByModuleData
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataService
import java.util.*

interface GradleProjectImportHandler {
    companion object : ProjectExtensionDescriptor<GradleProjectImportHandler>(
            "org.jetbrains.kotlin.gradleProjectImportHandler",
            GradleProjectImportHandler::class.java
    )

    fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>)
    fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>)
}

class KotlinGradleSourceSetDataService : AbstractProjectDataService<GradleSourceSetData, Void>() {
    override fun getTargetDataKey() = GradleSourceSetData.KEY

    override fun postProcess(
            toImport: Collection<DataNode<GradleSourceSetData>>,
            projectData: ProjectData?,
            project: Project,
            modelsProvider: IdeModifiableModelsProvider
    ) {
        for (sourceSetNode in toImport) {
            val sourceSetData = sourceSetNode.data
            val ideModule = modelsProvider.findIdeModule(sourceSetData) ?: continue

            val moduleNode = ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE) ?: continue
            val kotlinFacet = configureFacetByGradleModule(moduleNode, ideModule, modelsProvider) ?: continue
            GradleProjectImportHandler.getInstances(project).forEach { it.importBySourceSet(kotlinFacet, sourceSetNode) }
        }
    }
}

class KotlinGradleProjectDataService : AbstractProjectDataService<ModuleData, Void>() {
    override fun getTargetDataKey() = ProjectKeys.MODULE

    override fun postProcess(
            toImport: MutableCollection<DataNode<ModuleData>>,
            projectData: ProjectData?,
            project: Project,
            modelsProvider: IdeModifiableModelsProvider
    ) {
        for (moduleNode in toImport) {
            // If source sets are present, configure facets in the their modules
            if (ExternalSystemApiUtil.getChildren(moduleNode, GradleSourceSetData.KEY).isNotEmpty()) continue

            val moduleData = moduleNode.data
            val ideModule = modelsProvider.findIdeModule(moduleData) ?: continue
            val kotlinFacet = configureFacetByGradleModule(moduleNode, ideModule, modelsProvider) ?: continue
            GradleProjectImportHandler.getInstances(project).forEach { it.importByModule(kotlinFacet, moduleNode) }
        }
    }
}

private fun detectPlatformByPlugin(moduleNode: DataNode<ModuleData>): TargetPlatformKind<*>? {
    val projectNode = ExternalSystemApiUtil.findParent(moduleNode, ProjectKeys.PROJECT)
    val externalProjectNode = ExternalSystemApiUtil.find(projectNode as DataNode<*>, ExternalProjectDataService.KEY)
    return externalProjectNode?.let {
        when (it.data.plugins.values.map { it.id }.firstOrNull { it.startsWith("kotlin-platform-") }) {
            "kotlin-platform-jvm" -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
            "kotlin-platform-js" -> TargetPlatformKind.JavaScript
            "kotlin-platform-common" -> TargetPlatformKind.Common
            else -> null
        }
    }
}

private fun detectPlatformByLibrary(moduleNode: DataNode<ModuleData>): TargetPlatformKind<*>? {
    return TargetPlatformKind.ALL_PLATFORMS.firstOrNull { moduleNode.getResolvedKotlinStdlibVersionByModuleData(it.mavenLibraryIds) != null }
}

private fun configureFacetByGradleModule(
        moduleNode: DataNode<ModuleData>,
        ideModule: Module,
        modelsProvider: IdeModifiableModelsProvider
): KotlinFacet? {
    val compilerVersion = moduleNode.findAll(BuildScriptClasspathData.KEY).firstOrNull()?.data?.let(::findKotlinPluginVersion)
                          ?: return null
    val platformKind = detectPlatformByPlugin(moduleNode) ?: detectPlatformByLibrary(moduleNode)

    val coroutinesProperty = CoroutineSupport.byCompilerArgument(
            moduleNode.coroutines ?: findKotlinCoroutinesProperty(ideModule.project))

    val kotlinFacet = ideModule.getOrCreateFacet(modelsProvider, false)
    kotlinFacet.configureFacet(compilerVersion, coroutinesProperty, platformKind, modelsProvider)

    moduleNode.serializedCompilerArguments?.let { parseCompilerArgumentsToFacet(it, kotlinFacet) }

    return kotlinFacet
}

private val gradlePropertyFiles = listOf("local.properties", "gradle.properties")

private fun findKotlinCoroutinesProperty(project: Project): String {
    for (propertyFileName in gradlePropertyFiles) {
        val propertyFile = project.baseDir.findChild(propertyFileName) ?: continue
        val properties = Properties()
        properties.load(propertyFile.inputStream)
        properties.getProperty("kotlin.coroutines")?.let { return it }
    }

    return CoroutineSupport.DEFAULT.compilerArgument
}