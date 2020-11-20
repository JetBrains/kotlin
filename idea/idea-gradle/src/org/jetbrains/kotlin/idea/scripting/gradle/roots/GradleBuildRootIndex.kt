/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.scripting.gradle.settings.StandaloneScriptsStorage

class GradleBuildRootIndex(private val project: Project) : StandaloneScriptsUpdater {
    private val log = logger<GradleBuildRootIndex>()

    private inner class StandaloneScriptRootsCache {
        private val standaloneScripts: MutableSet<String>
            get() = StandaloneScriptsStorage.getInstance(project)?.files ?: hashSetOf()

        private val standaloneScriptRoots = mutableMapOf<String, GradleBuildRoot?>().apply {
            standaloneScripts.forEach(::updateRootsCache)
        }

        fun all(): List<String> = standaloneScripts.toList()
        fun get(path: String): GradleBuildRoot? = standaloneScriptRoots[path]

        fun add(path: String) {
            standaloneScripts.add(path)
            updateRootsCache(path)
        }

        fun remove(path: String): GradleBuildRoot? {
            standaloneScripts.remove(path)
            return standaloneScriptRoots.remove(path)
        }

        fun updateRootsCache(path: String) {
            standaloneScriptRoots[path] = findNearestRoot(path)
        }
    }

    private val standaloneScriptRoots by lazy { StandaloneScriptRootsCache() }

    private val byWorkingDir = HashMap<String, GradleBuildRoot>()
    private val byProjectDir = HashMap<String, GradleBuildRoot>()

    val list: Collection<GradleBuildRoot>
        get() = byWorkingDir.values

    @Synchronized
    fun rebuildProjectRoots() {
        byProjectDir.clear()
        byWorkingDir.values.forEach { buildRoot ->
            buildRoot.projectRoots.forEach {
                byProjectDir[it] = buildRoot
            }
        }

        standaloneScriptRoots.all().forEach(standaloneScriptRoots::updateRootsCache)
    }

    @Synchronized
    fun getBuildByRootDir(dir: String) = byWorkingDir[dir]

    @Synchronized
    fun findNearestRoot(path: String): GradleBuildRoot? {
        var max: Pair<String, GradleBuildRoot>? = null
        byWorkingDir.entries.forEach {
            if (path.startsWith(it.key) && (max == null || it.key.length > max!!.first.length)) {
                max = it.key to it.value
            }
        }
        return max?.second
    }

    @Synchronized
    fun getBuildByProjectDir(projectDir: String) = byProjectDir[projectDir]

    @Synchronized
    fun isStandaloneScript(path: String) = path in standaloneScriptRoots.all()

    @Synchronized
    fun getStandaloneScriptRoot(path: String) = standaloneScriptRoots.get(path)

    @Synchronized
    fun add(value: GradleBuildRoot): GradleBuildRoot? {
        val prefix = value.pathPrefix
        val old = byWorkingDir.put(prefix, value)
        rebuildProjectRoots()
        log.info("$prefix: $old -> $value")
        return old
    }

    @Synchronized
    fun remove(prefix: String) = byWorkingDir.remove(prefix)?.also {
        rebuildProjectRoots()
        log.info("$prefix: removed")
    }

    @Synchronized
    override fun addStandaloneScript(path: String) {
        standaloneScriptRoots.add(path)
    }

    @Synchronized
    override fun removeStandaloneScript(path: String): GradleBuildRoot? {
        return standaloneScriptRoots.remove(path)
    }

    override val standaloneScripts: Collection<String>
        @Synchronized get() = standaloneScriptRoots.all()
}