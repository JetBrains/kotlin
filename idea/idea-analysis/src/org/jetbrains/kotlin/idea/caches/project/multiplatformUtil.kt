/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.facet.KotlinFacetType.Companion.ID

fun Module.findImplementingModules() =
    ModuleManager.getInstance(project).modules.filter { name in it.findImplementedModuleNames() }

val Module.implementingModules: List<Module>
    get() = cached(CachedValueProvider {
        CachedValueProvider.Result(
                findImplementingModules(),
                ProjectRootModificationTracker.getInstance(project)
        )
    })

private fun Module.getModuleInfo(baseModuleSourceInfo: ModuleSourceInfo): ModuleSourceInfo? =
    when (baseModuleSourceInfo) {
        is ModuleProductionSourceInfo -> productionSourceInfo()
        is ModuleTestSourceInfo -> testSourceInfo()
        else -> null
    }

private fun Module.findImplementingModuleInfos(moduleSourceInfo: ModuleSourceInfo) =
    findImplementingModules().mapNotNull { it.getModuleInfo(moduleSourceInfo) }

val ModuleDescriptor.implementingDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        if (moduleInfo is PlatformModuleInfo) {
            return listOf(this)
        }
        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()
        val module = moduleSourceInfo.module
        return module.cached(CachedValueProvider {
            val implementingModuleInfos = module.findImplementingModuleInfos(moduleSourceInfo)
            val implementingModuleDescriptors = implementingModuleInfos.mapNotNull {
                KotlinCacheService.getInstance(module.project).getResolutionFacadeByModuleInfo(it, it.platform)?.moduleDescriptor
            }
            CachedValueProvider.Result(
                implementingModuleDescriptors,
                *(implementingModuleInfos.map { it.createModificationTracker() } +
                        ProjectRootModificationTracker.getInstance(module.project)).toTypedArray()
            )
        })
    }

val ModuleDescriptor.implementedDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        if (moduleInfo is PlatformModuleInfo) return listOf(this)

        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()

        return moduleSourceInfo.expectedBy.mapNotNull {
            KotlinCacheService.getInstance(moduleSourceInfo.module.project)
                .getResolutionFacadeByModuleInfo(it, it.platform)?.moduleDescriptor
        }
    }

fun Module.findImplementedModuleNames(): List<String> {
    val facet = FacetManager.getInstance(this).findFacet(
        KotlinFacetType.TYPE_ID,
        FacetTypeRegistry.getInstance().findFacetType(ID)!!.defaultFacetName
    )
    return facet?.configuration?.settings?.implementedModuleNames ?: emptyList()
}

fun Module.findImplementedModules() = this.cached<List<Module>>(
    CachedValueProvider {
        val modelsProvider = IdeModelsProviderImpl(project)
        CachedValueProvider.Result(
            findImplementedModuleNames().mapNotNull { modelsProvider.findIdeModule(it) },
            ProjectRootModificationTracker.getInstance(project)
        )
    }
)
