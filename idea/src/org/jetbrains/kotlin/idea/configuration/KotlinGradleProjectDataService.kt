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
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CoroutineSupport
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.configureFacet
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.facet.mavenLibraryId
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

    operator fun invoke(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>)
}

class KotlinGradleProjectDataService : AbstractProjectDataService<GradleSourceSetData, Void>() {
    override fun getTargetDataKey() = GradleSourceSetData.KEY

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
        return TargetPlatformKind.ALL_PLATFORMS.firstOrNull { moduleNode.getResolvedKotlinStdlibVersionByModuleData(it.mavenLibraryId) != null }
    }

    override fun postProcess(
            toImport: Collection<DataNode<GradleSourceSetData>>,
            projectData: ProjectData?,
            project: Project,
            modelsProvider: IdeModifiableModelsProvider
    ) {
        for (sourceSetNode in toImport) {
            val sourceSetData = sourceSetNode.data
            val ideModule = modelsProvider.findIdeModule(sourceSetData) ?: continue

            val moduleNode = ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE)
            val compilerVersion = moduleNode?.findAll(BuildScriptClasspathData.KEY)?.firstOrNull()?.data?.let(::findKotlinPluginVersion)
                                  ?: continue
            val platformKind = detectPlatformByPlugin(moduleNode) ?: detectPlatformByLibrary(moduleNode)

            val coroutinesProperty = findKotlinCoroutinesProperty(project)

            val kotlinFacet = ideModule.getOrCreateFacet(modelsProvider, false)
            kotlinFacet.configureFacet(compilerVersion, coroutinesProperty, platformKind, modelsProvider)
            GradleProjectImportHandler.getInstances(project).forEach { it(kotlinFacet, sourceSetNode) }
        }
    }
}

private fun findKotlinCoroutinesProperty(project: Project): CoroutineSupport {
    val localPropertiesFile = project.baseDir.findChild("local.properties") ?: return CoroutineSupport.DEFAULT

    val properties = Properties()
    properties.load(localPropertiesFile.inputStream)
    val coroutinesProperty = properties.getProperty("kotlin.coroutines") ?: return CoroutineSupport.DEFAULT

    return CoroutineSupport.byCompilerArgument(coroutinesProperty)
}