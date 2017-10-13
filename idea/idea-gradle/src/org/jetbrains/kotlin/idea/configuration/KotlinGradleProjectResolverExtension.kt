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
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaModuleDependency
import org.jetbrains.kotlin.gradle.CompilerArgumentsBySourceSet
import org.jetbrains.kotlin.gradle.KotlinGradleModel
import org.jetbrains.kotlin.gradle.KotlinGradleModelBuilder
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.model.ExternalProjectDependency
import org.jetbrains.plugins.gradle.model.FileCollectionDependency
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import java.util.*

var DataNode<ModuleData>.isResolved
        by NotNullableUserDataProperty(Key.create<Boolean>("IS_RESOLVED"), false)
var DataNode<ModuleData>.hasKotlinPlugin
        by NotNullableUserDataProperty(Key.create<Boolean>("HAS_KOTLIN_PLUGIN"), false)
var DataNode<ModuleData>.compilerArgumentsBySourceSet
        by UserDataProperty(Key.create<CompilerArgumentsBySourceSet>("CURRENT_COMPILER_ARGUMENTS"))
var DataNode<ModuleData>.coroutines
        by UserDataProperty(Key.create<String>("KOTLIN_COROUTINES"))
var DataNode<ModuleData>.platformPluginId
        by UserDataProperty(Key.create<String>("PLATFORM_PLUGIN_ID"))
var DataNode<ModuleData>.implementedModule
        by UserDataProperty(Key.create<DataNode<ModuleData>>("IMPLEMENTS"))

class KotlinGradleProjectResolverExtension : AbstractProjectResolverExtension() {
    override fun getToolingExtensionsClasses(): Set<Class<out Any>> {
        return setOf(KotlinGradleModelBuilder::class.java, Unit::class.java)
    }

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinGradleModel::class.java)
    }

    override fun populateModuleDependencies(gradleModule: IdeaModule,
                                            ideModule: DataNode<ModuleData>,
                                            ideProject: DataNode<ProjectData>) {
        val outputToSourceSet = ideProject.getUserData(GradleProjectResolver.MODULES_OUTPUTS)
        val sourceSetByName = ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS)

        val gradleModel = resolverCtx.getExtraProject(gradleModule, KotlinGradleModel::class.java)
                          ?: return super.populateModuleDependencies(gradleModule, ideModule, ideProject)

        val gradleIdeaProject = gradleModule.project

        fun DataNode<out ModuleData>.getGradleModule() =
                if (this != ideModule) gradleIdeaProject.modules.firstOrNull { it.gradleProject.path == data.id } else gradleModule

        fun DataNode<out ModuleData>.getDependencies(): Set<DataNode<out ModuleData>> {
            if (resolverCtx.isResolveModulePerSourceSet) {
                if (sourceSetByName == null) return emptySet()
                val externalSourceSet = sourceSetByName[data.id]?.second ?: return emptySet()
                return externalSourceSet.dependencies.mapNotNullTo(LinkedHashSet()) { dependency ->
                    when (dependency) {
                        is ExternalProjectDependency -> {
                            val targetModuleNode = ExternalSystemApiUtil.findFirstRecursively(ideProject) {
                                (it.data as? ModuleData)?.id == dependency.projectPath
                            } as DataNode<ModuleData>? ?: return@mapNotNullTo null
                            ExternalSystemApiUtil.findAll(targetModuleNode, GradleSourceSetData.KEY).firstOrNull { it.sourceSetName == "main" }
                        }
                        is FileCollectionDependency -> {
                            dependency.files
                                    .mapTo(HashSet()) {
                                        val path = FileUtil.toSystemIndependentName(it.path)
                                        val targetSourceSetId = outputToSourceSet?.get(path)?.first ?: return@mapTo null
                                        sourceSetByName[targetSourceSetId]?.first
                                    }
                                    .singleOrNull()
                        }
                        else -> null
                    }
                }
            }

            return getGradleModule()?.dependencies?.mapNotNullTo(LinkedHashSet()) {
                val targetModuleName = (it as? IdeaModuleDependency)?.targetModuleName ?: return@mapNotNullTo null
                val targetGradleModule = gradleIdeaProject.modules.firstOrNull { it.name == targetModuleName } ?: return@mapNotNullTo null
                ExternalSystemApiUtil.findFirstRecursively(ideProject) {
                    (it.data as? ModuleData)?.id == targetGradleModule.gradleProject.path
                } as DataNode<ModuleData>?
            } ?: emptySet()
        }

        fun addTransitiveDependenciesOnImplementedModules() {
            val moduleNodesToProcess = if (resolverCtx.isResolveModulePerSourceSet) {
                ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)
            }
            else listOf(ideModule)

            for (currentModuleNode in moduleNodesToProcess) {
                val toProcess = LinkedList<DataNode<out ModuleData>>()
                val processed = HashSet<DataNode<out ModuleData>>()
                toProcess.add(currentModuleNode)

                while (toProcess.isNotEmpty()) {
                    val moduleNode = toProcess.pollFirst()
                    processed.add(moduleNode)

                    val moduleNodeForGradleModel = if (resolverCtx.isResolveModulePerSourceSet) {
                        ExternalSystemApiUtil.findParent(moduleNode, ProjectKeys.MODULE)
                    }
                    else moduleNode
                    val ideaModule = if (moduleNodeForGradleModel != ideModule) {
                        gradleIdeaProject.modules.firstOrNull { it.gradleProject.path == moduleNodeForGradleModel?.data?.id }
                    }
                    else gradleModule

                    val implementsInfo = resolverCtx.getExtraProject(ideaModule, KotlinGradleModel::class.java)?.implements
                    val targetModule = implementsInfo?.let { findModule(ideProject, it) }
                    if (targetModule != null) {
                        if (resolverCtx.isResolveModulePerSourceSet) {
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
                        }
                        else {
                            addDependency(currentModuleNode, targetModule)
                        }
                    }

                    moduleNode.getDependencies().filterTo(toProcess) { it !in processed }
                }
            }
        }

        addTransitiveDependenciesOnImplementedModules()

        ideModule.isResolved = true
        ideModule.hasKotlinPlugin = gradleModel.hasKotlinPlugin
        ideModule.compilerArgumentsBySourceSet = gradleModel.compilerArgumentsBySourceSet
        ideModule.coroutines = gradleModel.coroutines
        ideModule.platformPluginId = gradleModel.platformPluginId
        ideModule.implementedModule = gradleModel.implements?.let { findModule(ideProject, it) }

        super.populateModuleDependencies(gradleModule, ideModule, ideProject)
    }

    private val DataNode<out ModuleData>.sourceSetName
        get() = (data as? GradleSourceSetData)?.id?.substringAfterLast(':')

    private fun addDependency(ideModule: DataNode<out ModuleData>, targetModule: DataNode<out ModuleData>) {
        val moduleDependencyData = ModuleDependencyData(ideModule.data, targetModule.data)
        moduleDependencyData.scope = DependencyScope.COMPILE
        moduleDependencyData.isExported = false
        ideModule.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
    }

    private fun findModule(ideProject: DataNode<ProjectData>, moduleId: String): DataNode<ModuleData>? {
        return ideProject.children.find { (it.data as? ModuleData)?.id == moduleId } as DataNode<ModuleData>?
    }
}
