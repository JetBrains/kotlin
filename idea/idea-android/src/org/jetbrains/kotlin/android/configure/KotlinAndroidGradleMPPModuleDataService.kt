/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.sync.idea.data.service.AndroidProjectKeys
import com.android.tools.idea.gradle.util.ContentEntries.findParentContentEntry
import com.android.tools.idea.io.FilePaths
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.service.project.manage.ContentRootDataService.CREATE_EMPTY_DIRECTORIES
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.stream
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.gradle.KotlinCompilation
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.gradle.KotlinSourceSet
import org.jetbrains.kotlin.idea.addModuleDependencyIfNeeded
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.io.File
import java.io.IOException
import org.jetbrains.kotlin.idea.configuration.kotlinSourceSet

class KotlinAndroidGradleMPPModuleDataService : AbstractProjectDataService<ModuleData, Void>() {
    override fun getTargetDataKey() = ProjectKeys.MODULE

    private fun shouldCreateEmptySourceRoots(
        moduleDataNode: DataNode<ModuleData>,
        module: Module
    ): Boolean {
        val projectDataNode = ExternalSystemApiUtil.findParent(moduleDataNode, ProjectKeys.PROJECT) ?: return false
        if (projectDataNode.getUserData<Boolean>(CREATE_EMPTY_DIRECTORIES) == true) return true

        val projectSystemId = projectDataNode.data.owner
        val externalSystemSettings = ExternalSystemApiUtil.getSettings(module.project, projectSystemId)

        val path = ExternalSystemModulePropertyManager.getInstance(module).getRootProjectPath() ?: return false
        return externalSystemSettings.getLinkedProjectSettings(path)?.isCreateEmptyContentRootDirectories ?: false
    }

    override fun postProcess(
        toImport: MutableCollection<DataNode<ModuleData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        for (nodeToImport in toImport) {
            val projectNode = ExternalSystemApiUtil.findParent(nodeToImport, ProjectKeys.PROJECT) ?: continue
            val moduleData = nodeToImport.data
            val module = modelsProvider.findIdeModule(moduleData) ?: continue
            val shouldCreateEmptySourceRoots = shouldCreateEmptySourceRoots(nodeToImport, module)
            val rootModel = modelsProvider.getModifiableRootModel(module)
            val kotlinAndroidSourceSets = nodeToImport.kotlinAndroidSourceSets ?: emptyList()
            for (sourceSetInfo in kotlinAndroidSourceSets) {
                val compilation = sourceSetInfo.kotlinModule as? KotlinCompilation ?: continue
                for (sourceSet in compilation.sourceSets) {
                    if (sourceSet.actualPlatforms.supports(KotlinPlatform.ANDROID)) {
                        val sourceType = if (sourceSet.isTestModule) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
                        val resourceType = if (sourceSet.isTestModule) JavaResourceRootType.TEST_RESOURCE else JavaResourceRootType.RESOURCE
                        sourceSet.sourceDirs.forEach { addSourceRoot(it, sourceType, rootModel, shouldCreateEmptySourceRoots) }
                        sourceSet.resourceDirs.forEach { addSourceRoot(it, resourceType, rootModel, shouldCreateEmptySourceRoots) }
                    }
                }
            }
            addExtraDependeeModules(nodeToImport, projectNode, modelsProvider, rootModel, false)
            addExtraDependeeModules(nodeToImport, projectNode, modelsProvider, rootModel, true)

            if (nodeToImport.kotlinAndroidSourceSets == null) {
                continue
            }

            val androidModel = getAndroidModuleModel(nodeToImport) ?: continue
            val variantName = androidModel.selectedVariant.name
            val activeSourceSetInfos = nodeToImport.kotlinAndroidSourceSets?.filter { it.kotlinModule.name.startsWith(variantName) } ?: emptyList()
            for (activeSourceSetInfo in activeSourceSetInfos) {
                val activeCompilation = activeSourceSetInfo.kotlinModule as? KotlinCompilation ?: continue
                for (sourceSet in activeCompilation.sourceSets) {
                    if (! sourceSet.actualPlatforms.supports(KotlinPlatform.ANDROID)) {
                        val sourceSetId = activeSourceSetInfo.sourceSetIdsByName[sourceSet.name] ?: continue
                        val sourceSetNode = ExternalSystemApiUtil.findFirstRecursively(projectNode) {
                            (it.data as? ModuleData)?.id == sourceSetId
                        } as? DataNode<out ModuleData>? ?: continue
                        val sourceSetData = sourceSetNode.data as? ModuleData ?: continue
                        val sourceSetModule = modelsProvider.findIdeModule(sourceSetData) ?: continue
                        addModuleDependencyIfNeeded(
                            rootModel,
                            sourceSetModule,
                            activeSourceSetInfo.isTestModule,
                            sourceSetNode.kotlinSourceSet?.isTestModule ?: false
                        )
                    }
                }
            }

            val mainSourceSetInfo = activeSourceSetInfos.firstOrNull { it.kotlinModule.name == variantName }
            if (mainSourceSetInfo != null) {
                KotlinSourceSetDataService.configureFacet(moduleData, mainSourceSetInfo, nodeToImport, module, modelsProvider)
            }

            val kotlinFacet = KotlinFacet.get(module)
            if (kotlinFacet != null) {
                GradleProjectImportHandler.getInstances(project).forEach { it.importByModule(kotlinFacet, nodeToImport) }
            }
        }
    }

