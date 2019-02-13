/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.analyzer.common.CommonPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import java.util.concurrent.ConcurrentHashMap

fun getModuleInfosFromIdeaModel(project: Project, platform: TargetPlatform): List<IdeaModuleInfo> {
    val modelInfosCache = project.cached(CachedValueProvider {
        CachedValueProvider.Result(collectModuleInfosFromIdeaModel(project), ProjectRootModificationTracker.getInstance(project))
    })
    return modelInfosCache.forPlatform(platform)
}

private class IdeaModelInfosCache(
    val moduleSourceInfos: List<ModuleSourceInfo>,
    val libraryInfos: List<LibraryInfo>,
    val sdkInfos: List<SdkInfo>
) {
    private val resultByPlatform = ConcurrentHashMap<TargetPlatform, List<IdeaModuleInfo>>()

    fun forPlatform(platform: TargetPlatform): List<IdeaModuleInfo> {
        return resultByPlatform.getOrPut(platform) {
            /** mergePlatformModules is not available anymore. Instead, we properly filter all modules by their platform
             * TODO:
             * - adjust module's platform (remember that it's not only one platform now, so it shouldn't be an equality)
             * - think about libraryInfos and sdkInfos, they are probably should be supplied by build system too
             */
//            mergePlatformModules(moduleSourceInfos, platform) + libraryInfos + sdkInfos
            moduleSourceInfos.filter { it.platform == platform } + libraryInfos + sdkInfos
        }
    }
}


private fun collectModuleInfosFromIdeaModel(
    project: Project
): IdeaModelInfosCache {
    val ideaModules = ModuleManager.getInstance(project).modules.toList()

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<LibraryOrderEntry>().map {
            it.library
        }
    }.filterNotNull().toSet()

    val sdksFromModulesDependencies = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<JdkOrderEntry>().map {
            it.jdk
        }
    }

    return IdeaModelInfosCache(
        moduleSourceInfos = ideaModules.flatMap(Module::correspondingModuleInfos),
        libraryInfos = ideaLibraries.flatMap { createLibraryInfo(project, it) },
        sdkInfos = (sdksFromModulesDependencies + getAllProjectSdks()).filterNotNull().toSet().map { SdkInfo(project, it) }
    )
}

/** This function used to:
 * a) Introduce PlatformModuleInfo, on which other hacks heavily rely (see how we essentially re-wrap ModuleInfo + expectedBy-list
 *    into PlatformModuleInfo)
 * b) Remove common modules from platform resolvers (see how we remove from allModules all contained modules (this will include
 *    expectedBy's)
 *
 * Now, common modules should be properly resolved by common resolver (remember that "common" resolver now actually is a more complex
 * thing, essentially it is a "MixedPlatform", not just "Common")
 *
 * Visibility of common symbols from platform modules should be also added.
 */
//private fun mergePlatformModules(
//    allModules: List<ModuleSourceInfo>,
//    platform: TargetPlatform
//): List<IdeaModuleInfo> {
//    if (platform is CommonPlatform) return allModules
//
//    val platformModules =
//        allModules.flatMap { module ->
//            if (module.platform == platform && module.expectedBy.isNotEmpty())
//                listOf(module to module.expectedBy)
//            else emptyList()
//        }.map { (module, expectedBys) ->
//            PlatformModuleInfo(module, expectedBys)
//        }
//
//    val rest = allModules - platformModules.flatMap { it.containedModules }
//    return rest + platformModules
//}

internal fun getAllProjectSdks(): Collection<Sdk> {
    return ProjectJdkTable.getInstance().allJdks.toList()
}