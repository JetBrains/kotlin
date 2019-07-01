/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.execution

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.getExternalProjectInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.PathUtilRt
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.idea.caches.project.isMPPModule
import org.jetbrains.kotlin.idea.caches.project.isNewMPPModule
import org.jetbrains.kotlin.idea.configuration.KotlinOutputPathsData
import org.jetbrains.plugins.gradle.execution.GradleOrderEnumeratorHandler
import org.jetbrains.plugins.gradle.model.ExternalProject
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants

open class KotlinGradleOrderEnumerationHandler(module: Module) : GradleOrderEnumeratorHandler(module) {
    private companion object {
        private val PRODUCTION_SOURCE_TYPES = listOf(
            ExternalSystemSourceType.SOURCE,
            ExternalSystemSourceType.RESOURCE,
            ExternalSystemSourceType.SOURCE_GENERATED
        )

        private val TEST_SOURCE_TYPES = listOf(
            ExternalSystemSourceType.TEST,
            ExternalSystemSourceType.TEST_RESOURCE,
            ExternalSystemSourceType.TEST_GENERATED
        )
    }

    override fun shouldIncludeTestsFromDependentModulesToTestClasspath(): Boolean {
        return true
    }

    override fun addCustomModuleRoots(
        type: OrderRootType,
        rootModel: ModuleRootModel,
        result: MutableCollection<String>,
        includeProduction: Boolean,
        includeTests: Boolean
    ): Boolean {
        if (super.addCustomModuleRoots(type, rootModel, result, includeProduction, includeTests)) {
            // The code below works as a fallback when the default implementation fails
            return true
        }

        val module = rootModel.module.takeIf { it.isNewMPPModule } ?: return false
        val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return false
        val externalProjectInfo = getExternalProjectInfo(module.project, GradleConstants.SYSTEM_ID, projectPath) ?: return false
        val moduleData = GradleProjectResolverUtil.findModule(externalProjectInfo.externalProjectStructure, projectPath) ?: return false

        val sourceSetData = ExternalSystemApiUtil.getChildren(moduleData, GradleSourceSetData.KEY)
            .filter { it.data.internalName == module.name }

        if (sourceSetData.isEmpty()) {
            return false
        }

        val existingRoots = result.toMutableSet()
        var hasNewRoots = false

        fun appendPaths(paths: Collection<String>) {
            for (path in paths) {
                val url = VfsUtilCore.pathToUrl(path)
                if (existingRoots.add(url)) {
                    result.add(url)
                    if (!hasNewRoots) {
                        hasNewRoots = true
                    }
                }
            }
        }

        fun appendPaths(outputPaths: KotlinOutputPathsData, configurations: List<ExternalSystemSourceType>) {
            configurations.forEach { appendPaths(outputPaths.paths[it]) }
        }

        for (data in sourceSetData) {
            for (outputPaths in ExternalSystemApiUtil.findAll(data, KotlinOutputPathsData.KEY)) {
                if (includeProduction) {
                    appendPaths(outputPaths.data, PRODUCTION_SOURCE_TYPES)
                }
                if (includeTests) {
                    appendPaths(outputPaths.data, TEST_SOURCE_TYPES)
                }
            }
        }

        return hasNewRoots
    }

    open class Factory : GradleOrderEnumeratorHandler.FactoryImpl() {
        override fun isApplicable(module: Module): Boolean {
            return module.isMPPModule
        }

        override fun createHandler(module: Module) = KotlinGradleOrderEnumerationHandler(module)
    }
}
