/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.project.cacheByClassInvalidatingOnRootModifications
import org.jetbrains.kotlin.idea.caches.project.LibraryDependenciesCache
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfosFromIdeaModel
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled
import org.jetbrains.kotlin.types.typeUtil.closure
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface ResolutionAnchorCacheService {
    val resolutionAnchorsForLibraries: Map<LibraryInfo, ModuleSourceInfo>

    fun getDependencyResolutionAnchors(libraryInfo: LibraryInfo): Set<ModuleSourceInfo>

    companion object {
        val Default = object : ResolutionAnchorCacheService {
            override val resolutionAnchorsForLibraries: Map<LibraryInfo, ModuleSourceInfo>
                get() = emptyMap()

            override fun getDependencyResolutionAnchors(libraryInfo: LibraryInfo): Set<ModuleSourceInfo> = emptySet()
        }

        fun getInstance(project: Project): ResolutionAnchorCacheService =
            ServiceManager.getService(project, ResolutionAnchorCacheService::class.java) ?: Default
    }
}

@State(name = "KotlinIdeAnchorService", storages = [Storage("anchors.xml")])
class ResolutionAnchorCacheServiceImpl(val project: Project) :
    ResolutionAnchorCacheService,
    PersistentStateComponent<ResolutionAnchorCacheServiceImpl.State> {

    data class State(
        var moduleNameToAnchorName: Map<String, String> = emptyMap()
    )

    private val logger = logger<ResolutionAnchorCacheServiceImpl>()

    @JvmField
    @Volatile
    var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    object ResolutionAnchorMappingCacheKey
    object ResolutionAnchorDependenciesCacheKey

    override val resolutionAnchorsForLibraries: Map<LibraryInfo, ModuleSourceInfo> by lazy {
        project.cacheByClassInvalidatingOnRootModifications(ResolutionAnchorMappingCacheKey::class.java) {
            mapResolutionAnchorForLibraries()
        }
    }

    private val resolutionAnchorDependenciesCache: MutableMap<LibraryInfo, Set<ModuleSourceInfo>> =
        project.cacheByClassInvalidatingOnRootModifications(ResolutionAnchorDependenciesCacheKey::class.java) {
            ContainerUtil.createConcurrentWeakMap()
        }

    override fun getDependencyResolutionAnchors(libraryInfo: LibraryInfo): Set<ModuleSourceInfo> {
        return resolutionAnchorDependenciesCache.getOrPut(libraryInfo) {
            val allTransitiveLibraryDependencies = with(LibraryDependenciesCache.getInstance(project)) {
                val (directDependenciesOnLibraries, _) = getLibrariesAndSdksUsedWith(libraryInfo)
                directDependenciesOnLibraries.closure { libraryDependency ->
                    checkCanceled()
                    getLibrariesAndSdksUsedWith(libraryDependency).first
                }
            }
            allTransitiveLibraryDependencies.mapNotNull { resolutionAnchorsForLibraries[it] }.toSet()
        }
    }

    private fun associateModulesByNames(): Map<String, ModuleInfo> {
        return getModuleInfosFromIdeaModel(project).associateBy { moduleInfo ->
            checkCanceled()
            when (moduleInfo) {
                is LibraryInfo -> moduleInfo.library.name ?: "" // TODO: when does library have no name?
                is ModuleSourceInfo -> moduleInfo.module.name
                else -> moduleInfo.name.asString()
            }
        }
    }

    private fun mapResolutionAnchorForLibraries(): Map<LibraryInfo, ModuleSourceInfo> {
        val modulesByNames: Map<String, ModuleInfo> = associateModulesByNames()

        return myState.moduleNameToAnchorName.entries.mapNotNull { (libraryName, anchorName) ->
            val library: LibraryInfo = modulesByNames[libraryName]?.takeIf { it is LibraryInfo }?.cast()
                ?: run {
                    logger.warn("Resolution anchor mapping key doesn't point to a known library: $libraryName. Skipping this anchor")
                    return@mapNotNull null
                }
            val anchor: ModuleSourceInfo = modulesByNames[anchorName]?.takeIf { it is ModuleSourceInfo }?.cast()
                ?: run {
                    logger.warn("Resolution anchor mapping value doesn't point to a source module: $anchorName. Skipping this anchor")
                    return@mapNotNull null
                }

            library to anchor
        }.toMap()
    }
}
