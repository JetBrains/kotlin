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

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.OrderEnumerationHandler
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.task.ExecuteRunConfigurationTask
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.plugins.gradle.execution.build.GradleProjectTaskRunner
import org.jetbrains.plugins.gradle.model.ExternalSourceDirectorySet
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/**
 * Ensures that build/run actions are always delegated to Gradle for multiplatform projects.
 */
class MultiplatformGradleProjectTaskRunner : GradleProjectTaskRunner() {

    override fun canRun(projectTask: ProjectTask) =
            when (projectTask) {
                is ModuleBuildTask ->
                    projectTask.module.isMultiplatformModule()

                is ExecuteRunConfigurationTask -> {
                    val runProfile = projectTask.runProfile
                    if (runProfile is ModuleBasedConfiguration<*>) {
                        runProfile.configurationModule.module?.isMultiplatformModule() == true
                    }
                    else {
                        false
                    }
                }


                else -> false
            }
}

class MultiplatformGradleOrderEnumeratorHandler : OrderEnumerationHandler() {
    override fun addCustomModuleRoots(type: OrderRootType, rootModel: ModuleRootModel, result: MutableCollection<String>, includeProduction: Boolean, includeTests: Boolean): Boolean {
        if (type != OrderRootType.CLASSES) return false
        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, rootModel.module)) return false
        if (GradleSystemRunningSettings.getInstance().isUseGradleAwareMake) return false

        val gradleProjectPath = ExternalSystemModulePropertyManager.getInstance(rootModel.module).getRootProjectPath() ?: return false
        val externalProjectDataCache = ExternalProjectDataCache.getInstance(rootModel.module.project)!!
        val externalRootProject = externalProjectDataCache.getRootExternalProject(GradleConstants.SYSTEM_ID,
                                                                                  File(gradleProjectPath)) ?: return false

        val externalSourceSets = externalProjectDataCache.findExternalProject(externalRootProject, rootModel.module)
        if (externalSourceSets.isEmpty()) return false

        for (sourceSet in externalSourceSets.values) {
            if (includeTests) {
                addOutputModuleRoots(sourceSet.sources[ExternalSystemSourceType.TEST], result)
            }
            if (includeProduction) {
                addOutputModuleRoots(sourceSet.sources[ExternalSystemSourceType.SOURCE], result)
            }
        }
        return false
    }

    private fun addOutputModuleRoots(directorySet: ExternalSourceDirectorySet?, result: MutableCollection<String>) {
        directorySet?.gradleOutputDirs?.mapTo(result) { VfsUtilCore.pathToUrl(it.absolutePath) }
    }

    class FactoryImpl : Factory() {
        override fun isApplicable(module: Module): Boolean {
            return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module) &&
                   !GradleSystemRunningSettings.getInstance().isUseGradleAwareMake &&
                   module.isMultiplatformModule()
        }

        override fun createHandler(module: Module): OrderEnumerationHandler =
            MultiplatformGradleOrderEnumeratorHandler()
    }
}

private fun Module.isMultiplatformModule(): Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
