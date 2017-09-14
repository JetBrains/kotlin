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

import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.OrderEnumerationHandler
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.task.*
import com.intellij.task.impl.ModuleBuildTaskImpl
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.targetPlatform
import org.jetbrains.kotlin.idea.util.rootManager
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

    override fun run(
        project: Project,
        context: ProjectTaskContext,
        callback: ProjectTaskNotification?,
        tasks: Collection<ProjectTask>
    ) {
        val configuration = context.runConfiguration
        if (configuration is ModuleBasedConfiguration<*> &&
            (configuration.configurationModule is JavaRunConfigurationModule || configuration is JetRunConfiguration)) {

            val module = configuration.configurationModule.module
            if (module?.targetPlatform == TargetPlatformKind.Common) {
                val implModule = module.findJvmImplementationModule()
                if (implModule != null) {
                    val replacedTasks = tasks.map { it.replaceModule(module, implModule) }
                    super.run(project, context, callback, replacedTasks)
                    return
                }
            }
        }

        super.run(project, context, callback, tasks)
    }

    private fun ProjectTask.replaceModule(origin: Module, replacement: Module): ProjectTask =
            when (this) {
                is ModuleFilesBuildTask -> this

                is ModuleBuildTask ->
                        if (module == origin)
                            ModuleBuildTaskImpl(replacement, isIncrementalBuild, isIncludeDependentModules, isIncludeRuntimeDependencies)
                        else
                            this

                else -> this
            }
}

class MultiplatformGradleOrderEnumeratorHandler : OrderEnumerationHandler() {
    override fun addCustomModuleRoots(type: OrderRootType, rootModel: ModuleRootModel, result: MutableCollection<String>, includeProduction: Boolean, includeTests: Boolean): Boolean {
        if (type != OrderRootType.CLASSES) return false
        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, rootModel.module)) return false

        if (!GradleSystemRunningSettings.getInstance().isUseGradleAwareMake) {
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
        }

        val implModule = rootModel.module.findJvmImplementationModule()
        implModule
            ?.rootManager
            ?.orderEntries()
            ?.satisfying { orderEntry -> (orderEntry as? ModuleOrderEntry)?.module != rootModel.module }
            ?.compileOnly()
            ?.classesRoots
            ?.mapTo(result) { it.url }

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