    private fun getDependeeModuleNodes(
        moduleNode: DataNode<ModuleData>,
        projectNode: DataNode<ProjectData>,
        modelsProvider: IdeModifiableModelsProvider,
        testScope: Boolean
    ): List<DataNode<out ModuleData>> {
        val androidModel = getAndroidModuleModel(moduleNode)
        if (androidModel != null) {
            val dependencies = if (testScope) {
                androidModel.selectedAndroidTestCompileDependencies
            } else {
                androidModel.selectedMainCompileLevel2Dependencies
            } ?: return emptyList()
            return dependencies
                .moduleDependencies
                .mapNotNull { projectNode.findChildModuleById(it.projectPath) }
        }

        val javaModel = getJavaModuleModel(moduleNode)
        if (javaModel != null) {
            val scope = if (testScope) DependencyScope.TEST.name else DependencyScope.COMPILE.name
            val moduleNames = javaModel
                .javaModuleDependencies
                .filter { scope == it.scope ?: DependencyScope.COMPILE.name }
                .mapTo(HashSet()) { it.moduleName }
            return ExternalSystemApiUtil
                .getChildren(projectNode, ProjectKeys.MODULE)
                .filter { modelsProvider.findIdeModule(it.data)?.name in moduleNames }
        }

        return emptyList()
    }

    private fun List<DataNode<GradleSourceSetData>>.firstByPlatformOrNull(platform: KotlinPlatform) = firstOrNull {
        it.kotlinSourceSet?.actualPlatforms?.supports(platform) ?: false
    }

    private fun List<DataNode<GradleSourceSetData>>.filterByPlatform(platform: KotlinPlatform) = this.filter {
        it.kotlinSourceSet?.actualPlatforms?.supports(platform) ?: false
    }

