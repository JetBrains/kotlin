/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ScriptClassRootsStorage",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class ScriptClassRootsStorage(val project: Project) : PersistentStateComponent<ScriptClassRootsStorage> {
    private var classpath: Set<String> = hashSetOf()
    private var sources: Set<String> = hashSetOf()
    private var sdks: Set<String> = hashSetOf()

    override fun getState(): ScriptClassRootsStorage? {
        return this
    }

    override fun loadState(state: ScriptClassRootsStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun save(roots: ScriptClassRootsCache) {
        classpath = roots.classes
        sources = roots.sources
        sdks = roots.sdks.keys.toSet()
    }

    fun load(): ScriptClassRootsCache =
        ScriptClassRootsCache.Builder(project).let {
            it.sources.addAll(sources)
            it.classes.addAll(classpath)
            sdks.forEach(it::addSdkByName)
            it.build()
        }

    companion object {
        fun getInstance(project: Project): ScriptClassRootsStorage =
            ServiceManager.getService(project, ScriptClassRootsStorage::class.java)
    }
}