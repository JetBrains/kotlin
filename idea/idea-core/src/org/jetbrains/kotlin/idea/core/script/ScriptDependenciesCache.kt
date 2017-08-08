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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.NonClasspathDirectoriesScope
import kotlinx.coroutines.experimental.launch
import org.jetbrains.kotlin.idea.core.util.EDT
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.dependencies.ScriptDependencies

internal class ScriptDependenciesCache(private val project: Project) {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptDependencies>()

    operator fun get(virtualFile: VirtualFile): ScriptDependencies? = cacheLock.read { cache[virtualFile.path] }

    val allScriptsClasspathCache = ClearableLazyValue(cacheLock) {
        val files = cache.values.flatMap { it.classpath }.distinct()
        ScriptDependenciesManager.toVfsRoots(files)
    }

    val allScriptsClasspathScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allScriptsClasspathCache.get())
    }

    val allLibrarySourcesCache = ClearableLazyValue(cacheLock) {
        ScriptDependenciesManager.toVfsRoots(cache.values.flatMap { it.sources }.distinct())
    }

    val allLibrarySourcesScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allLibrarySourcesCache.get())
    }

    private fun onChange(file: VirtualFile?) {
        allScriptsClasspathCache.clear()
        allScriptsClasspathScope.clear()
        allLibrarySourcesCache.clear()
        allLibrarySourcesScope.clear()

        val kotlinScriptDependenciesClassFinder =
                Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                        .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                        .single()

        kotlinScriptDependenciesClassFinder.clearCache()
        updateHighlighting(file)
    }

    private fun updateHighlighting(file: VirtualFile?) {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        launch(EDT) {
            if (file != null) {
                file.let { PsiManager.getInstance(project).findFile(it) }?.let { psiFile ->
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
            else {
                assert(ApplicationManager.getApplication().isUnitTestMode)
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }

    fun hasNotCachedRoots(scriptDependencies: ScriptDependencies): Boolean {
        return !allScriptsClasspathCache.get().containsAll(ScriptDependenciesManager.toVfsRoots(scriptDependencies.classpath)) ||
               !allLibrarySourcesCache.get().containsAll(ScriptDependenciesManager.toVfsRoots(scriptDependencies.sources))
    }

    fun clear() {
        cacheLock.write(cache::clear)
        onChange(null)
    }

    fun save(virtualFile: VirtualFile, new: ScriptDependencies): Boolean {
        val path = virtualFile.path
        val old = cacheLock.write {
            val old = cache[path]
            cache[path] = new
            old
        }
        val changed = new != old
        if (changed) {
            onChange(virtualFile)
        }

        return changed
    }

    fun delete(virtualFile: VirtualFile): Boolean {
        val changed = cacheLock.write {
            cache.remove(virtualFile.path) != null
        }
        if (changed) {
            onChange(virtualFile)
        }
        return changed
    }
}

internal class ClearableLazyValue<out T : Any>(private val lock: ReentrantReadWriteLock, private val compute: () -> T) {
    private var value: T? = null

    fun get(): T {
        lock.read {
            if (value == null) {
                lock.write {
                    value = compute()
                }
            }
            return value!!
        }
    }

    fun clear() {
        lock.write {
            value = null
        }
    }
}