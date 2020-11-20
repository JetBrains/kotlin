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
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptCacheDependencies.Companion.scriptCacheDependencies
import org.jetbrains.kotlin.idea.core.util.cachedFileAttribute
import org.jetbrains.kotlin.idea.core.util.readObject
import org.jetbrains.kotlin.idea.core.util.writeObject
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import java.io.Serializable
import java.lang.ref.Reference
import java.lang.ref.SoftReference

class ScriptClassRootsCache(
    private val scripts: Map<String, LightScriptInfo>,
    private val classes: Set<String>,
    private val sources: Set<String>,
    val customDefinitionsUsed: Boolean,
    val sdks: ScriptSdks
) {
    companion object {
        val EMPTY = ScriptClassRootsCache(
            mapOf(), setOf(), setOf(), true,
            ScriptSdks(mapOf(), setOf(), setOf())
        )
    }

    fun withUpdatedSdks(newSdks: ScriptSdks) =
        ScriptClassRootsCache(scripts, classes, sources, customDefinitionsUsed, newSdks)

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

    val allDependenciesClassFiles: Set<VirtualFile>

    val allDependenciesSources: Set<VirtualFile>

    init {
        allDependenciesClassFiles = mutableSetOf<VirtualFile>().also { result ->
            result.addAll(sdks.nonIndexedClassRoots)
            classes.mapNotNullTo(result) { classpathEntryToVfs(File(it)) }
        }

        allDependenciesSources = mutableSetOf<VirtualFile>().also { result ->
            result.addAll(sdks.nonIndexedSourceRoots)
            sources.mapNotNullTo(result) { classpathEntryToVfs(File(it)) }
        }
    }

    val allDependenciesClassFilesScope = compose(allDependenciesClassFiles.toList())

    val allDependenciesSourcesScope = compose(allDependenciesSources.toList())

    fun getScriptConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        getHeavyScriptInfo(file.path)?.scriptConfiguration

    fun getScriptSdk(file: VirtualFile): Sdk? =
        getHeavyScriptInfo(file.path)?.sdk

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        getHeavyScriptInfo(file.path)?.classFilesScope ?: GlobalSearchScope.EMPTY_SCOPE

    fun diff(project: Project, old: ScriptClassRootsCache?): Updates =
        when (old) {
            null -> FullUpdate(project, this)
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

    class FullUpdate(private val project: Project, override val cache: ScriptClassRootsCache) : Updates {
        override val changed: Boolean get() = true
        override val hasUpdatedScripts: Boolean get() = true
        override fun isScriptChanged(scriptPath: String): Boolean = true

        override val hasNewRoots: Boolean
            get() {
                val scriptCacheDependencies = project.scriptCacheDependencies()
                return scriptCacheDependencies?.sameAs(cache) == false
            }
    }

    class NotChanged(override val cache: ScriptClassRootsCache) : Updates {
        override val changed: Boolean get() = false
        override val hasNewRoots: Boolean get() = false
        override val hasUpdatedScripts: Boolean get() = false
        override fun isScriptChanged(scriptPath: String): Boolean = false
    }
}

internal class ScriptCacheDependencies(
    val classFiles: Set<String>,
    val sources: Set<String>
) : Serializable {
    constructor(cache: ScriptClassRootsCache) : this(
        cache.allDependenciesClassFiles.mapTo(HashSet(), VirtualFile::getPath),
        cache.allDependenciesSources.mapTo(HashSet(), VirtualFile::getPath)
    )

    fun sameAs(cache: ScriptClassRootsCache): Boolean {
        if (cache.allDependenciesClassFiles.size != classFiles.size ||
            cache.allDependenciesSources.size != sources.size
        ) {
            return false
        }

        return cache.allDependenciesClassFiles.firstOrNull {
            !classFiles.contains(it.path)
        } == null && cache.allDependenciesSources.firstOrNull {
            !sources.contains(it.path)
        } == null
    }

    fun save(project: Project) {
        project.scriptCacheDependenciesFile()?.scriptCacheDependencies = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScriptCacheDependencies

        return classFiles == other.classFiles && sources == other.sources
    }

    override fun hashCode(): Int {
        var result = classFiles.hashCode()
        result = 31 * result + sources.hashCode()
        return result
    }

    companion object {
        private fun Project.scriptCacheDependenciesFile(): VirtualFile? {
            var file = this.projectFile ?: return null
            while (!file.isDirectory || file.name == Project.DIRECTORY_STORE_FOLDER) {
                file = file.parent
            }

            return file
        }

        fun Project.scriptCacheDependencies(): ScriptCacheDependencies? =
            try {
                scriptCacheDependenciesFile()?.scriptCacheDependencies
            } catch (e: Exception) {
                null
            }

    }

}

private var VirtualFile.scriptCacheDependencies: ScriptCacheDependencies? by cachedFileAttribute(
    name = "kotlin-script-cache-dependencies",
    version = 1,
    read = { readObject() },
    write = { writeObject(it) }
)
