/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.ucache

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope.compose
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.classpathEntryToVfs
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.toVfsRoots
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import java.lang.ref.Reference
import java.lang.ref.SoftReference

class ScriptClassRootsCache(
    val project: Project,
    private val scripts: Map<String, LightScriptInfo>,
    private val classes: Set<String>,
    private val sources: Set<String>,
    val customDefinitionsUsed: Boolean,
    val sdks: ScriptSdks
) {
    fun withUpdatedSdks(newSdks: ScriptSdks) =
        ScriptClassRootsCache(project, scripts, classes, sources, customDefinitionsUsed, newSdks)

    abstract class LightScriptInfo(val definition: ScriptDefinition?) {
        @Volatile
        var heavyCache: Reference<HeavyScriptInfo>? = null

        abstract fun buildConfiguration(): ScriptCompilationConfigurationWrapper?
    }

    class DirectScriptInfo(val result: ScriptCompilationConfigurationWrapper) : LightScriptInfo(null) {
        override fun buildConfiguration(): ScriptCompilationConfigurationWrapper = result
    }

    class HeavyScriptInfo(
        val scriptConfiguration: ScriptCompilationConfigurationWrapper,
        val classFilesScope: GlobalSearchScope,
        val sdk: Sdk?
    )

    fun getLightScriptInfo(file: String) = scripts[file]

    fun contains(file: VirtualFile): Boolean = file.path in scripts

    private fun getHeavyScriptInfo(file: String): HeavyScriptInfo? {
        val lightScriptInfo = getLightScriptInfo(file) ?: return null
        val heavy0 = lightScriptInfo.heavyCache?.get()
        if (heavy0 != null) return heavy0
        synchronized(lightScriptInfo) {
            val heavy1 = lightScriptInfo.heavyCache?.get()
            if (heavy1 != null) return heavy1
            val heavy2 = computeHeavy(lightScriptInfo)
            lightScriptInfo.heavyCache = SoftReference(heavy2)
            return heavy2
        }
    }

    private fun computeHeavy(lightScriptInfo: LightScriptInfo): HeavyScriptInfo? {
        val configuration = lightScriptInfo.buildConfiguration() ?: return null

        val roots = configuration.dependenciesClassPath
        val sdk = sdks[SdkId(configuration.javaHome)]

        return if (sdk == null) {
            HeavyScriptInfo(configuration, compose(toVfsRoots(roots)), null)
        } else {
            val sdkClasses = sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList()
            HeavyScriptInfo(configuration, compose(sdkClasses + toVfsRoots(roots)), sdk)
        }
    }

    val firstScriptSdk: Sdk?
        get() = sdks.first

    val allDependenciesClassFiles: List<VirtualFile>

    val allDependenciesSources: List<VirtualFile>

    init {
        allDependenciesClassFiles = mutableSetOf<VirtualFile>().also { result ->
            result.addAll(sdks.nonIndexedClassRoots)
            classes.mapNotNullTo(result) { classpathEntryToVfs(File(it)) }
        }.toList()

        allDependenciesSources = mutableSetOf<VirtualFile>().also { result ->
            result.addAll(sdks.nonIndexedSourceRoots)
            sources.mapNotNullTo(result) { classpathEntryToVfs(File(it)) }
        }.toList()
    }

    val allDependenciesClassFilesScope = compose(allDependenciesClassFiles)

    val allDependenciesSourcesScope = compose(allDependenciesSources)

    fun getScriptConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        getHeavyScriptInfo(file.path)?.scriptConfiguration

    fun getScriptSdk(file: VirtualFile): Sdk? =
        getHeavyScriptInfo(file.path)?.sdk

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        getHeavyScriptInfo(file.path)?.classFilesScope ?: GlobalSearchScope.EMPTY_SCOPE

    fun diff(old: ScriptClassRootsCache?): Updates =
        when (old) {
            null -> FullUpdate(this)
            this -> NotChanged(this)
            else -> IncrementalUpdates(
                this,
                this.hasNewRoots(old),
                old.hasNewRoots(this),
                getChangedScripts(old)
            )
        }

    private fun hasNewRoots(old: ScriptClassRootsCache): Boolean {
        val oldClassRoots = old.allDependenciesClassFiles.toSet()
        val oldSourceRoots = old.allDependenciesSources.toSet()

        return allDependenciesClassFiles.any { it !in oldClassRoots }
                || allDependenciesSources.any { it !in oldSourceRoots }
    }

    private fun getChangedScripts(old: ScriptClassRootsCache): Set<String> {
        val changed = mutableSetOf<String>()

        scripts.forEach {
            if (old.scripts[it.key] != it.value) {
                changed.add(it.key)
            }
        }

        old.scripts.forEach {
            if (it.key !in scripts) {
                changed.add(it.key)
            }
        }

        return changed
    }

    interface Updates {
        val cache: ScriptClassRootsCache
        val changed: Boolean
        val hasNewRoots: Boolean
        val hasUpdatedScripts: Boolean
        fun isScriptChanged(scriptPath: String): Boolean
    }

    class IncrementalUpdates(
        override val cache: ScriptClassRootsCache,
        override val hasNewRoots: Boolean,
        private val hasOldRoots: Boolean,
        private val updatedScripts: Set<String>
    ) : Updates {
        override val hasUpdatedScripts: Boolean get() = updatedScripts.isNotEmpty()
        override fun isScriptChanged(scriptPath: String) = scriptPath in updatedScripts

        override val changed: Boolean
            get() = hasNewRoots || updatedScripts.isNotEmpty() || hasOldRoots
    }

    class FullUpdate(override val cache: ScriptClassRootsCache) : Updates {
        override val changed: Boolean get() = true
        override val hasUpdatedScripts: Boolean get() = true
        override fun isScriptChanged(scriptPath: String): Boolean = true

        override val hasNewRoots: Boolean
            get() {
                return cache.allDependenciesClassFiles.isNotEmpty() || cache.allDependenciesSources.isNotEmpty()
            }
    }

    class NotChanged(override val cache: ScriptClassRootsCache) : Updates {
        override val changed: Boolean get() = false
        override val hasNewRoots: Boolean get() = false
        override val hasUpdatedScripts: Boolean get() = false
        override fun isScriptChanged(scriptPath: String): Boolean = false
    }
}

