/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

open class ScriptConfigurationMemoryCache(
    val project: Project
) : ScriptConfigurationCache {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val memoryCache = SLRUMap<VirtualFile, ScriptConfigurationState>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED)

    @Synchronized
    override operator fun get(file: VirtualFile): ScriptConfigurationState? {
        return memoryCache.get(file)
    }

    override fun getAnyLoadedScript(): ScriptCompilationConfigurationWrapper? =
        memoryCache.entrySet().firstOrNull()?.value?.loaded?.configuration

    @Synchronized
    override fun setApplied(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
        val old = memoryCache[file] ?: ScriptConfigurationState()
        memoryCache.put(file, old.copy(applied = configurationSnapshot))
    }

    override fun remove(file: VirtualFile) =
        memoryCache.remove(file)

    @Synchronized
    override fun setLoaded(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
        val old = memoryCache[file] ?: ScriptConfigurationState()
        memoryCache.put(file, old.copy(loaded = configurationSnapshot))
    }

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    override fun allApplied(): List<Pair<VirtualFile, ScriptCompilationConfigurationWrapper>> {
        val result = mutableListOf<Pair<VirtualFile, ScriptCompilationConfigurationWrapper>>()
        for ((file, configuration) in memoryCache.entrySet()) {
            if (configuration.applied?.configuration != null) {
                result.add(Pair(file, configuration.applied.configuration))
            }
        }
        return result
    }

    @Synchronized
    override fun clear() {
        memoryCache.clear()
    }

}