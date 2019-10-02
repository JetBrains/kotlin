/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.idea.core.script.configuration.AbstractScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.DataInput
import java.io.DataOutput

abstract class ScriptConfigurationMemoryCache(
    val project: Project
) : ScriptConfigurationCache {
    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val memoryCache = SLRUMap<VirtualFile, CachedConfigurationSnapshot>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED)

    @Synchronized
    override operator fun get(file: VirtualFile): CachedConfigurationSnapshot? {
        return memoryCache.get(file)
    }


    @Synchronized
    override operator fun set(file: VirtualFile, configurationSnapshot: CachedConfigurationSnapshot) {
        memoryCache.put(
            file,
            configurationSnapshot
        )
    }

    @Synchronized
    override fun markOutOfDate(file: VirtualFile) {
        val old = memoryCache[file]
        if (old != null) {
            memoryCache.put(file, old.copy(inputs = CachedConfigurationInputs.OutOfDate))
        }
    }

    @Synchronized
    override fun all() = memoryCache.entrySet().map { it.key to it.value.configuration }
}