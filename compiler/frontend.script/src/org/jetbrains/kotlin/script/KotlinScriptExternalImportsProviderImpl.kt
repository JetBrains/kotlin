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

package org.jetbrains.kotlin.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.dependencies.KotlinScriptExternalDependencies

class KotlinScriptExternalImportsProviderImpl(
        val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) : KotlinScriptExternalImportsProvider {

    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, KotlinScriptExternalDependencies?>()

    override fun <TF: Any> getExternalImports(file: TF): KotlinScriptExternalDependencies? = cacheLock.read {
        calculateExternalDependencies(file)
    }

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
    override fun <TF: Any> cacheExternalImports(files: Iterable<TF>): Iterable<TF> = cacheLock.write {
        var filesCount = 0
        var additionsCount = 0
        val (res, time) = measureThreadTimeMillis {
            files.mapNotNull { file ->
                filesCount += 1
                val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
                if (scriptDef != null) {
                    val path = getFilePath(file)
                    if (isValidFile(file) && !cache.containsKey(path)) {
                        val deps = scriptDef.getDependenciesFor(file, project, null)
                        cache.put(path, deps)
                        if (deps != null) {
                            log.info("[kts] cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                            additionsCount += 1
                            file
                        }
                        else null
                    }
                    else null
                }
                else null
            }
        }
        log.info("[kts] cache creation: $filesCount checked, $additionsCount added (in ${time}ms)")
        res
    }

    // optimized for update, no special duplicates handling
    // returns files with valid script definition (or deleted from cache - which in fact should have script def too)
    // TODO: this is the badly designed contract, since it mixes the entities, but these files are needed on the calling site now. Find out other solution
    override fun <TF: Any> updateExternalImportsCache(files: Iterable<TF>): Iterable<TF> = cacheLock.write {
        var filesCount = 0
        var updatesCount = 0
        val (res, time) = measureThreadTimeMillis {
            files.mapNotNull { file ->
                filesCount += 1
                val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
                if (scriptDef != null) {
                    val path = getFilePath(file)
                    if (!isValidFile(file)) {
                        if (cache.remove(path) != null) {
                            log.debug("[kts] removed deps for file $path")
                            updatesCount += 1
                            file
                        } // cleared
                        else null // unknown
                    }
                    else {
                        val oldDeps = cache[path]
                        val deps = scriptDef.getDependenciesFor(file, project, oldDeps)
                        when {
                            deps != null && (oldDeps == null ||
                                             !deps.classpath.isSamePathListAs(oldDeps.classpath) || !deps.sources.isSamePathListAs(oldDeps.sources)) -> {
                                // changed or new
                                log.info("[kts] updated/new cached deps for $path: ${deps.classpath.joinToString(File.pathSeparator)}")
                                cache.put(path, deps)
                                updatesCount += 1
                                file
                            }
                            deps != null -> {
                                // same as before
                                null
                            }
                            cache.remove(path) != null -> {
                                log.debug("[kts] removed deps for $path")
                                updatesCount += 1
                                file
                            }
                            else -> null // unknown
                        }
                    }
                }
                else null // not a script
            }
        }
        if (updatesCount > 0) {
            log.info("[kts] cache update check: $filesCount checked, $updatesCount updated (in ${time}ms)")
        }
        res
    }

    override fun invalidateCaches() {
        cacheLock.write(cache::clear)
    }

    override fun getKnownCombinedClasspath(): List<File> = cacheLock.read {
        cache.values.flatMap { it?.classpath ?: emptyList() }
    }.distinct()

    override fun getKnownSourceRoots(): List<File> = cacheLock.read {
        cache.values.flatMap { it?.sources ?: emptyList() }
    }.distinct()

    override fun <TF: Any> getCombinedClasspathFor(files: Iterable<TF>): List<File> =
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

private fun Iterable<File>.isSamePathListAs(other: Iterable<File>): Boolean =
        with (Pair(iterator(), other.iterator())) {
            while (first.hasNext() && second.hasNext()) {
                if (first.next().canonicalPath != second.next().canonicalPath) return false
            }
            !(first.hasNext() || second.hasNext())
        }

private inline fun<T> measureThreadTimeMillis(body: () -> T): Pair<T, Long> {
    val mxBeans = ManagementFactory.getThreadMXBean()
    val startTime = mxBeans.currentThreadCpuTime
    val res = body()
    return res to TimeUnit.NANOSECONDS.toMillis(mxBeans.currentThreadCpuTime - startTime)
}
