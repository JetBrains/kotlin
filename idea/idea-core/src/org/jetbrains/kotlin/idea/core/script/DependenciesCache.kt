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

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.NonClasspathDirectoriesScope
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.dependencies.ScriptDependencies

internal class DependenciesCache(private val project: Project) {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, ScriptDependencies>()

    operator fun get(virtualFile: VirtualFile): ScriptDependencies? = cacheLock.read { cache[virtualFile.path] }

    val allScriptsClasspathCache = ClearableLazyValue(cacheLock) {
        val files = cache.values.flatMap { it.classpath }.distinct()
        KotlinScriptConfigurationManager.toVfsRoots(files)
    }

    val allScriptsClasspathScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allScriptsClasspathCache.get())
    }

    val allLibrarySourcesCache = ClearableLazyValue(cacheLock) {
        KotlinScriptConfigurationManager.toVfsRoots(cache.values.flatMap { it.sources }.distinct())
    }

    val allLibrarySourcesScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(allLibrarySourcesCache.get())
    }

    fun onChange() {
        allScriptsClasspathCache.clear()
        allScriptsClasspathScope.clear()
        allLibrarySourcesCache.clear()
        allLibrarySourcesScope.clear()

        val kotlinScriptDependenciesClassFinder =
                Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                        .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                        .single()

        kotlinScriptDependenciesClassFinder.clearCache()
    }

    fun clear() {
        cacheLock.write(cache::clear)
        onChange()
    }

    fun save(virtualFile: VirtualFile, new: ScriptDependencies): Boolean {
        val path = virtualFile.path
        val old = cacheLock.write {
            val old = cache[path]
            cache[path] = new
            old
        }
        return old == null || !new.match(old)
    }

    fun delete(virtualFile: VirtualFile): Boolean = cacheLock.write {
        cache.remove(virtualFile.path) != null
    }
}

// TODO: relying on this to compare dependencies seems wrong, doesn't take javaHome and other stuff into account
private fun ScriptDependencies.match(other: ScriptDependencies)
        = classpath.isSamePathListAs(other.classpath) &&
          sources.toSet().isSamePathListAs(other.sources.toSet()) // TODO: gradle returns stdlib and reflect sources in unstable order for some reason


private fun Iterable<File>.isSamePathListAs(other: Iterable<File>): Boolean =
        with(Pair(iterator(), other.iterator())) {
            while (first.hasNext() && second.hasNext()) {
                if (first.next().canonicalPath != second.next().canonicalPath) return false
            }
            !(first.hasNext() || second.hasNext())
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