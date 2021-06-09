/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.concurrentMapOf
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.ArrayDeque

/**
 * Maintains and caches mapping ModuleInfo -> SdkInfo *form its dependencies*
 * (note that this SDK might be different from Project SDK)
 *
 * Cache is needed because one and the same library might (and usually does)
 * participate as dependency in several modules, so if someone queries a lot
 * of modules for their SDKs (ex.: determine built-ins for each module in a
 * project), we end up inspecting one and the same dependency multiple times.
 *
 * With projects with abnormally high amount of dependencies this might lead
 * to performance issues.
 */
interface SdkInfoCache {
    fun findOrGetCachedSdk(moduleInfo: ModuleInfo): SdkInfo?

    companion object {
        fun getInstance(project: Project): SdkInfoCache =
            ServiceManager.getService(project, SdkInfoCache::class.java)
    }
}

class SdkInfoCacheImpl(private val project: Project) : SdkInfoCache {
    @JvmInline
    value class SdkDependency(val sdk: SdkInfo?)

    private val cache: MutableMap<ModuleInfo, SdkDependency>
        get() = project.cacheInvalidatingOnRootModifications { concurrentMapOf() }

    override fun findOrGetCachedSdk(moduleInfo: ModuleInfo): SdkInfo? {
        // get an operate on the fixed instance of a cache to avoid case of roots modification in the middle of lookup
        val instance = cache

        if (!instance.containsKey(moduleInfo)) {
            findSdk(instance, moduleInfo)
        }

        return instance[moduleInfo]?.sdk
    }

    private fun findSdk(cache: MutableMap<ModuleInfo, SdkDependency>, moduleInfo: ModuleInfo) {
        moduleInfo.safeAs<SdkDependency>()?.let {
            cache[moduleInfo] = it
            return
        }

        val libraryDependenciesCache = LibraryDependenciesCache.getInstance(this.project)
        val visitedModuleInfos = mutableSetOf<ModuleInfo>()

        // graphs is a stack of paths is used to implement DFS without recursion
        // it depends on a number of libs, that could be > 10k for a huge monorepos
        val graphs = ArrayDeque<List<ModuleInfo>>().also {
            // initial graph item
            it.add(listOf(moduleInfo))
        }

        val (path, sdkInfo) = run {
            while (graphs.isNotEmpty()) {
                ProgressManager.checkCanceled()
                // graph of DFS from the root i.e from `moduleInfo`

                // note: traverse of graph goes in a depth over the most left edge:
                // - poll() retrieves and removes the head of the queue
                // - push(element) inserts the element at the front of the deque
                val graph = graphs.poll()

                val last = graph.last()
                // the result could be immediately returned when cache already has it
                val cached = cache[last]
                if (cached != null) {
                    cached.sdk?.let { return@run graph to cached }
                    continue
                }

                if (!visitedModuleInfos.add(last)) continue

                val dependencies = run deps@{
                    if (last is LibraryInfo) {
                        // use a special case for LibraryInfo to reuse values from a library dependencies cache
                        val (libraries, sdks) = libraryDependenciesCache.getLibrariesAndSdksUsedWith(last)
                        sdks.firstOrNull()?.let {
                            return@run graph to SdkDependency(it)
                        }
                        libraries
                    } else {
                        last.dependencies()
                            .also { dependencies ->
                                dependencies.firstIsInstanceOrNull<SdkInfo>()?.let {
                                    return@run graph to SdkDependency(it)
                                }
                            }
                    }
                }

                dependencies.forEach { dependency ->
                    val sdkDependency = cache[dependency]
                    if (sdkDependency != null) {
                        sdkDependency.sdk?.let {
                            // sdk is found when some dependency is already resolved
                            return@run (graph + dependency) to sdkDependency
                        }
                    } else {
                        // otherwise add a new graph of (existed graph + dependency) as candidates for DFS lookup
                        if (!visitedModuleInfos.contains(dependency)) {
                            graphs.push(graph + dependency)
                        }
                    }
                }
            }
            return@run null to noSdkDependency
        }
        // when sdk is found: mark all graph elements could be resolved to the same sdk
        path?.let {
            it.forEach { info -> cache[info] = sdkInfo }

            visitedModuleInfos.removeAll(it)
        }
        // mark all visited modules (apart of found path) as dead ends
        visitedModuleInfos.forEach { info -> cache[info] = noSdkDependency }
    }

    companion object {
        private val noSdkDependency = SdkDependency(null)
    }
}