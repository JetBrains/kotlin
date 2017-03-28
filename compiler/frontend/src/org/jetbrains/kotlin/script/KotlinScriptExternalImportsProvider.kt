/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class KotlinScriptExternalImportsProvider(val project: Project, private val scriptDefinitionProvider: KotlinScriptDefinitionProvider) {

    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, KotlinScriptExternalDependencies?>()

    fun <TF: Any> getExternalImports(file: TF): KotlinScriptExternalDependencies? = cacheLock.read { calculateExternalDependencies(file) }

    fun <TF: Any> getExternalImports(files: Iterable<TF>): List<KotlinScriptExternalDependencies> = cacheLock.read {
        files.mapNotNull { calculateExternalDependencies(it) }
    }

    private fun <TF: Any> calculateExternalDependencies(file: TF): KotlinScriptExternalDependencies? {
        val path = getFilePath(file)
        val cached = cache[path]
        return if (cached != null) cached else {
            val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
            if (scriptDef != null) {
                val deps = scriptDef.getDependenciesFor(file, project, null)
                if (deps != null) {
                    log.info("[kts] new cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                }
                cacheLock.write {
                    cache.put(path, deps)
                }
                deps
            }
            else null
        }
    }

    // optimized for initial caching, additional handling of possible duplicates to save a call to distinct
    // returns list of cached files
    fun <TF: Any> cacheExternalImports(files: Iterable<TF>): Iterable<TF> = cacheLock.write {
        val uncached = hashSetOf<String>()
        files.mapNotNull { file ->
            val path = getFilePath(file)
            if (isValidFile(file) && !cache.containsKey(path) && !uncached.contains(path)) {
                val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
                if (scriptDef != null) {
                    val deps = scriptDef.getDependenciesFor(file, project, null)
                    if (deps != null) {
                        log.info("[kts] cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                    }
                    cache.put(path, deps)
                    file
                }
                else {
                    uncached.add(path)
                    null
                }
            }
            else null
        }
    }

    // optimized for update, no special duplicates handling
    // returns files with valid script definition (or deleted from cache - which in fact should have script def too)
    // TODO: this is the badly designed contract, since it mixes the entities, but these files are needed on the calling site now. Find out other solution
    fun <TF: Any> updateExternalImportsCache(files: Iterable<TF>): Iterable<TF> = cacheLock.write {
        files.mapNotNull { file ->
            val path = getFilePath(file)
            if (!isValidFile(file)) {
                if (cache.remove(path) != null) {
                    log.debug("[kts] removed deps for file $path")
                    file
                } // cleared
                else {
                    null // unknown
                }
            }
            else {
                val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
                if (scriptDef != null) {
                    val oldDeps = cache[path]
                    val deps = scriptDef.getDependenciesFor(file, project, oldDeps)
                    when {
                        deps != null && (oldDeps == null ||
                                         !deps.classpath.isSamePathListAs(oldDeps.classpath) || !deps.sources.isSamePathListAs(oldDeps.sources)) -> {
                            // changed or new
                            log.info("[kts] updated/new cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                            cache.put(path, deps)
                        }
                        deps != null -> {
                            // same as before
                        }
                        else -> {
                            if (cache.remove(path) != null) {
                                log.debug("[kts] removed deps for $path")
                            } // cleared
                        }
                    }
                    file
                }
                else null // not a script
            }
        }
    }

    fun invalidateCaches() {
        cacheLock.write(cache::clear)
    }

    fun getKnownCombinedClasspath(): List<File> = cacheLock.read {
        cache.values.flatMap { it?.classpath ?: emptyList() }
    }.distinct()

    fun getKnownSourceRoots(): List<File> = cacheLock.read {
        cache.values.flatMap { it?.sources ?: emptyList() }
    }.distinct()

    fun <TF: Any> getCombinedClasspathFor(files: Iterable<TF>): List<File> =
        getExternalImports(files)
                .flatMap { it.classpath }
                .distinct()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptExternalImportsProvider? =
                ServiceManager.getService(project, KotlinScriptExternalImportsProvider::class.java)
        internal val log = Logger.getInstance(KotlinScriptExternalImportsProvider::class.java)
    }
}

internal fun Iterable<File>.isSamePathListAs(other: Iterable<File>): Boolean =
        with (Pair(iterator(), other.iterator())) {
            while (first.hasNext() && second.hasNext()) {
                if (first.next().canonicalPath != second.next().canonicalPath) return false
            }
            !(first.hasNext() || second.hasNext())
        }

fun getScriptExternalDependencies(file: VirtualFile, project: Project): KotlinScriptExternalDependencies?  =
        KotlinScriptExternalImportsProvider.getInstance(project)?.getExternalImports(file)
