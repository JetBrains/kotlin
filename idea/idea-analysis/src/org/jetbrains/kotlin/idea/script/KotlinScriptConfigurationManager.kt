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
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
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

@Suppress("unused") // project component
class KotlinScriptConfigurationManager(private val project: Project,
                                       private val scriptDefinitionProvider: KotlinScriptDefinitionProvider,
                                       private val scriptExtraImportsProvider: KotlinScriptExtraImportsProvider?
) : AbstractProjectComponent(project) {

    private val kotlinEnvVars: Map<String, List<String>> by lazy { generateKotlinScriptClasspathEnvVarsForIdea(myProject) }

    init {
        reloadScriptDefinitions()
        val conn = myProject.messageBus.connect()
        conn.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            override fun after(events: List<VFileEvent>) {
                var anyScriptDefinitionChanged = false
                events.filter { it is VFileEvent }.forEach {
                    it.file?.let {
                        if (!anyScriptDefinitionChanged && isScriptDefinitionConfigFile(it)) {
                            anyScriptDefinitionChanged = true
                        }
                        scriptExtraImportsProvider?.run {
                            if (isExtraImportsConfig(it)) {
                                invalidateExtraImports(it)
                            }
                        }
                    }
                }
                if (anyScriptDefinitionChanged) {
                    reloadScriptDefinitions()
                }
            }
        })
    }

    // TODO: cache
    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> =
        getScriptClasspathRaw(file)
                .mapNotNull { StandardFileSystems.local().findFileByPath(it) }
                .distinct()

    // TODO: cache
    fun getAllScriptsClasspath(): List<VirtualFile> {
        fun<R> VirtualFile.vfsWalkFiles(onFile: (VirtualFile) -> List<R>?): List<R> {
            assert(isDirectory)
            return children.flatMap { when {
                it.isDirectory -> it.vfsWalkFiles(onFile)
                else -> onFile(it) ?: emptyList()
            } }
        }
        return project.baseDir.vfsWalkFiles { getScriptClasspathRaw(it) }
                .distinct()
                .mapNotNull {
                    if (File(it).isDirectory)
                        StandardFileSystems.local()?.findFileByPath(it) ?: throw FileNotFoundException("Classpath entry points to a non-existent location: $it")
                    else
                        StandardFileSystems.jar()?.findFileByPath(it + URLUtil.JAR_SEPARATOR) ?: throw FileNotFoundException("Classpath entry points to a file that is not a JAR archive: $it")

                }
    }

    fun getAllScriptsClasspathScope(): GlobalSearchScope =
            GlobalSearchScope.union(getAllScriptsClasspath().map { FileLibraryScope(project, it) }.toTypedArray())

    private fun getScriptClasspathRaw(file: VirtualFile): List<String> =
            scriptDefinitionProvider.findScriptDefinition(file)?.getScriptDependenciesClasspath()?.let {
                it + (scriptExtraImportsProvider?.getExtraImports(file)?.flatMap { it.classpath } ?: emptyList())
            } ?: emptyList()

    private fun reloadScriptDefinitions() {
        loadScriptConfigsFromProjectRoot(File(myProject.basePath ?: ".")).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.scriptDefinitions =
                        it.map { KotlinConfigurableScriptDefinition(it, kotlinEnvVars) } + StandardScriptDefinition
            }
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptConfigurationManager =
                project.getComponent(KotlinScriptConfigurationManager::class.java)
    }
}


class KotlinScriptDependenciesIndexableSetContributor : IndexableSetContributor() {

    override fun getAdditionalProjectRootsToIndex(project: Project): Set<VirtualFile> =
            super.getAdditionalProjectRootsToIndex(project) +
                KotlinScriptConfigurationManager.getInstance(project).getAllScriptsClasspath()

    override fun getAdditionalRootsToIndex(): Set<VirtualFile> = emptySet()
}

