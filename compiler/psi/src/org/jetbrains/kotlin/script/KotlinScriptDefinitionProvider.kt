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

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface ScriptDefinitionProvider {
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?
    fun isScript(fileName: String): Boolean
    fun getDefaultScriptDefinition(): KotlinScriptDefinition

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java)
    }
}

fun findScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? {
    if (file.isDirectory ||
        file.extension == KotlinFileType.EXTENSION ||
        file.extension == JavaClassFileType.INSTANCE.defaultExtension ||
        !isKotlinFileType(file)
    ) {
        return null
    }

    val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project)
    val psiFile = PsiManager.getInstance(project).findFile(file)
    if (psiFile != null) {
        if (psiFile !is KtFile) return null

        // Do not use psiFile.script here because this method can be called during indexes access
        // and accessing stubs may cause deadlock
        // If script definition cannot be find, default script definition is used
        // because all KtFile-s with KotlinFileType and non-kts extensions are parsed as scripts
        val definition = scriptDefinitionProvider.findScriptDefinition(file.name)
        return definition ?: scriptDefinitionProvider.getDefaultScriptDefinition()
    }

    return scriptDefinitionProvider.findScriptDefinition(file.name)
}

private fun isKotlinFileType(file: VirtualFile): Boolean {
    val typeRegistry = FileTypeRegistry.getInstance()
    return typeRegistry.getFileTypeByFile(file) == KotlinFileType.INSTANCE ||
            typeRegistry.getFileTypeByFileName(file.name) == KotlinFileType.INSTANCE
}

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
        fileName.endsWith(it, ignoreCase = true)
    }

    override fun findScriptDefinition(fileName: String): KotlinScriptDefinition? =
        if (nonScriptFileName(fileName)) null
        else lock.read {
            cachedDefinitions.firstOrNull { it.isScript(fileName) }
        }

    override fun isScript(fileName: String) = findScriptDefinition(fileName) != null

    override fun getKnownFilenameExtensions(): Sequence<String> = lock.read {
        cachedDefinitions.map { it.fileExtension }
    }

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

        override fun hasNext(): Boolean =
            lock.read { cacheCursor < cache.size }
                    // iterator's hasNext can mutate the iterator's state, therefore write lock is needed
                    || lock.write { cacheCursor < cache.size || sequenceIterator.hasNext() }

        override fun next(): T {
            lock.read {
                if (cacheCursor < cache.size) return cache[cacheCursor++]
            }
            // lock.write is not an upgrade but retake, therefore - one more check needed
            lock.write {
                return if (cacheCursor < cache.size) cache[cacheCursor++]
                else sequenceIterator.next().also { cache.add(it) }
            }
        }
    }

    override fun iterator(): Iterator<T> = CachingIterator()
}
