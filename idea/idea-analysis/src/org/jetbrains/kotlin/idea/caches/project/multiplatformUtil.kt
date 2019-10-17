/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.KotlinMultiplatformVersion
import org.jetbrains.kotlin.config.isHmpp
import org.jetbrains.kotlin.config.isNewMPP
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.SourceType.PRODUCTION
import org.jetbrains.kotlin.idea.caches.project.SourceType.TEST
import org.jetbrains.kotlin.idea.core.isAndroidModule
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.facet.KotlinFacetType.Companion.ID
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon

val Module.isNewMPPModule: Boolean
    get() = facetSettings?.mppVersion.isNewMPP ||
            facetSettings?.mppVersion.isHmpp // TODO: review clients, correct them to use precise checks for MPP version

val Module.externalProjectId: String
    get() = facetSettings?.externalProjectId ?: ""

val Module.sourceType: SourceType?
    get() = facetSettings?.isTestModule?.let { isTest -> if (isTest) TEST else PRODUCTION }

val Module.isMPPModule: Boolean
    get() = facetSettings?.isMPPModule ?: false

val Module.isTestModule: Boolean
    get() = facetSettings?.isTestModule ?: false

val KotlinFacetSettings.isMPPModule: Boolean
    get() = this.mppVersion != null

private val Module.facetSettings get() = KotlinFacet.get(this)?.configuration?.settings

val Module.implementingModules: List<Module>
    get() = cacheInvalidatingOnRootModifications {
        val moduleManager = ModuleManager.getInstance(project)

        when (facetSettings?.mppVersion) {
            null -> emptyList()

            KotlinMultiplatformVersion.M3 -> {
                val thisModuleStableName = stableModuleName

                moduleManager.modules.filter { it.facetSettings?.dependsOnModuleNames?.contains(thisModuleStableName) == true }
            }

            KotlinMultiplatformVersion.M2 -> moduleManager.getModuleDependentModules(this).filter {
                it.isNewMPPModule && it.externalProjectId == externalProjectId
            }

            KotlinMultiplatformVersion.M1 -> moduleManager.modules.filter { name in it.findOldFashionedImplementedModuleNames() }
        }
    }

private val Module.stableModuleName: String
    get() = ExternalSystemModulePropertyManager.getInstance(this).getLinkedProjectId()
        ?: name.also {
            if (!ApplicationManager.getApplication().isUnitTestMode) LOG.error("Don't have a LinkedProjectId for module $this for HMPP!")
        }

private val Project.modulesByLinkedKey: Map<String, Module>
    get() = cacheInvalidatingOnRootModifications {
        val moduleManager = ModuleManager.getInstance(this)

        moduleManager.modules.associateBy { it.stableModuleName }
    }

val Module.implementedModules: List<Module>
    get() = cacheInvalidatingOnRootModifications {
        val facetSettings = facetSettings
        when (facetSettings?.mppVersion) {
            null -> emptyList()

            KotlinMultiplatformVersion.M3 -> {
                facetSettings.dependsOnModuleNames.mapNotNull {
                    project.modulesByLinkedKey[it]
                }
            }

            KotlinMultiplatformVersion.M2 -> {
                rootManager.dependencies.filter {
                    // TODO: remove additional android check
                    it.isNewMPPModule && it.platform.isCommon() && it.externalProjectId == externalProjectId && (isAndroidModule() || it.isTestModule == isTestModule)
                }
            }

            KotlinMultiplatformVersion.M1 -> {
                val modelsProvider = IdeModelsProviderImpl(project)
                findOldFashionedImplementedModuleNames().mapNotNull { modelsProvider.findIdeModule(it) }
            }
        }
    }

private fun Module.findOldFashionedImplementedModuleNames(): List<String> {
    val facet = FacetManager.getInstance(this).findFacet(
        KotlinFacetType.TYPE_ID,
        FacetTypeRegistry.getInstance().findFacetType(ID)!!.defaultFacetName
    )
    return facet?.configuration?.settings?.implementedModuleNames ?: emptyList()
}


val ModuleDescriptor.implementingDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        if (moduleInfo is PlatformModuleInfo) {
            return listOf(this)
        }
        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()
        val implementingModuleInfos = moduleSourceInfo.module.implementingModules.mapNotNull { it.toInfo(moduleSourceInfo.sourceType) }
        return implementingModuleInfos.mapNotNull { it.toDescriptor() }
    }

fun Module.toInfo(type: SourceType): ModuleSourceInfo? = when (type) {
    PRODUCTION -> productionSourceInfo()
    TEST -> testSourceInfo()
}


/**
 * This function returns immediate parents in dependsOn graph
 */
val ModuleDescriptor.implementedDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        if (moduleInfo is PlatformModuleInfo) return listOf(this)

        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()

        return moduleSourceInfo.expectedBy.mapNotNull { it.toDescriptor() }
    }

fun Module.toDescriptor() = (productionSourceInfo() ?: testSourceInfo())?.toDescriptor()

fun ModuleSourceInfo.toDescriptor() = KotlinCacheService.getInstance(module.project)
    .getResolutionFacadeByModuleInfo(this, platform)?.moduleDescriptor

fun PsiElement.getPlatformModuleInfo(desiredPlatform: TargetPlatform): PlatformModuleInfo? {
    assert(!desiredPlatform.isCommon()) { "Platform module cannot have Common platform" }
    val moduleInfo = getNullableModuleInfo() as? ModuleSourceInfo ?: return null
    val platform = moduleInfo.platform
    return when {
        platform.isCommon() -> {
            val correspondingImplementingModule = moduleInfo.module.implementingModules.map { it.toInfo(moduleInfo.sourceType) }
                .firstOrNull { it?.platform == desiredPlatform } ?: return null
            PlatformModuleInfo(correspondingImplementingModule, correspondingImplementingModule.expectedBy)
        }
        platform == desiredPlatform -> {
            val expectedBy = moduleInfo.expectedBy.takeIf { it.isNotEmpty() } ?: return null
            PlatformModuleInfo(moduleInfo, expectedBy)
        }
        else -> null
    }
}
