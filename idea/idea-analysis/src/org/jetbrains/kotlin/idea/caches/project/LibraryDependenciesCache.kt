/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Condition
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.configuration.IdeBuiltInsLoadingState
import org.jetbrains.kotlin.idea.core.util.CachedValue
import org.jetbrains.kotlin.idea.core.util.getValue
import org.jetbrains.kotlin.idea.project.isHMPPEnabled
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

internal typealias LibrariesAndSdks = Pair<List<LibraryInfo>, List<SdkInfo>>

interface LibraryDependenciesCache {
    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, LibraryDependenciesCache::class.java)!!
    }

    fun getLibrariesAndSdksUsedWith(libraryInfo: LibraryInfo): LibrariesAndSdks
}

class LibraryDependenciesCacheImpl(private val project: Project) : LibraryDependenciesCache {
    private val cache by CachedValue(project) {
        CachedValueProvider.Result(
            ContainerUtil.createConcurrentWeakMap<LibraryInfo, LibrariesAndSdks>(),
            ProjectRootManager.getInstance(project)
        )
    }

    private val moduleDependenciesCache by CachedValue(project) {
        CachedValueProvider.Result(
            ContainerUtil.createConcurrentWeakMap<Module, LibrariesAndSdks>(),
            ProjectRootManager.getInstance(project)
        )
    }

    override fun getLibrariesAndSdksUsedWith(libraryInfo: LibraryInfo): LibrariesAndSdks =
        cache.getOrPut(libraryInfo) { computeLibrariesAndSdksUsedWith(libraryInfo) }

    private fun computeLibrariesAndSdksUsedIn(module: Module): LibrariesAndSdks {
        val libraries = LinkedHashSet<LibraryInfo>()
        val sdks = LinkedHashSet<SdkInfo>()

        val processedModules = HashSet<Module>()
        val condition = Condition<OrderEntry> { orderEntry ->
            orderEntry.safeAs<ModuleOrderEntry>()?.let {
                it.module?.run { this !in processedModules } ?: false
            } ?: true
        }

        ModuleRootManager.getInstance(module).orderEntries().recursively().satisfying(condition).process(object : RootPolicy<Unit>() {
            override fun visitModuleSourceOrderEntry(moduleSourceOrderEntry: ModuleSourceOrderEntry, value: Unit) {
                processedModules.add(moduleSourceOrderEntry.ownerModule)
            }

            override fun visitLibraryOrderEntry(libraryOrderEntry: LibraryOrderEntry, value: Unit) {
                libraryOrderEntry.library.safeAs<LibraryEx>()?.takeIf { !it.isDisposed }?.let {
                    libraries += createLibraryInfo(project, it)
                }
            }

            override fun visitJdkOrderEntry(jdkOrderEntry: JdkOrderEntry, value: Unit) {
                jdkOrderEntry.jdk?.let { jdk ->
                    sdks += SdkInfo(project, jdk)
                }
            }
        }, Unit)

        return Pair(libraries.toList(), sdks.toList())
    }

    //NOTE: used LibraryRuntimeClasspathScope as reference
    private fun computeLibrariesAndSdksUsedWith(libraryInfo: LibraryInfo): LibrariesAndSdks {
        val libraries = LinkedHashSet<LibraryInfo>()
        val sdks = LinkedHashSet<SdkInfo>()

        val platform = libraryInfo.platform

        for (module in getLibraryUsageIndex().getModulesLibraryIsUsedIn(libraryInfo)) {
            val (moduleLibraries, moduleSdks) = moduleDependenciesCache.getOrPut(module) {
                computeLibrariesAndSdksUsedIn(module)
            }

            moduleLibraries.filter { compatiblePlatforms(platform, it.platform) }.forEach { libraries.add(it) }
            moduleSdks.filter { compatiblePlatforms(platform, it.platform) }.forEach { sdks.add(it) }
        }

        val filteredLibraries = filterForBuiltins(libraryInfo, libraries)

        return Pair(filteredLibraries.toList(), sdks.toList())
    }

    /**
     * @return true if it's OK to add a dependency from a library with platform [from] to a library with platform [to]
     */
    private fun compatiblePlatforms(from: TargetPlatform, to: TargetPlatform): Boolean {
        return from === to || to.containsAll(from) || to.isCommon()
    }

    private fun getLibraryUsageIndex(): LibraryUsageIndex {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(LibraryUsageIndex(), ProjectRootModificationTracker.getInstance(project))
        }!!
    }

    /*
    * When built-ins are created from module dependencies (as opposed to loading them from classloader)
    * we must resolve Kotlin standard library containing some of the built-ins declarations in the same
    * resolver for project as JDK. This comes from the following requirements:
    * - JvmBuiltins need JDK and standard library descriptors -> resolver for project should be able to
    *   resolve them
    * - Builtins are created in BuiltinsCache -> module descriptors should be resolved under lock of the
    *   SDK resolver to prevent deadlocks
    * This means we have to maintain dependencies of the standard library manually or effectively drop
    * resolver for SDK otherwise. Libraries depend on superset of their actual dependencies because of
    * the inability to get real dependencies from IDEA model. So moving stdlib with all dependencies
    * down is a questionable option.
    */
    private fun filterForBuiltins(libraryInfo: LibraryInfo, dependencyLibraries: Set<LibraryInfo>): Set<LibraryInfo> {
        return if (!IdeBuiltInsLoadingState.isFromClassLoader && libraryInfo.isCoreKotlinLibrary(project)) {
            dependencyLibraries.filterTo(mutableSetOf()) { it.isCoreKotlinLibrary(project) }
        } else {
            dependencyLibraries
        }
    }

    private inner class LibraryUsageIndex {
        private val modulesLibraryIsUsedIn: MultiMap<Library, Module> = MultiMap.createSet()

        init {
            for (module in ModuleManager.getInstance(project).modules) {
                for (entry in ModuleRootManager.getInstance(module).orderEntries) {
                    if (entry is LibraryOrderEntry) {
                        val library = entry.library
                        if (library != null) {
                            modulesLibraryIsUsedIn.putValue(library, module)
                        }
                    }
                }
            }
        }

        fun getModulesLibraryIsUsedIn(libraryInfo: LibraryInfo) = sequence<Module> {
            val ideaModelInfosCache = getIdeaModelInfosCache(project)
            for (module in modulesLibraryIsUsedIn[libraryInfo.library]) {
                val mappedModuleInfos = ideaModelInfosCache.getModuleInfosForModule(module)
                if (mappedModuleInfos.any { it.platform.canDependOn(libraryInfo, module.isHMPPEnabled) }) {
                    yield(module)
                }
            }
        }
    }
}
