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
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class KotlinScriptExternalImportsProvider(val project: Project, private val scriptDefinitionProvider: KotlinScriptDefinitionProvider) {
    private val cacheLock = ReentrantReadWriteLock()
    private val cache = hashMapOf<String, KotlinScriptExternalDependencies>()
    private val cacheOfNulls = hashSetOf<String>()

    fun <TF> getExternalImports(vararg files: TF): List<KotlinScriptExternalDependencies> = getExternalImports(files.asIterable())

    fun <TF> getExternalImports(files: Iterable<TF>): List<KotlinScriptExternalDependencies> = cacheLock.read {
        files.mapNotNull { file ->
            val path = getFilePath(file)
            cache[path]
            ?: if (cacheOfNulls.contains(path)) null
               else scriptDefinitionProvider.findScriptDefinition(file)
                    ?.let { it.getDependenciesFor(file, project) }
                    .apply { cacheLock.write {
                        if (this == null) {
                            cacheOfNulls.add(path)
                        }
                        else {
                            cache.put(path, this)
                        }
                    }
            }
        }
    }

    // optimized for initial caching, additional handling of possible duplicates to save a call to distinct
    fun <TF> cacheExternalImports(files: Iterable<TF>): Unit = cacheLock.write {
        val uncached = hashSetOf<String>()
        files.forEach { file ->
            val path = getFilePath(file)
            if (!cache.containsKey(path) && !cacheOfNulls.contains(path) && !uncached.contains(path)) {
                val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
                if (scriptDef != null) {
                    val deps = scriptDef.getDependenciesFor(file, project)
                    if (deps != null) {
                        cache.put(path, deps)
                    }
                    else {
                        cacheOfNulls.add(path)
                    }
                }
                else {
                    uncached.add(path)
                }
            }
        }
    }

    // optimized for update, no special duplicates handling
    fun <TF: Any> updateExternalImportsCache(files: Iterable<TF>): Iterable<TF> = cacheLock.write {
        files.mapNotNull { file ->
            val path = getFilePath(file)
            val scriptDef = scriptDefinitionProvider.findScriptDefinition(file)
            if (scriptDef != null) {
                val deps = scriptDef.getDependenciesFor(file, project)
                val oldDeps = cache[path]
                when {
                    deps != null && (oldDeps == null ||
                                     !deps.classpath.isSameClasspathAs(oldDeps.classpath) || !deps.sources.isSameClasspathAs(oldDeps.sources)) -> {
                        // changed or new
                        cache.put(path, deps)
                        cacheOfNulls.remove(path)
                        file
                    }
                    deps != null -> {
                        // same as before
                        null
                    }
                    else -> {
                        if (cache.remove(path) != null || cacheOfNulls.remove(path)) file // cleared
                        else null // same as before
                    }
                }
            }
            else null // not a script
        }
    }

    fun invalidateCaches() {
        cacheLock.write {
            cache.keys.toList().apply {
                cache.clear()
            }
        }
    }

    fun <TF> invalidateCachesFor(vararg files: TF) { invalidateCachesFor(files.asIterable()) }

    fun <TF> invalidateCachesFor(files: Iterable<TF>) {
        cacheLock.write {
            files.forEach { file ->
                val path = getFilePath(file)
                cache.remove(path)
                cacheOfNulls.remove(path)
            }
        }
    }

    fun getKnownCombinedClasspath(): List<File> = cacheLock.read {
        cache.values.flatMap { it.classpath }
    }.distinct()

    fun <TF> getCombinedClasspathFor(files: Iterable<TF>): List<File> =
        getExternalImports(files)
                .flatMap { it.classpath }
                .distinct()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptExternalImportsProvider? =
                ServiceManager.getService(project, KotlinScriptExternalImportsProvider::class.java)
    }
}