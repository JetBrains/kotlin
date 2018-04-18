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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface ScriptDefinitionProvider {
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?
    fun isScript(fileName: String): Boolean

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java)
    }
}

fun ScriptDefinitionProvider.findScriptDefinition(file: VirtualFile): KotlinScriptDefinition? = findScriptDefinition(file.name)

fun getScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? =
    ScriptDefinitionProvider.getInstance(project).findScriptDefinition(file)

fun getScriptDefinition(psiFile: PsiFile): KotlinScriptDefinition? =
    ScriptDefinitionProvider.getInstance(psiFile.project).findScriptDefinition(psiFile.name)

abstract class LazyScriptDefinitionProvider : ScriptDefinitionProvider {

    protected val lock = ReentrantReadWriteLock()

    protected abstract val currentDefinitions: Sequence<KotlinScriptDefinition>

    private var _cachedDefinitions: Sequence<KotlinScriptDefinition>? = null
    private val cachedDefinitions: Sequence<KotlinScriptDefinition>
        get() {
            // assuming it is always called under read lock
            assert(lock.readLockCount > 0)
            if (_cachedDefinitions == null) lock.write {
                _cachedDefinitions = CashingSequence(currentDefinitions.constrainOnce())
            }
            return _cachedDefinitions!!
        }

    protected fun clearCache() {
        lock.write {
            _cachedDefinitions = null
        }
    }

    protected open fun nonScriptFileName(fileName: String) = nonScriptFilenameSuffixes.any {
        fileName.endsWith( it, ignoreCase = true)
    }

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? =
        if (nonScriptFileName(fileName)) null
        else lock.read {
            cachedDefinitions.firstOrNull { it.isScript(fileName) }
        }

    override fun isScript(fileName: String) =
        if (nonScriptFileName(fileName)) false
        else lock.read {
            cachedDefinitions.any { it.isScript(fileName) }
        }

    companion object {
        // TODO: find a common place for storing kotlin-related extensions and reuse values from it everywhere
        protected val nonScriptFilenameSuffixes = arrayOf(".kt", ".java")
    }
}

private class CashingSequence<T>(from: Sequence<T>) : Sequence<T> {

    private val lock = ReentrantReadWriteLock()
    private val sequenceIterator = from.iterator()
    private val cache = arrayListOf<T>()

    private inner class CashingIterator : Iterator<T> {

        private val cacheIterator: Iterator<T> = cache.iterator()
        private var cacheRunOut = !cacheIterator.hasNext()

        private fun cacheHasNext() = !cacheRunOut && (cacheIterator.hasNext().also { if (!it) cacheRunOut = true })

        override fun hasNext(): Boolean = lock.read { cacheHasNext() || sequenceIterator.hasNext() }

        override fun next(): T = lock.read {
            if (cacheHasNext()) cacheIterator.next()
            else sequenceIterator.next().also { lock.write { cache.add(it) } }
        }
    }

    override fun iterator(): Iterator<T> = CashingIterator()
}
