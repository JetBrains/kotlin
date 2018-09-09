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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.impl.libraries.LibraryImpl
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.util.Key
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.gradle.ArgsInfo
import org.jetbrains.kotlin.gradle.CompilerArgumentsBySourceSet
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.facet.*
import org.jetbrains.kotlin.idea.formatter.KotlinObsoleteCodeStyle
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle
import org.jetbrains.kotlin.idea.formatter.ProjectCodeStyleImporter
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.detectLibraryKind
import org.jetbrains.kotlin.idea.inspections.gradle.findAll
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.idea.inspections.gradle.getResolvedVersionByModuleData
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.roots.migrateNonJvmSourceFolders
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.impl.isJavaScript
import org.jetbrains.kotlin.platform.impl.isJvm
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.util.*

var Module.compilerArgumentsBySourceSet
        by UserDataProperty(Key.create<CompilerArgumentsBySourceSet>("CURRENT_COMPILER_ARGUMENTS"))

var Module.sourceSetName
        by UserDataProperty(Key.create<String>("SOURCE_SET_NAME"))

interface GradleProjectImportHandler {
    companion object : ProjectExtensionDescriptor<GradleProjectImportHandler>(
        "org.jetbrains.kotlin.gradleProjectImportHandler",
        GradleProjectImportHandler::class.java
    )

    fun importBySourceSet(facet: KotlinFacet, sourceSetNode: DataNode<GradleSourceSetData>)
    fun importByModule(facet: KotlinFacet, moduleNode: DataNode<ModuleData>)
}

class KotlinGradleProjectSettingsDataService : AbstractProjectDataService<ProjectData, Void>() {
    override fun getTargetDataKey() = ProjectKeys.PROJECT

    override fun postProcess(
        toImport: MutableCollection<DataNode<ProjectData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val allSettings = modelsProvider.modules.mapNotNull {
            val settings = modelsProvider
                .getModifiableFacetModel(it)
                .findFacet(KotlinFacetType.TYPE_ID, KotlinFacetType.INSTANCE.defaultFacetName)
                ?.configuration
                ?.settings ?: return@mapNotNull null
            if (settings.useProjectSettings) null else settings
        }
        val languageVersion = allSettings.asSequence().mapNotNullTo(LinkedHashSet()) { it.languageLevel }.singleOrNull()
        val apiVersion = allSettings.asSequence().mapNotNullTo(LinkedHashSet()) { it.apiLevel }.singleOrNull()
        KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
            if (languageVersion != null) {
                this.languageVersion = languageVersion.versionString
            }
            if (apiVersion != null) {
                this.apiVersion = apiVersion.versionString
            }
        }
    }
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
            val kotlinFacet = configureFacetByGradleModule(ideModule, modelsProvider, moduleNode, sourceSetNode) ?: continue
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
            val kotlinFacet = configureFacetByGradleModule(ideModule, modelsProvider, moduleNode, null) ?: continue
            GradleProjectImportHandler.getInstances(project).forEach { it.importByModule(kotlinFacet, moduleNode) }
        }

        val codeStyleStr = GradlePropertiesFileUtils.readProperty(project, GradlePropertiesFileUtils.KOTLIN_CODE_STYLE_GRADLE_SETTING)
        ProjectCodeStyleImporter.apply(project, codeStyleStr)
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
        if (toImport.isEmpty()) return
        val projectDataNode = toImport.first().parent!!
        @Suppress("UNCHECKED_CAST")
        val moduleDataNodes = projectDataNode.children.filter { it.data is ModuleData } as List<DataNode<ModuleData>>
        val anyNonJvmModules = moduleDataNodes
            .any { node -> detectPlatformKindByPlugin(node)?.takeIf { !it.isJvm } != null }

        for (libraryDataNode in toImport) {
            val ideLibrary = modelsProvider.findIdeLibrary(libraryDataNode.data) ?: continue

            val modifiableModel = modelsProvider.getModifiableLibraryModel(ideLibrary) as LibraryEx.ModifiableModelEx
            if (anyNonJvmModules || ideLibrary.name?.looksAsNonJvmLibraryName() == true) {
                detectLibraryKind(modifiableModel.getFiles(OrderRootType.CLASSES))?.let { modifiableModel.kind = it }
            } else if (ideLibrary is LibraryImpl && (ideLibrary.kind === JSLibraryKind || ideLibrary.kind === CommonLibraryKind)) {
                resetLibraryKind(modifiableModel)
            }
        }
    }

    private fun String.looksAsNonJvmLibraryName() = nonJvmSuffixes.any { it in this }

    private fun resetLibraryKind(modifiableModel: LibraryEx.ModifiableModelEx) {
        try {
            val cls = LibraryImpl::class.java
            // Don't use name-based lookup because field names are scrambled in IDEA Ultimate
            for (field in cls.declaredFields) {
                if (field.type == PersistentLibraryKind::class.java) {
                    field.isAccessible = true
                    field.set(modifiableModel, null)
                    return
                }
            }
            LOG.info("Could not find field of type PersistentLibraryKind in LibraryImpl.class")
        } catch (e: Exception) {
            LOG.info("Failed to reset library kind", e)
        }
    }

    companion object {
        val LOG = Logger.getInstance(KotlinGradleLibraryDataService::class.java)

        val nonJvmSuffixes = listOf("-common", "-js", "-native", "-kjsm")
    }
}

