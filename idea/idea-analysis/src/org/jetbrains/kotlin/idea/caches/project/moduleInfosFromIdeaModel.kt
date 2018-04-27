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
import org.jetbrains.kotlin.resolve.TargetPlatform

fun collectAllModuleInfosFromIdeaModel(project: Project, platform: TargetPlatform): List<IdeaModuleInfo> {
    val ideaModules = ModuleManager.getInstance(project).modules.toList()
    val modulesSourcesInfos = ideaModules.flatMap(Module::correspondingModuleInfos)

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<LibraryOrderEntry>().map {
            it.library
        }
    }.filterNotNull().toSet()

    val librariesInfos = ideaLibraries.map { LibraryInfo(project, it) }

    val sdksFromModulesDependencies = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<JdkOrderEntry>().map {
            it.jdk
        }
    }

    val sdksInfos = (sdksFromModulesDependencies + getAllProjectSdks()).filterNotNull().toSet().map {
        SdkInfo(
            project,
            it
        )
    }

    return mergePlatformModules(modulesSourcesInfos, platform) + librariesInfos + sdksInfos
}

private fun mergePlatformModules(
    allModules: List<IdeaModuleInfo>,
    platform: TargetPlatform
): List<IdeaModuleInfo> {
    if (platform == TargetPlatform.Common) return allModules

    val platformModules =
        allModules.flatMap { module ->
            if (module is ModuleSourceInfo && module.platform == platform && module.expectedBy.isNotEmpty())
                listOf(module to module.expectedBy)
            else emptyList()
        }.map { (module, expectedBys) ->
            PlatformModuleInfo(module, expectedBys)
        }

    val rest = allModules - platformModules.flatMap { it.containedModules }
    return rest + platformModules
}

internal fun getAllProjectSdks(): Collection<Sdk> {
    return ProjectJdkTable.getInstance().allJdks.toList()
}