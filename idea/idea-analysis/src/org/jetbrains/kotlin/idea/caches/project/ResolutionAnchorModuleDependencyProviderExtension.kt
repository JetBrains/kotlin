/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionAnchorCacheService
import org.jetbrains.kotlin.idea.caches.trackers.ModuleDependencyProviderExtension
import org.jetbrains.kotlin.idea.project.libraryToSourceAnalysisEnabled
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled

class ResolutionAnchorModuleDependencyProviderExtension(private val project: Project) : ModuleDependencyProviderExtension {
    /**
     * Consider modules M1, M2, M3, library L1 resolving via Resolution anchor M2, other libraries L2, L3 with the following dependencies:
     * M2 depends on M1
     * L1 depends on anchor M2
     * L2 depends on L1
     * L3 depends on L2
     * M3 depends on L3
     * Then modification of M1 should lead to complete invalidation of all modules and libraries in this example.
     *
     * Updates for libraries aren't managed here, corresponding ModificationTracker is responsible for that.
     * This extension provides missing dependencies from source-dependent library dependencies only to source modules.
     */
    override fun getAdditionalDependencyModules(module: Module): Collection<Module> {
        if (!project.libraryToSourceAnalysisEnabled) return emptySet()

        val resolutionAnchorDependencies = HashSet<ModuleSourceInfo>()
        ModuleRootManager.getInstance(module).orderEntries().recursively().forEachLibrary { library ->
            getIdeaModelInfosCache(project).getLibraryInfosForLibrary(library).flatMapTo(resolutionAnchorDependencies) { libraryInfo ->
                checkCanceled()
                ResolutionAnchorCacheService.getInstance(project).getDependencyResolutionAnchors(libraryInfo)
            }
            true
        }

        val additionalModules = HashSet<Module>()
        for (anchorModule in resolutionAnchorDependencies) {
            ModuleRootManager.getInstance(anchorModule.module).orderEntries().recursively()
                .forEachModule(CommonProcessors.CollectProcessor(additionalModules))
        }
        return additionalModules
    }
}
