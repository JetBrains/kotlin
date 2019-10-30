/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.ProjectTopics
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.KotlinScriptDependenciesClassFinder
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationMemoryCache.Companion.MAX_SCRIPTS_CACHED
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

class ScriptConfigurationMemoryCache internal constructor(private val project: Project) {

    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                clearClassRootsCaches()
            }
        })
    }

    private val cacheLock = ReentrantReadWriteLock()

    private val scriptDependenciesCache =
        SLRUCacheWithLock<VirtualFile, ScriptCompilationConfigurationWrapper>()
    private val scriptsModificationStampsCache =
        SLRUCacheWithLock<VirtualFile, Long>()

    fun getCachedConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? = scriptDependenciesCache.get(file)

    fun isConfigurationUpToDate(file: VirtualFile): Boolean {
        return scriptsModificationStampsCache.get(file) == file.modificationStamp
    }

    fun setUpToDate(file: VirtualFile) {
        scriptsModificationStampsCache.replace(file, file.modificationStamp)
    }

    fun replaceConfiguration(file: VirtualFile, new: ScriptCompilationConfigurationWrapper) {
        scriptDependenciesCache.replace(file, new)
    }

    val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = getConfiguration(file)
                ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

            val roots = compilationConfiguration.dependenciesClassPath
            val sdk = scriptsSdksCache[file]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap NonClasspathDirectoriesScope.compose(
                    ScriptConfigurationManager.toVfsRoots(
                        roots
                    )
                )
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() +
                        ScriptConfigurationManager.toVfsRoots(roots)
            )
        }

    val scriptsSdksCache: MutableMap<VirtualFile, Sdk?> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = getConfiguration(file)
            return@createWeakMap getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(
                project
            )
        }

    private fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? {
        val configuration = getCachedConfiguration(file)

        if (configuration != null) return configuration

        val ktFile = runReadAction { PsiManager.getInstance(project).findFile(file) as? KtFile } ?: return null
        return ScriptConfigurationManager.getInstance(project)
            .getConfiguration(ktFile)
    }

    private fun getScriptSdk(compilationConfiguration: ScriptCompilationConfigurationWrapper?): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHome = try {
            compilationConfiguration?.javaHome?.let { VfsUtil.findFileByIoFile(it, true) }
        } catch (e: Throwable) {
            null
        } ?: return null

        return getAllProjectSdks().find { it.homeDirectory == javaHome }
    }

    val firstScriptSdk: Sdk?
        get() {
            val firstCachedScript = scriptDependenciesCache.getAll().firstOrNull()?.key ?: return null
            return scriptsSdksCache[firstCachedScript]
        }

    private val allSdks by ClearableLazyValue(cacheLock) {
        scriptDependenciesCache.getAll()
            .mapNotNull { scriptsSdksCache[it.key] }
            .distinct()
    }

    private val allNonIndexedSdks by ClearableLazyValue(
        cacheLock
    ) {
        scriptDependenciesCache.getAll()
            .mapNotNull { scriptsSdksCache[it.key] }
            .filterNonModuleSdk()
            .distinct()
    }

    val allDependenciesClassFiles by ClearableLazyValue(
        cacheLock
    ) {
        val sdkFiles = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.CLASSES).toList() }

        val scriptDependenciesClasspath = scriptDependenciesCache.getAll()
            .flatMap { it.value.dependenciesClassPath }.distinct()

        sdkFiles + ScriptConfigurationManager.toVfsRoots(
            scriptDependenciesClasspath
        )
    }

    val allDependenciesSources by ClearableLazyValue(cacheLock) {
        val sdkSources = allNonIndexedSdks
            .flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }

        val scriptDependenciesSources = scriptDependenciesCache.getAll()
            .flatMap { it.value.dependenciesSources }.distinct()
        sdkSources + ScriptConfigurationManager.toVfsRoots(
            scriptDependenciesSources
        )
    }

    val allDependenciesClassFilesScope by ClearableLazyValue(
        cacheLock
    ) {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by ClearableLazyValue(
        cacheLock
    ) {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private fun List<Sdk>.filterNonModuleSdk(): List<Sdk> {
        val moduleSdks = ModuleManager.getInstance(project).modules.map { ModuleRootManager.getInstance(it).sdk }
        return filterNot { moduleSdks.contains(it) }
    }

    fun clearConfigurationCaches(): List<VirtualFile> {
        debug { "configuration caches cleared" }

        val keys = scriptDependenciesCache.getAll().map { it.key }.toList()

        scriptDependenciesCache.clear()
        scriptsModificationStampsCache.clear()

        return keys
    }

    fun clearClassRootsCaches() {
        debug { "class roots caches cleared" }

        this::allSdks.clearValue()
        this::allNonIndexedSdks.clearValue()

        this::allDependenciesClassFiles.clearValue()
        this::allDependenciesClassFilesScope.clearValue()

        this::allDependenciesSources.clearValue()
        this::allDependenciesSourcesScope.clearValue()

        scriptsDependenciesClasspathScopeCache.clear()
        scriptsSdksCache.clear()

        val kotlinScriptDependenciesClassFinder =
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
    }

    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = getScriptSdk(compilationConfiguration) ?: ScriptConfigurationManager.getScriptDefaultSdk(
            project
        )
        val wasSdkChanged = scriptSdk != null && !allSdks.contains(scriptSdk)
        if (wasSdkChanged) {
            debug { "sdk was changed: $compilationConfiguration" }
            return true
        }

        val newClassRoots = ScriptConfigurationManager.toVfsRoots(
            compilationConfiguration.dependenciesClassPath
        )
        for (newClassRoot in newClassRoots) {
            if (!allDependenciesClassFiles.contains(newClassRoot)) {
                debug { "class root was changed: $newClassRoot" }
                return true
            }
        }

        val newSourceRoots = ScriptConfigurationManager.toVfsRoots(
            compilationConfiguration.dependenciesSources
        )
        for (newSourceRoot in newSourceRoots) {
            if (!allDependenciesSources.contains(newSourceRoot)) {
                debug { "source root was changed: $newSourceRoot" }
                return true
            }
        }
        return false
    }

}

private fun <R> KProperty0<R>.clearValue() {
    isAccessible = true
    (getDelegate() as ClearableLazyValue<*, *>).clear()
}

private class ClearableLazyValue<in R, out T : Any>(
    private val lock: ReentrantReadWriteLock,
    private val compute: () -> T
) : ReadOnlyProperty<R, T> {
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        lock.write {
            if (value == null) {
                value = compute()
            }
            return value!!
        }
    }

    private var value: T? = null


    fun clear() {
        lock.write {
            value = null
        }
    }
}

private class SLRUCacheWithLock<K, V> {
    private val lock = ReentrantReadWriteLock()

    val cache = SLRUMap<K, V>(
        MAX_SCRIPTS_CACHED,
        MAX_SCRIPTS_CACHED
    )

    fun get(value: K): V? = lock.write {
        cache[value]
    }

    fun getOrPut(key: K, defaultValue: () -> V): V = lock.write {
        val value = cache.get(key)
        return if (value == null) {
            val answer = defaultValue()
            replace(key, answer)
            answer
        } else {
            value
        }
    }

    fun remove(file: K) = lock.write {
        cache.remove(file)
    }

    fun getAll(): Collection<Map.Entry<K, V>> = lock.write {
        cache.entrySet()
    }

    fun clear() = lock.write {
        cache.clear()
    }

    fun replace(file: K, value: V): V? = lock.write {
        val old = get(file)
        cache.put(file, value)
        old
    }
}