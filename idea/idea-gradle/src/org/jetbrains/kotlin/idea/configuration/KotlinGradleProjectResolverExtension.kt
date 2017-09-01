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
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.CompilerArgumentsBySourceSet
import org.jetbrains.kotlin.gradle.KotlinGradleModel
import org.jetbrains.kotlin.gradle.KotlinGradleModelBuilder
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.model.ExternalProject
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getModuleId

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
        val gradleModel = resolverCtx.getExtraProject(gradleModule, KotlinGradleModel::class.java)
                          ?: return super.populateModuleDependencies(gradleModule, ideModule, ideProject)

        importTransitiveCommonDependencies(gradleModel, ideProject, gradleModule, ideModule)

        ideModule.isResolved = true
        ideModule.hasKotlinPlugin = gradleModel.hasKotlinPlugin
        ideModule.compilerArgumentsBySourceSet = gradleModel.compilerArgumentsBySourceSet
        ideModule.coroutines = gradleModel.coroutines
        ideModule.platformPluginId = gradleModel.platformPluginId

        super.populateModuleDependencies(gradleModule, ideModule, ideProject)
    }

    private fun importTransitiveCommonDependencies(gradleModel: KotlinGradleModel, ideProject: DataNode<ProjectData>, gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        gradleModel.transitiveCommonDependencies.forEach { implementsModuleId ->
            val targetModule = findModule(ideProject, implementsModuleId) ?: return
            if (resolverCtx.isResolveModulePerSourceSet) {
                populateSourceSetDependencies(gradleModule, ideModule, targetModule)
            }
            else {
                addDependency(ideModule, targetModule)
            }
        }
    }

    private fun addDependency(ideModule: DataNode<out ModuleData>, targetModule: DataNode<out ModuleData>) {
        val moduleDependencyData = ModuleDependencyData(ideModule.data, targetModule.data)
        moduleDependencyData.scope = DependencyScope.COMPILE
        moduleDependencyData.isExported = false
        ideModule.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
    }

    private fun findModule(ideProject: DataNode<ProjectData>, moduleId: String): DataNode<ModuleData>? {
        return ideProject.children.find { (it.data as? ModuleData)?.id == moduleId } as DataNode<ModuleData>?
    }

    private fun populateSourceSetDependencies(gradleModule: IdeaModule,
                                              fromModule: DataNode<ModuleData>,
                                              targetModule: DataNode<ModuleData>) {
        val fromSourceSets = ExternalSystemApiUtil.findAll(fromModule, GradleSourceSetData.KEY)
                .associateBy { it.data.id }
        val targetSourceSets = ExternalSystemApiUtil.findAll(targetModule, GradleSourceSetData.KEY)
                .associateBy { it.data.id.substringAfterLast(':') }

        val externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java) ?: return
        for (sourceSet in externalProject.sourceSets.values) {
            if (sourceSet == null || sourceSet.sources.isEmpty()) continue

            val moduleId = getModuleId(externalProject, sourceSet)
            val fromModuleDataNode = (if (fromSourceSets.isEmpty()) fromModule else fromSourceSets[moduleId]) ?: continue
            val targetModuleDataNode = (if (targetSourceSets.isEmpty()) targetModule else targetSourceSets[sourceSet.name]) ?: continue
            addDependency(fromModuleDataNode, targetModuleDataNode)
        }
    }
}