fun detectPlatformKindByPlugin(moduleNode: DataNode<ModuleData>): IdePlatformKind<*>? {
    val pluginId = moduleNode.platformPluginId
    return IdePlatformKind.ALL_KINDS.firstOrNull { it.tooling.gradlePluginId == pluginId }
}

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use detectPlatformKindByPlugin() instead",
    replaceWith = ReplaceWith("detectPlatformKindByPlugin(moduleNode)"),
    level = DeprecationLevel.ERROR
)
fun detectPlatformByPlugin(moduleNode: DataNode<ModuleData>): TargetPlatformKind<*>? {
    return when (moduleNode.platformPluginId) {
        "kotlin-platform-jvm" -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
        "kotlin-platform-js" -> TargetPlatformKind.JavaScript
        "kotlin-platform-common" -> TargetPlatformKind.Common
        else -> null
    }
}

private fun detectPlatformByLibrary(moduleNode: DataNode<ModuleData>): IdePlatformKind<*>? {
    val detectedPlatforms =
        mavenLibraryIdToPlatform.entries
            .filter { moduleNode.getResolvedVersionByModuleData(KOTLIN_GROUP_ID, listOf(it.key)) != null }
            .map { it.value }.distinct()
    return detectedPlatforms.singleOrNull() ?: detectedPlatforms.firstOrNull { !it.isCommon }
}

@Suppress("unused") // Used in the Android plugin
fun configureFacetByGradleModule(
    moduleNode: DataNode<ModuleData>,
    sourceSetName: String?,
    ideModule: Module,
    modelsProvider: IdeModifiableModelsProvider
): KotlinFacet? {
    return configureFacetByGradleModule(ideModule, modelsProvider, moduleNode, null, sourceSetName)
}

fun configureFacetByGradleModule(
    ideModule: Module,
    modelsProvider: IdeModifiableModelsProvider,
    moduleNode: DataNode<ModuleData>,
    sourceSetNode: DataNode<GradleSourceSetData>?,
    sourceSetName: String? = sourceSetNode?.data?.id?.let { it.substring(it.lastIndexOf(':') + 1) }
): KotlinFacet? {
    if (moduleNode.kotlinSourceSet != null) return null // Suppress in the presence of new MPP model
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
    val platformKind = detectPlatformKindByPlugin(moduleNode) ?: detectPlatformByLibrary(moduleNode)

    // TODO there should be a way to figure out the correct platform version
    val platform = platformKind?.defaultPlatform

    val coroutinesProperty = CoroutineSupport.byCompilerArgument(
        moduleNode.coroutines ?: findKotlinCoroutinesProperty(ideModule.project)
    )

    val kotlinFacet = ideModule.getOrCreateFacet(modelsProvider, false, GradleConstants.SYSTEM_ID.id)
    kotlinFacet.configureFacet(compilerVersion, coroutinesProperty, platform, modelsProvider)

    if (sourceSetNode == null) {
        ideModule.compilerArgumentsBySourceSet = moduleNode.compilerArgumentsBySourceSet
        ideModule.sourceSetName = sourceSetName
    }

    val argsInfo = moduleNode.compilerArgumentsBySourceSet?.get(sourceSetName ?: "main")
    if (argsInfo != null) {
        configureFacetByCompilerArguments(kotlinFacet, argsInfo, modelsProvider)
    }

    with(kotlinFacet.configuration.settings) {
        implementedModuleNames = (sourceSetNode ?: moduleNode).implementedModuleNames
        productionOutputPath = getExplicitOutputPath(moduleNode, platformKind, "main")
        testOutputPath = getExplicitOutputPath(moduleNode, platformKind, "test")
    }

    kotlinFacet.noVersionAutoAdvance()

    if (platformKind != null && !platformKind.isJvm) {
        migrateNonJvmSourceFolders(modelsProvider.getModifiableRootModel(ideModule))
    }

    return kotlinFacet
}

fun configureFacetByCompilerArguments(kotlinFacet: KotlinFacet, argsInfo: ArgsInfo, modelsProvider: IdeModifiableModelsProvider?) {
    val currentCompilerArguments = argsInfo.currentArguments
    val defaultCompilerArguments = argsInfo.defaultArguments
    val dependencyClasspath = argsInfo.dependencyClasspath.map { PathUtil.toSystemIndependentName(it) }
    if (currentCompilerArguments.isNotEmpty()) {
        parseCompilerArgumentsToFacet(currentCompilerArguments, defaultCompilerArguments, kotlinFacet, modelsProvider)
    }
    adjustClasspath(kotlinFacet, dependencyClasspath)
}

private fun getExplicitOutputPath(moduleNode: DataNode<ModuleData>, platformKind: IdePlatformKind<*>?, sourceSet: String): String? {
    if (!platformKind.isJavaScript) {
        return null
    }

    val k2jsArgumentList = moduleNode.compilerArgumentsBySourceSet?.get(sourceSet)?.currentArguments ?: return null
    return K2JSCompilerArguments().apply { parseCommandLineArguments(k2jsArgumentList, this) }.outputFile
}

internal fun adjustClasspath(kotlinFacet: KotlinFacet, dependencyClasspath: List<String>) {
    if (dependencyClasspath.isEmpty()) return
    val arguments = kotlinFacet.configuration.settings.compilerArguments as? K2JVMCompilerArguments ?: return
    val fullClasspath = arguments.classpath?.split(File.pathSeparator) ?: emptyList()
    if (fullClasspath.isEmpty()) return
    val newClasspath = fullClasspath - dependencyClasspath
    arguments.classpath = if (newClasspath.isNotEmpty()) newClasspath.joinToString(File.pathSeparator) else null
}

internal fun findKotlinCoroutinesProperty(project: Project): String {
    return GradlePropertiesFileUtils.readProperty(project, "kotlin.coroutines")
        ?: CoroutineSupport.getCompilerArgument(LanguageFeature.Coroutines.defaultState)
}
