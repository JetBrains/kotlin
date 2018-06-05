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
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.artifacts.Dependency
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.CompilerArgumentsBySourceSet
import org.jetbrains.kotlin.gradle.KotlinGradleModel
import org.jetbrains.kotlin.gradle.KotlinGradleModelBuilder
import org.jetbrains.kotlin.idea.inspections.gradle.getDependencyModules
import org.jetbrains.kotlin.idea.util.CopyableDataNodeUserDataProperty
import org.jetbrains.kotlin.idea.util.NotNullableCopyableDataNodeUserDataProperty
import org.jetbrains.plugins.gradle.model.ExternalProjectDependency
import org.jetbrains.plugins.gradle.model.FileCollectionDependency
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import java.io.File
import java.util.*

var DataNode<ModuleData>.isResolved
        by NotNullableCopyableDataNodeUserDataProperty(Key.create<Boolean>("IS_RESOLVED"), false)
var DataNode<ModuleData>.hasKotlinPlugin
        by NotNullableCopyableDataNodeUserDataProperty(Key.create<Boolean>("HAS_KOTLIN_PLUGIN"), false)
var DataNode<ModuleData>.compilerArgumentsBySourceSet
        by CopyableDataNodeUserDataProperty(Key.create<CompilerArgumentsBySourceSet>("CURRENT_COMPILER_ARGUMENTS"))
var DataNode<ModuleData>.coroutines
        by CopyableDataNodeUserDataProperty(Key.create<String>("KOTLIN_COROUTINES"))
var DataNode<ModuleData>.platformPluginId
        by CopyableDataNodeUserDataProperty(Key.create<String>("PLATFORM_PLUGIN_ID"))
var DataNode<out ModuleData>.implementedModuleNames
        by NotNullableCopyableDataNodeUserDataProperty(Key.create<List<String>>("IMPLEMENTED_MODULE_NAME"), emptyList())

class KotlinGradleProjectResolverExtension : AbstractProjectResolverExtension() {
    val isAndroidProjectKey = Key.findKeyByName("IS_ANDROID_PROJECT_KEY")

