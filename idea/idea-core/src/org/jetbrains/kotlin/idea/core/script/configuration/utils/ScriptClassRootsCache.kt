/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope.compose
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.LOG
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.classpathEntryToVfs
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.toVfsRoots
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import java.lang.ref.Reference
import java.lang.ref.SoftReference

class ScriptClassRootsCache(
    val scripts: Map<String, LightScriptInfo>,
    val classes: Set<String>,
    val sources: Set<String>,
    val sdks: Map<String?, Sdk?>,
    private val nonModulesSdks: Collection<Sdk>,
    val customDefinitionsUsed: Boolean
) {
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

    class Builder(val project: Project) {
        val scripts = mutableMapOf<String, LightScriptInfo>()

        private val defaultSdk = getScriptDefaultSdk()
        val sdks = mutableMapOf<String?, Sdk?>(null to defaultSdk)

        val classes = mutableSetOf<String>()
        val sources = mutableSetOf<String>()
        private var customDefinitionsUsed: Boolean = false

        fun build(): ScriptClassRootsCache {
            val nonIndexedSdks = collectNonIndexedSdks()
            return ScriptClassRootsCache(scripts, classes, sources, sdks, nonIndexedSdks, customDefinitionsUsed)
        }

        fun collectNonIndexedSdks(): MutableSet<Sdk> {
            val nonIndexedSdks = sdks.values.filterNotNullTo(mutableSetOf())
            ModuleManager.getInstance(project).modules.map {
                nonIndexedSdks.remove(ModuleRootManager.getInstance(it).sdk)
            }
            return nonIndexedSdks
        }

        fun useCustomScriptDefinition() {
            customDefinitionsUsed = true
        }

        fun add(
            vFile: VirtualFile,
            configuration: ScriptCompilationConfigurationWrapper
        ) {
            addSdk(configuration.javaHome)

            configuration.dependenciesClassPath.forEach { classes.add(it.absolutePath) }
            configuration.dependenciesSources.forEach { sources.add(it.absolutePath) }

            scripts[vFile.path] = DirectScriptInfo(configuration)

            useCustomScriptDefinition()
        }

        fun add(other: Builder) {
            classes.addAll(other.classes)
            sources.addAll(other.sources)
            sdks.putAll(other.sdks)
            scripts.putAll(other.scripts)
        }

        fun addSdk(javaHome: File?): Sdk? {
            if (javaHome == null) return defaultSdk
            val canonicalPath = javaHome.canonicalPath
            return sdks.getOrPut(canonicalPath) {
                getScriptSdkByJavaHome(javaHome) ?: defaultSdk
            }
        }

        private fun getScriptSdkByJavaHome(javaHome: File): Sdk? {
            // workaround for mismatched gradle wrapper and plugin version
            val javaHomeVF = try {
                VfsUtil.findFileByIoFile(javaHome, true)
            } catch (e: Throwable) {
                null
            } ?: return null

            return getProjectJdkTableSafe().allJdks.find { it.homeDirectory == javaHomeVF }
        }

        fun addSdkByName(sdkName: String) {
            val sdk = getProjectJdkTableSafe().allJdks.find { it.name == sdkName } ?: defaultSdk ?: return
            val homePath = sdk.homePath ?: return
            sdks[homePath] = sdk
        }

        private fun getScriptDefaultSdk(): Sdk? {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.canBeUsedForScript() }
            if (projectSdk != null) return projectSdk

            val anyJavaSdk = getAllProjectSdks().find { it.canBeUsedForScript() }
            if (anyJavaSdk != null) {
                return anyJavaSdk
            }

            LOG.warn(
                "Default Script SDK is null: " +
                        "projectSdk = ${ProjectRootManager.getInstance(project).projectSdk}, " +
                        "all sdks = ${getAllProjectSdks().joinToString("\n")}"
            )

            return null
        }

        private fun Sdk.canBeUsedForScript() = sdkType is JavaSdkType
    }

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
        val sdk = sdks[configuration.javaHome?.canonicalPath]

        return if (sdk == null) {
            HeavyScriptInfo(configuration, compose(toVfsRoots(roots)), null)
        } else {
            val sdkClasses = sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList()
            HeavyScriptInfo(configuration, compose(sdkClasses + toVfsRoots(roots)), sdk)
        }
    }

    val firstScriptSdk: Sdk? = sdks.values.firstOrNull()

    val allDependenciesClassFiles: List<VirtualFile>

    val allDependenciesSources: List<VirtualFile>

    init {
        allDependenciesClassFiles = mutableSetOf<VirtualFile>().also { result ->
            nonModulesSdks.forEach { result.addAll(it.rootProvider.getFiles(OrderRootType.CLASSES)) }
            classes.mapNotNullTo(result) { classpathEntryToVfs(File(it)) }
        }.toList()

        allDependenciesSources = mutableSetOf<VirtualFile>().also { result ->
            nonModulesSdks.forEach { result.addAll(it.rootProvider.getFiles(OrderRootType.SOURCES)) }
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

    fun hasInvalidSdk(project: Project): Boolean {
        val builder = Builder(project)
        if (sdks.any { (home, sdk) -> builder.addSdk(home?.let(::File)) != sdk }) return true
        if (builder.collectNonIndexedSdks() != nonModulesSdks) return true
        return false
    }

    fun diff(old: ScriptClassRootsCache): Updates =
        Updates(
            hasNewRoots(old),
            getChangedScripts(old)
        )

    private fun hasNewRoots(old: ScriptClassRootsCache): Boolean {
        return classes.any { it !in old.classes }
                || sources.any { it !in old.sources }
                || sdks.any { it.key !in old.sdks }
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

    class Updates(
        val hasNewRoots: Boolean,
        val updatedScripts: Set<String>
    ) {
        val changed: Boolean
            get() = hasNewRoots || updatedScripts.isNotEmpty()
    }
}

