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

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.IndexableSetContributor
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.caches.resolve.FileLibraryScope
import org.jetbrains.kotlin.script.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.read
import kotlin.concurrent.write

@Suppress("unused") // project component
class KotlinScriptConfigurationManager(project: Project,
                                       private val scriptDefinitionProvider: KotlinScriptDefinitionProvider,
                                       private val scriptExtraImportsProvider: KotlinScriptExtraImportsProvider?,
                                       private val kotlinScriptDependenciesIndexableSetContributor: KotlinScriptDependenciesIndexableSetContributor?
) : AbstractProjectComponent(project) {

    private val kotlinEnvVars: Map<String, List<String>> by lazy { generateKotlinScriptClasspathEnvVars(myProject) }

    init {
        reloadScriptDefinitions()

        // TODO: get rid of this expensive call as soon as makeRootsChange call will work reliably
        cacheAllScriptsExtraImports()

        val weakThis = WeakReference(this)
        myProject.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            override fun after(events: List<VFileEvent>) {
                val changedExtraImportConfigs = ArrayList<VirtualFile>()
                var anyScriptDefinitionChanged = false
                events.filter { it is VFileEvent }.forEach {
                    it.file?.let {
                        if (isScriptDefinitionConfigFile(it)) {
                            anyScriptDefinitionChanged = true
                        }
                        else {
                            weakThis.get()?.scriptExtraImportsProvider?.run {
                                if (isExtraImportsConfig(it)) {
                                    changedExtraImportConfigs.add(it)
                                }
                            }
                        }
                    }
                }
                if (anyScriptDefinitionChanged) {
                    weakThis.get()?.reloadScriptDefinitions()
                }
                if (changedExtraImportConfigs.isNotEmpty()) {
                    weakThis.get()?.scriptExtraImportsProvider?.invalidateExtraImportsByImportsFiles(changedExtraImportConfigs)
                }
            }
        })
        // omitting case then scriptExtraImportsProvider is not configured, considering it happens only in tests
        scriptExtraImportsProvider?.subscribeOnExtraImportsChanged { files ->
            weakThis.get()?.apply {
                cacheLock.write {
                    allScriptsClasspathCache = null
                }
                ProjectRootManagerEx.getInstanceEx(myProject)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
            }
        }
    }

    private var allScriptsClasspathCache: List<VirtualFile>? = null
    private val cacheLock = java.util.concurrent.locks.ReentrantReadWriteLock()

    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> =
            scriptExtraImportsProvider
                    ?.getExtraImports(file)
                    ?.flatMap { it.classpath }
                    ?.map { it.classpathEntryToVfs() }
                    ?: emptyList()

    fun getAllScriptsClasspath(): List<VirtualFile> = cacheLock.read {
        if (allScriptsClasspathCache == null) {
            allScriptsClasspathCache =
                    (scriptExtraImportsProvider?.getKnownCombinedClasspath() ?: emptyList())
                    .distinct()
                    .mapNotNull { it.classpathEntryToVfs() }
        }
        return allScriptsClasspathCache!!
    }

    private fun String.classpathEntryToVfs(): VirtualFile =
            if (File(this).isDirectory)
                StandardFileSystems.local()?.findFileByPath(this) ?: throw FileNotFoundException("Classpath entry points to a non-existent location: ${this}")
            else
                StandardFileSystems.jar()?.findFileByPath(this + URLUtil.JAR_SEPARATOR) ?: throw FileNotFoundException("Classpath entry points to a file that is not a JAR archive: ${this}")

    fun getAllScriptsClasspathScope(): GlobalSearchScope? {
        return getAllScriptsClasspath().let { cp ->
            if (cp.isEmpty()) null
            else GlobalSearchScope.union(cp.map { FileLibraryScope(myProject, it) }.toTypedArray())
        }
    }

    private fun reloadScriptDefinitions() {
        loadScriptConfigsFromProjectRoot(File(myProject.basePath ?: ".")).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.setScriptDefinitions(
                        it.map { KotlinConfigurableScriptDefinition(it, kotlinEnvVars) } + StandardScriptDefinition)
            }
        }
    }

    private fun cacheAllScriptsExtraImports() {
        fun<R> VirtualFile.vfsWalkFiles(onFile: (VirtualFile) -> List<R>?): List<R> {
            assert(isDirectory)
            return children.flatMap { when {
                it.isDirectory -> it.vfsWalkFiles(onFile)
                else -> onFile(it) ?: emptyList()
            } }
        }
        myProject.baseDir.vfsWalkFiles {
            scriptExtraImportsProvider?.getExtraImports(it)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptConfigurationManager =
                project.getComponent(KotlinScriptConfigurationManager::class.java)
    }
}


class KotlinScriptDependenciesIndexableSetContributor : IndexableSetContributor() {

    override fun getAdditionalProjectRootsToIndex(project: Project): Set<VirtualFile> {
        return super.getAdditionalProjectRootsToIndex(project) +
            KotlinScriptConfigurationManager.getInstance(project).getAllScriptsClasspath()
    }

    override fun getAdditionalRootsToIndex(): Set<VirtualFile> = emptySet()
}