    override fun getToolingExtensionsClasses(): Set<Class<out Any>> {
        return setOf(KotlinGradleModelBuilder::class.java, Unit::class.java)
    }

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinGradleModel::class.java)
    }

    private fun useModulePerSourceSet(): Boolean {
        // See AndroidGradleProjectResolver
        if (isAndroidProjectKey != null && resolverCtx.getUserData(isAndroidProjectKey) == true) {
            return false
        }
        return resolverCtx.isResolveModulePerSourceSet
    }

    override fun populateModuleDependencies(
        gradleModule: IdeaModule,
        ideModule: DataNode<ModuleData>,
        ideProject: DataNode<ProjectData>
    ) {
        val outputToSourceSet = ideProject.getUserData(GradleProjectResolver.MODULES_OUTPUTS)
        val sourceSetByName = ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS)

        val gradleModel = resolverCtx.getExtraProject(gradleModule, KotlinGradleModel::class.java)
                ?: return super.populateModuleDependencies(gradleModule, ideModule, ideProject)

        val gradleIdeaProject = gradleModule.project

        fun getDependencyByFiles(files: Collection<File>) = files
            .mapTo(HashSet()) {
                val path = FileUtil.toSystemIndependentName(it.path)
                val targetSourceSetId = outputToSourceSet?.get(path)?.first ?: return@mapTo null
                sourceSetByName?.get(targetSourceSetId)?.first
            }
            .singleOrNull()

        fun DataNode<out ModuleData>.getDependencies(): Collection<DataNode<out ModuleData>> {
            if (useModulePerSourceSet()) {
                if (sourceSetByName == null) return emptySet()
                val externalSourceSet = sourceSetByName[data.id]?.second ?: return emptySet()
                return externalSourceSet.dependencies.mapNotNullTo(LinkedHashSet()) { dependency ->
                    when (dependency) {
                        is ExternalProjectDependency -> {
                            if (dependency.configurationName == Dependency.DEFAULT_CONFIGURATION) {
                                val targetModuleNode = ExternalSystemApiUtil.findFirstRecursively(ideProject) {
                                    (it.data as? ModuleData)?.id == dependency.projectPath
                                } as DataNode<ModuleData>? ?: return@mapNotNullTo null
                                ExternalSystemApiUtil.findAll(targetModuleNode, GradleSourceSetData.KEY)
                                    .firstOrNull { it.sourceSetName == "main" }
                            } else {
                                getDependencyByFiles(dependency.projectDependencyArtifacts)
                            }
                        }
                        is FileCollectionDependency -> {
                            getDependencyByFiles(dependency.files)
                        }
                        else -> null
                    }
                }
            }

            return getDependencyModules(ideModule, gradleIdeaProject)
        }

        fun addTransitiveDependenciesOnImplementedModules() {
            val moduleNodesToProcess = if (useModulePerSourceSet()) {
                ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)
            } else listOf(ideModule)

            for (currentModuleNode in moduleNodesToProcess) {
                val toProcess = LinkedList<DataNode<out ModuleData>>()
                val processed = HashSet<DataNode<out ModuleData>>()
                toProcess.add(currentModuleNode)

                while (toProcess.isNotEmpty()) {
                    val moduleNode = toProcess.pollFirst()
                    processed.add(moduleNode)

                    val moduleNodeForGradleModel = if (useModulePerSourceSet()) {
                        ExternalSystemApiUtil.findParent(moduleNode, ProjectKeys.MODULE)
                    } else moduleNode
                    val ideaModule = if (moduleNodeForGradleModel != ideModule) {
                        gradleIdeaProject.modules.firstOrNull { it.gradleProject.path == moduleNodeForGradleModel?.data?.id }
                    } else gradleModule
                    val implementsModuleIds = resolverCtx.getExtraProject(ideaModule, KotlinGradleModel::class.java)?.implements
                            ?: emptyList()

                    for (implementsModuleId in implementsModuleIds) {
                        val targetModule = findModuleById(ideProject, gradleModule, implementsModuleId) ?: continue

                        if (useModulePerSourceSet()) {
                            val targetSourceSetsByName = ExternalSystemApiUtil
                                .findAll(targetModule, GradleSourceSetData.KEY)
                                .associateBy { it.sourceSetName }
                            val targetMainSourceSet = targetSourceSetsByName["main"] ?: targetModule
                            val targetSourceSet = targetSourceSetsByName[currentModuleNode.sourceSetName]
                            if (targetSourceSet != null) {
                                addDependency(currentModuleNode, targetSourceSet)
                            }
                            if (currentModuleNode.sourceSetName == "test" && targetMainSourceSet != targetSourceSet) {
                                addDependency(currentModuleNode, targetMainSourceSet)
                            }
                        } else {
                            addDependency(currentModuleNode, targetModule)
                        }
                    }

                    moduleNode.getDependencies().filterTo(toProcess) { it !in processed }
                }
            }
        }

        if (!useModulePerSourceSet()) {
            super.populateModuleDependencies(gradleModule, ideModule, ideProject)
        }

        addTransitiveDependenciesOnImplementedModules()

        ideModule.isResolved = true
        ideModule.hasKotlinPlugin = gradleModel.hasKotlinPlugin
        ideModule.compilerArgumentsBySourceSet = gradleModel.compilerArgumentsBySourceSet
        ideModule.coroutines = gradleModel.coroutines
        ideModule.platformPluginId = gradleModel.platformPluginId
        addImplementedModuleNames(gradleModule, ideModule, ideProject, gradleModel)

        if (useModulePerSourceSet()) {
            super.populateModuleDependencies(gradleModule, ideModule, ideProject)
        }
    }

    private fun addImplementedModuleNames(
        gradleModule: IdeaModule,
        dependentModule: DataNode<ModuleData>,
        ideProject: DataNode<ProjectData>,
        gradleModel: KotlinGradleModel
    ) {
        val implementedModules = gradleModel.implements.mapNotNull { findModuleById(ideProject, gradleModule, it) }
        if (useModulePerSourceSet()) {
            val dependentSourceSets = dependentModule.getSourceSetsMap()
            val implementedSourceSetMaps = implementedModules.map { it.getSourceSetsMap() }
            for ((sourceSetName, dependentSourceSet) in dependentSourceSets) {
                dependentSourceSet.implementedModuleNames = implementedSourceSetMaps.mapNotNull { it[sourceSetName]?.data?.internalName }
            }
        } else {
            dependentModule.implementedModuleNames = implementedModules.map { it.data.internalName }
        }
    }

    private fun DataNode<ModuleData>.getSourceSetsMap() =
        ExternalSystemApiUtil.getChildren(this, GradleSourceSetData.KEY).associateBy { it.sourceSetName }

    private val DataNode<out ModuleData>.sourceSetName
        get() = (data as? GradleSourceSetData)?.id?.substringAfterLast(':')

    private fun addDependency(ideModule: DataNode<out ModuleData>, targetModule: DataNode<out ModuleData>) {
        val moduleDependencyData = ModuleDependencyData(ideModule.data, targetModule.data)
        moduleDependencyData.scope = DependencyScope.COMPILE
        moduleDependencyData.isExported = false
        ideModule.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
    }

    private fun findModuleById(ideProject: DataNode<ProjectData>, gradleModule: IdeaModule, moduleId: String): DataNode<ModuleData>? {
        val isCompositeProject = resolverCtx.models.ideaProject != gradleModule.project
        val compositePrefix =
            if (isCompositeProject && moduleId.startsWith(":")) gradleModule.project.name
            else ""

        val fullModuleId = compositePrefix + moduleId

        return ideProject.children.find { (it.data as? ModuleData)?.id == fullModuleId } as DataNode<ModuleData>?
    }
}
