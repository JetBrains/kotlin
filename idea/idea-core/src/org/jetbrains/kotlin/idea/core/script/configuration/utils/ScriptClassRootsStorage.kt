/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.idea.core.script.debug
import java.io.File

@State(
    name = "ScriptClassRootsStorage",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class ScriptClassRootsStorage : PersistentStateComponent<ScriptClassRootsStorage> {
    private var classpath: Set<String> = hashSetOf()
    private var sources: Set<String> = hashSetOf()
    private var sdks: Set<String> = hashSetOf()

    override fun getState(): ScriptClassRootsStorage? {
        return this
    }

    override fun loadState(state: ScriptClassRootsStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }

    private fun toStringNames(sdks: Collection<Sdk>): Set<String> {
        return sdks.map { it.name }.toSet()
    }

    private fun toStringValues(prop: Collection<File>): Set<String> {
        return prop.mapNotNull {
            when {
                it.isDirectory -> it.absolutePath
                it.isFile -> it.absolutePath + URLUtil.JAR_SEPARATOR
                else -> null
            }
        }.toSet()
    }

    private fun toVirtualFiles(prop: Set<String>, sources: Boolean): List<VirtualFile> {
        val rootType = if (sources) OrderRootType.SOURCES else OrderRootType.CLASSES
        return prop.mapNotNull { ProjectJdkTable.getInstance().findJdk(it) }
            .flatMap { it.rootProvider.getFiles(rootType).toList() }
    }

    private fun toVirtualFiles(prop: Collection<String>): List<VirtualFile> {
        return prop.mapNotNull {
            StandardFileSystems.local()?.findFileByPath(it)?.let {
                return@mapNotNull it
            }

            StandardFileSystems.jar()?.findFileByPath(it)?.let {
                return@mapNotNull it
            }

            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })

            null
        }.distinct()
    }

    fun containsAll(configuration: ScriptClassRoots): Boolean {
        if (!classpath.containsAll(toStringValues(configuration.classpathFiles))) {
            debug { "class roots were changed: old = $classpath, new = ${configuration.classpathFiles}" }
            return false
        }
        if (!sources.containsAll(toStringValues(configuration.sourcesFiles))) {
            debug { "source roots were changed: old = $sources, new = ${configuration.sourcesFiles}" }
            return false
        }
        if (!sdks.containsAll(toStringNames(configuration.sdks))) {
            debug { "sdk classes were changed: old = $sdks, new = ${configuration.sdks.map { it.homePath }}" }
            return false
        }
        return true
    }

    fun save(configuration: ScriptClassRoots) {
        // TODO: do not drop all storage on save: KT-34444
        classpath = toStringValues(configuration.classpathFiles)
        sources = toStringValues(configuration.sourcesFiles)

        sdks = toStringNames(configuration.sdks)
    }

    fun loadClasspathRoots(): List<VirtualFile> {
        return toVirtualFiles(sdks, false) + toVirtualFiles(classpath)
    }

    fun loadSourcesRoots(): List<VirtualFile> {
        return toVirtualFiles(sdks, true) + toVirtualFiles(sources)
    }

    companion object {
        fun getInstance(project: Project): ScriptClassRootsStorage =
            ServiceManager.getService(project, ScriptClassRootsStorage::class.java)

        data class ScriptClassRoots(
            val classpathFiles: List<File>,
            val sourcesFiles: List<File>,
            val sdks: List<Sdk>
        )
    }
}