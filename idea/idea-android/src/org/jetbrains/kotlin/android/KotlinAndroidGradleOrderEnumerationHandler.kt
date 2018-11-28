/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.OrderEnumerationHandler
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.idea.caches.project.isMPPModule
import org.jetbrains.kotlin.idea.core.isAndroidModule
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.plugins.gradle.model.ExternalSourceDirectorySet
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/*
   Partially copied from org.jetbrains.plugins.gradle.execution.GradleOrderEnumeratorHandler.

   Unfortunately, GradleOrderEnumeratorHandler doesn't work for Android Studio as it returns 'false' for
   GradleSystemRunningSettings::isUseGradleAwareMake(), so only resource directories are added.
   We can't specify 'isUseGradleAwareMake()' cause it's a global IDEA preference.

   Everything works for Java-only projects because there's only a single classes directory,
   but Kotlin Gradle plugin adds a separate output directory, and it's not attached by default.
 */
class KotlinAndroidGradleOrderEnumerationHandler(private val module: Module) : OrderEnumerationHandler() {
    override fun addCustomModuleRoots(
        type: OrderRootType,
        rootModel: ModuleRootModel,
        result: MutableCollection<String>,
        includeProduction: Boolean,
        includeTests: Boolean
    ): Boolean {
        if (type != OrderRootType.CLASSES) return false

        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, rootModel.module)) return false
        KotlinFacet.get(rootModel.module) ?: return false

        val module = rootModel.module

        // The current module has to be an ordinary Java module, but the code below is for Android projects
        if (module.isAndroidModule() || ModuleManager.getInstance(module.project).modules.none { it.isAndroidModule() }) {
            return false
        }

        val gradleProjectPath = ExternalSystemModulePropertyManager.getInstance(rootModel.module).getRootProjectPath() ?: return false

        val externalProjectDataCache = ExternalProjectDataCache.getInstance(rootModel.module.project) ?: return false
        val externalRootProject = externalProjectDataCache
            .getRootExternalProject(GradleConstants.SYSTEM_ID, File(gradleProjectPath)) ?: return false

        val externalSourceSets = externalProjectDataCache.findExternalProject(externalRootProject, rootModel.module)
        if (externalSourceSets.isEmpty()) return false

        for (sourceSet in externalSourceSets.values) {
            fun ExternalSourceDirectorySet.addSourceRoots() {
                for (it in gradleOutputDirs) {
                    val path = VfsUtilCore.pathToUrl(it.absolutePath).takeIf { it !in result } ?: continue
                    result.add(path)
                }
            }

            if (includeTests) {
                sourceSet.sources[ExternalSystemSourceType.TEST]?.addSourceRoots()
                sourceSet.sources[ExternalSystemSourceType.TEST_RESOURCE]?.addSourceRoots()
            }
            if (includeProduction) {
                sourceSet.sources[ExternalSystemSourceType.SOURCE]?.addSourceRoots()
                sourceSet.sources[ExternalSystemSourceType.RESOURCE]?.addSourceRoots()
            }
        }

        return true
    }

    class FactoryImpl : OrderEnumerationHandler.Factory() {
        override fun isApplicable(module: Module): Boolean {
            return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module) && !module.isMPPModule
        }

        override fun createHandler(module: Module) = KotlinAndroidGradleOrderEnumerationHandler(module)
    }
}