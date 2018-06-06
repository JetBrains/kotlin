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
import kotlinx.coroutines.experimental.launch
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.script.experimental.dependencies.ScriptDependencies

private const val MAX_SCRIPTS_CACHED = 10

class ScriptDependenciesCache(private val project: Project) {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = SLRUMap<VirtualFile, ScriptDependencies>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED)

    operator fun get(virtualFile: VirtualFile): ScriptDependencies? = cacheLock.write { cache[virtualFile] }

    val allScriptsClasspath by ClearableLazyValue(cacheLock) {
        val files = cache.entrySet().flatMap { it.value.classpath }.distinct()
        ScriptDependenciesManager.toVfsRoots(files)
    }

    val allScriptsClasspathScope by ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allScriptsClasspath)
    }

    val allLibrarySources by ClearableLazyValue(cacheLock) {
        ScriptDependenciesManager.toVfsRoots(cache.entrySet().flatMap { it.value.sources }.distinct())
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

        launch(EDT(project)) {
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
        val keys = cacheLock.read {
            val keys = mutableListOf<VirtualFile>()
            cache.iterateKeys { keys.addIfNotNull(it) }
            cacheLock.write(cache::clear)
            keys
        }
        onChange(keys)
    }

    fun save(virtualFile: VirtualFile, new: ScriptDependencies): Boolean {
        val old = cacheLock.write {
            val old = cache[virtualFile]
            cache.put(virtualFile, new)
            old
        }
        val changed = new != old
        if (changed) {
            onChange(listOf(virtualFile))
        }

        return changed
    }

    fun delete(virtualFile: VirtualFile): Boolean {
        val changed = cacheLock.write {
            cache.remove(virtualFile)
        }
        if (changed) {
            onChange(listOf(virtualFile))
        }
        return changed
    }

    fun combineDependencies(filePredicate: (VirtualFile) -> Boolean): ScriptDependencies = cacheLock.read {
        val sources = mutableListOf<File>()
        val binaries = mutableListOf<File>()
        val imports = mutableListOf<String>()
        val scripts = mutableListOf<File>()

        val relevantEntries = cache.entrySet().filter { filePredicate(it.key) }.map { it.value }
        relevantEntries.forEach {
            sources += it.sources
            binaries += it.classpath
            imports += it.imports
            scripts += it.scripts
        }

        return ScriptDependencies(
            classpath = binaries.distinct(),
            sources = sources.distinct(),
            imports = imports.distinct(),
            scripts = scripts.distinct(),
            javaHome = relevantEntries.map { it.javaHome }.firstOrNull()
        )
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
        lock.read {
            if (value == null) {
                lock.write {
                    value = compute()
                }
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

