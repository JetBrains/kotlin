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
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.CoroutineSupport
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.idea.facet.*
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.inspections.gradle.findAll
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.idea.inspections.gradle.getResolvedKotlinStdlibVersionByModuleData
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File
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
            val kotlinFacet = configureFacetByGradleModule(moduleNode, sourceSetNode, ideModule, modelsProvider) ?: continue
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
            val kotlinFacet = configureFacetByGradleModule(moduleNode, null, ideModule, modelsProvider) ?: continue
            GradleProjectImportHandler.getInstances(project).forEach { it.importByModule(kotlinFacet, moduleNode) }
        }
    }
}

class KotlinGradleLibraryDataService : AbstractProjectDataService<LibraryData, Void>() {
    override fun getTargetDataKey() = ProjectKeys.LIBRARY

    override fun postProcess(
            toImport: MutableCollection<DataNode<LibraryData>>,
            projectData: ProjectData?,
            project: Project,
            modelsProvider: IdeModifiableModelsProvider
    ) {
        for (libraryDataNode in toImport) {
            val ideLibrary = modelsProvider.findIdeLibrary(libraryDataNode.data) ?: continue

            val projectDataNode = libraryDataNode.parent!! as DataNode<ProjectData>
            val ownerModule = findOwnerModule(libraryDataNode.data, projectDataNode) ?: continue
            val targetPlatform = detectPlatformByPlugin(ownerModule)
            if (targetPlatform == TargetPlatformKind.JavaScript) {
                val modifiableModel = modelsProvider.getModifiableLibraryModel(ideLibrary) as LibraryEx.ModifiableModelEx
                modifiableModel.kind = JSLibraryKind
            }
        }
    }
}

private fun findOwnerModule(libraryData: LibraryData,
                            projectDataNode: DataNode<ProjectData>): DataNode<ModuleData>? {
    return projectDataNode.children.firstOrNull { dataNode ->
        if (dataNode.data !is ModuleData) return@firstOrNull false
        val sourceSetDataNodes = dataNode.children.filter { it.data is GradleSourceSetData }
        sourceSetDataNodes.any { it.hasDependency(libraryData) }
    } as DataNode<ModuleData>?
}

private fun DataNode<*>.hasDependency(libraryData: LibraryData): Boolean =
        children.any {
            (it.data as? LibraryDependencyData)?.target == libraryData
        }

fun detectPlatformByPlugin(moduleNode: DataNode<ModuleData>): TargetPlatformKind<*>? {
    return when (moduleNode.platformPluginId) {
        "kotlin-platform-jvm" -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
        "kotlin-platform-js" -> TargetPlatformKind.JavaScript
        "kotlin-platform-common" -> TargetPlatformKind.Common
        else -> null
    }
}

private fun detectPlatformByLibrary(moduleNode: DataNode<ModuleData>): TargetPlatformKind<*>? {
    val detectedPlatforms = mavenLibraryIdToPlatform.entries.filter { moduleNode.getResolvedKotlinStdlibVersionByModuleData(listOf(it.key)) != null }.map { it.value }.distinct()
    return detectedPlatforms.singleOrNull() ?: detectedPlatforms.firstOrNull { it != TargetPlatformKind.Common }
}

private fun configureFacetByGradleModule(
        moduleNode: DataNode<ModuleData>,
        sourceSetNode: DataNode<GradleSourceSetData>?,
        ideModule: Module,
        modelsProvider: IdeModifiableModelsProvider
): KotlinFacet? {
    if (!moduleNode.isResolved) return null

    if (!moduleNode.hasKotlinPlugin) {
        val facetModel = modelsProvider.getModifiableFacetModel(ideModule)
        val facet = facetModel.getFacetByType(KotlinFacetType.TYPE_ID)
        if (facet != null) {
            facetModel.removeFacet(facet)
        }
        return null
    }

    val compilerVersion = moduleNode.findAll(BuildScriptClasspathData.KEY).firstOrNull()?.data?.let(::findKotlinPluginVersion)
                          ?: return null
    val platformKind = detectPlatformByPlugin(moduleNode) ?: detectPlatformByLibrary(moduleNode)

    val coroutinesProperty = CoroutineSupport.byCompilerArgument(
            moduleNode.coroutines ?: findKotlinCoroutinesProperty(ideModule.project))

    val kotlinFacet = ideModule.getOrCreateFacet(modelsProvider, false)
    kotlinFacet.configureFacet(compilerVersion, coroutinesProperty, platformKind, modelsProvider)

    val sourceSetName = sourceSetNode?.data?.id?.let { it.substring(it.lastIndexOf(':') + 1) } ?: "main"

    val argsInfo = moduleNode.compilerArgumentsBySourceSet?.get(sourceSetName)
    if (argsInfo != null) {
        val currentCompilerArguments = argsInfo.currentArguments
        val defaultCompilerArguments = argsInfo.defaultArguments
        val dependencyClasspath = argsInfo.dependencyClasspath.map { PathUtil.toSystemIndependentName(it) }
        if (currentCompilerArguments.isNotEmpty()) {
            parseCompilerArgumentsToFacet(currentCompilerArguments, defaultCompilerArguments, kotlinFacet, modelsProvider)
        }
        adjustClasspath(kotlinFacet, dependencyClasspath)
    }

    return kotlinFacet
}

private fun adjustClasspath(kotlinFacet: KotlinFacet, dependencyClasspath: List<String>) {
    if (dependencyClasspath.isEmpty()) return
    val arguments = kotlinFacet.configuration.settings.compilerArguments as? K2JVMCompilerArguments ?: return
    val fullClasspath = arguments.classpath?.split(File.pathSeparator) ?: emptyList()
    if (fullClasspath.isEmpty()) return
    val newClasspath = fullClasspath - dependencyClasspath
    arguments.classpath = if (newClasspath.isNotEmpty()) newClasspath.joinToString(File.pathSeparator) else null
}

private val gradlePropertyFiles = listOf("local.properties", "gradle.properties")

private fun findKotlinCoroutinesProperty(project: Project): String {
    for (propertyFileName in gradlePropertyFiles) {
        val propertyFile = project.baseDir.findChild(propertyFileName) ?: continue
        val properties = Properties()
        properties.load(propertyFile.inputStream)
        properties.getProperty("kotlin.coroutines")?.let { return it }
    }

    return CoroutineSupport.getCompilerArgument(LanguageFeature.Coroutines.defaultState)
}
