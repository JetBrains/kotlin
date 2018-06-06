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

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinFileType
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

fun ScriptDefinitionProvider.findScriptDefinition(file: VirtualFile): KotlinScriptDefinition? =
    if (file.isDirectory) null
    else findScriptDefinition(file.name)


fun getScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? =
    if (file.isDirectory) null
    else ScriptDefinitionProvider.getInstance(project).findScriptDefinition(file)

fun getScriptDefinition(psiFile: PsiFile): KotlinScriptDefinition? =
    if (psiFile.isDirectory) null
    else ScriptDefinitionProvider.getInstance(psiFile.project).findScriptDefinition(psiFile.name)

abstract class LazyScriptDefinitionProvider : ScriptDefinitionProvider {

    protected val lock = ReentrantReadWriteLock()

    protected abstract val currentDefinitions: Sequence<KotlinScriptDefinition>

    private var _cachedDefinitions: Sequence<KotlinScriptDefinition>? = null
    private val cachedDefinitions: Sequence<KotlinScriptDefinition>
        get() {
            assert(lock.readLockCount > 0) { "cachedDefinitions should only be used under the read lock" }
            if (_cachedDefinitions == null) lock.write {
                _cachedDefinitions = CachingSequence(currentDefinitions.constrainOnce())
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

    override fun isScript(fileName: String) = findScriptDefinition(fileName) != null

    companion object {
        // TODO: find a common place for storing kotlin-related extensions and reuse values from it everywhere
        protected val nonScriptFilenameSuffixes = arrayOf(".${KotlinFileType.EXTENSION}", ".${JavaFileType.DEFAULT_EXTENSION}")
    }
}

private class CachingSequence<T>(from: Sequence<T>) : Sequence<T> {

    private val lock = ReentrantReadWriteLock()
    private val sequenceIterator = from.iterator()
    private val cache = arrayListOf<T>()

    private inner class CachingIterator : Iterator<T> {

        private var cacheCursor = 0

        override fun hasNext(): Boolean = lock.read { cacheCursor < cache.size || sequenceIterator.hasNext() }

        override fun next(): T = lock.write {
            if (cacheCursor < cache.size) cache[cacheCursor++]
            else sequenceIterator.next().also { cache.add(it) }
        }
    }

    override fun iterator(): Iterator<T> = CachingIterator()
}
