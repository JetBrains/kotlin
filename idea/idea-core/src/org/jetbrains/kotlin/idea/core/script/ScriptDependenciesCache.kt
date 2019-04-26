/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.SLRUMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.EDT
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.script.experimental.dependencies.ScriptDependencies

class ScriptDependenciesCache(private val project: Project) {

    companion object {
        const val MAX_SCRIPTS_CACHED = 50
    }

    private val cacheLock = ReentrantReadWriteLock()

    private val scriptDependenciesCache = SLRUCacheWithLock<ScriptDependencies>()
    private val scriptsModificationStampsCache = SLRUCacheWithLock<Long>()

    operator fun get(virtualFile: VirtualFile): ScriptDependencies? = scriptDependenciesCache.get(virtualFile)

    fun shouldRunDependenciesUpdate(file: VirtualFile): Boolean {
        return scriptsModificationStampsCache.replace(file, file.modificationStamp) != file.modificationStamp
    }

    val allScriptsClasspath by ClearableLazyValue(cacheLock) {
        val files = scriptDependenciesCache.getAll().flatMap { it.value.classpath }.distinct()
        ScriptDependenciesManager.toVfsRoots(files)
    }

    val allScriptsClasspathScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allScriptsClasspath)
    }

    val allLibrarySources by ClearableLazyValue(cacheLock) {
        ScriptDependenciesManager.toVfsRoots(scriptDependenciesCache.getAll().flatMap { it.value.sources }.distinct())
    }

    val allLibrarySourcesScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allLibrarySources)
    }

    private fun onChange(files: List<VirtualFile>) {
        this::allScriptsClasspath.clearValue()
        this::allScriptsClasspathScope.clearValue()
        this::allLibrarySources.clearValue()
        this::allLibrarySourcesScope.clearValue()

        val kotlinScriptDependenciesClassFinder =
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()
        updateHighlighting(files)
    }

    private fun updateHighlighting(files: List<VirtualFile>) {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        GlobalScope.launch(EDT(project)) {
            files.filter { it.isValid }.forEach {
                PsiManager.getInstance(project).findFile(it)?.let { psiFile ->
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }

    fun hasNotCachedRoots(scriptDependencies: ScriptDependencies): Boolean {
        return !allScriptsClasspath.containsAll(ScriptDependenciesManager.toVfsRoots(scriptDependencies.classpath)) ||
                !allLibrarySources.containsAll(ScriptDependenciesManager.toVfsRoots(scriptDependencies.sources))
    }

    fun clear() {
        val keys = scriptDependenciesCache.getAll().map { it.key }.toList()

        scriptDependenciesCache.clear()

        onChange(keys)
    }

    fun save(virtualFile: VirtualFile, new: ScriptDependencies): Boolean {
        val old = scriptDependenciesCache.replace(virtualFile, new)
        val changed = new != old
        if (changed) {
            onChange(listOf(virtualFile))
        }

        return changed
    }

    fun delete(virtualFile: VirtualFile): Boolean {
        val changed = scriptDependenciesCache.remove(virtualFile)
        if (changed) {
            onChange(listOf(virtualFile))
        }
        return changed
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


private class SLRUCacheWithLock<T> {
    private val lock = ReentrantReadWriteLock()

    val cache = SLRUMap<VirtualFile, T>(
        ScriptDependenciesCache.MAX_SCRIPTS_CACHED,
        ScriptDependenciesCache.MAX_SCRIPTS_CACHED
    )

    fun get(value: VirtualFile): T? = lock.write {
        cache[value]
    }

    fun remove(file: VirtualFile) = lock.write {
        cache.remove(file)
    }

    fun getAll(): Collection<Map.Entry<VirtualFile, T>> = lock.write {
        cache.entrySet()
    }

    fun clear() = lock.write {
        cache.clear()
    }

    fun replace(file: VirtualFile, value: T): T? = lock.write {
        val old = get(file)
        cache.put(file, value)
        old
    }
}