    private fun addExtraDependeeModules(
        moduleNode: DataNode<ModuleData>,
        projectNode: DataNode<ProjectData>,
        modelsProvider: IdeModifiableModelsProvider,
        rootModel: ModifiableRootModel,
        testScope: Boolean
    ) {
        val legacyMode = !Registry.`is`("kotlin.android.import.mpp.all.transitive", true)
        val dependeeModuleNodes = getDependeeModuleNodes(moduleNode, projectNode, modelsProvider, testScope)
        val relevantNodes = dependeeModuleNodes
            .flatMap { ExternalSystemApiUtil.getChildren(it, GradleSourceSetData.KEY) }
            .filter {
                val ktModule = it.kotlinSourceSet?.kotlinModule
                ktModule != null && ktModule.isTestModule == testScope
            }
        val commonSourceSetName = KotlinSourceSet.commonName(testScope)
        val isAndroidModule = getAndroidModuleModel(moduleNode) != null

        val gradleSourceSetDataNodes = SmartList<DataNode<GradleSourceSetData>>()
            .apply {
                if (legacyMode) {
                    addIfNotNull(
                        (if (isAndroidModule) relevantNodes.firstByPlatformOrNull(KotlinPlatform.ANDROID) else null)
                            ?: relevantNodes.firstByPlatformOrNull(KotlinPlatform.JVM)
                    )
                    addIfNotNull(
                        relevantNodes.firstOrNull {
                            (it.kotlinSourceSet?.actualPlatforms?.supports(KotlinPlatform.COMMON) ?: false) && it.kotlinSourceSet?.kotlinModule?.name == commonSourceSetName
                        }
                    )
                } else {
                    val androidSourceSets = if (isAndroidModule) relevantNodes.filterByPlatform(KotlinPlatform.ANDROID) else emptyList()
                    if (androidSourceSets.isEmpty()) {
                        addAll(relevantNodes.filterByPlatform(KotlinPlatform.JVM))
                    } else {
                        addAll(androidSourceSets)
                    }
                    addAll(
                        relevantNodes.filter {
                            (it.kotlinSourceSet?.actualPlatforms?.supports(KotlinPlatform.COMMON) ?: false) && it.kotlinSourceSet?.kotlinModule?.name == commonSourceSetName
                        }
                    )
                }
            }

        val testKotlinModules =
            gradleSourceSetDataNodes.filter { it.kotlinSourceSet?.isTestModule ?: false }.mapNotNull { modelsProvider.findIdeModule(it.data) }
                .toSet()

        gradleSourceSetDataNodes.forEach { node ->
            val module = modelsProvider.findIdeModule(node.data) ?: return
            addModuleDependencyIfNeeded(rootModel, module, testScope, node.kotlinSourceSet?.isTestModule ?: false)
            val dependeeRootModel = modelsProvider.getModifiableRootModel(module)
            dependeeRootModel.getModuleDependencies(testScope).forEach { transitiveDependee ->
                addModuleDependencyIfNeeded(rootModel, transitiveDependee, testScope, testKotlinModules.contains(transitiveDependee))
            }
        }
    }

    private fun getAndroidModuleModel(moduleNode: DataNode<ModuleData>) =
        ExternalSystemApiUtil.getChildren(moduleNode, AndroidProjectKeys.ANDROID_MODEL).firstOrNull()?.data

    private fun getJavaModuleModel(moduleNode: DataNode<ModuleData>) =
        ExternalSystemApiUtil.getChildren(moduleNode, AndroidProjectKeys.JAVA_MODULE_MODEL).firstOrNull()?.data

    private fun addSourceRoot(
        sourceRoot: File,
        type: JpsModuleSourceRootType<*>,
        rootModel: ModifiableRootModel,
        shouldCreateEmptySourceRoots: Boolean
    ) {
        val parent = findParentContentEntry(sourceRoot, rootModel.contentEntries.stream()) ?: return
        val url = FilePaths.pathToIdeaUrl(sourceRoot)
        parent.addSourceFolder(url, type)
        if (shouldCreateEmptySourceRoots) {
            ExternalSystemApiUtil.doWriteAction {
                try {
                    VfsUtil.createDirectoryIfMissing(sourceRoot.path)
                } catch (e: IOException) {
                    LOG.warn(String.format("Unable to create directory for the path: %s", sourceRoot.path), e)
                }
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinAndroidGradleMPPModuleDataService::class.java)
    }
}