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

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.dependencies.FromFileAttributeScriptDependenciesLoader
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptDependenciesLoader
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.findScriptDefinition
import kotlin.script.experimental.dependencies.ScriptDependencies

class ScriptDependenciesUpdater(
    private val project: Project,
    private val cache: ScriptDependenciesCache,
    private val scriptDefinitionProvider: ScriptDefinitionProvider
) {
    private val modifiedScripts = mutableSetOf<VirtualFile>()

    init {
        listenToVfsChanges()
    }

    fun getCurrentDependencies(file: VirtualFile): ScriptDependencies {
        cache[file]?.let { return it }

        val scriptDef = scriptDefinitionProvider.findScriptDefinition(file) ?: return ScriptDependencies.Empty

        FromFileAttributeScriptDependenciesLoader(file, scriptDef, project).updateDependencies()
        ScriptDependenciesLoader.updateDependencies(file, scriptDef, project, shouldNotifyRootsChanged = false)

        return cache[file] ?: ScriptDependencies.Empty
    }

    fun reloadModifiedScripts() {
        for (it in modifiedScripts.filter { cache[it] != null }) {
            val scriptDef = scriptDefinitionProvider.findScriptDefinition(it) ?: return
            ScriptDependenciesLoader.updateDependencies(it, scriptDef, project, shouldNotifyRootsChanged = true)
        }
        modifiedScripts.clear()
    }

    fun requestUpdate(files: Iterable<VirtualFile>) {
        files.forEach { file ->
            if (!file.isValid) {
                cache.delete(file)
            } else if (cache[file] != null) { // only update dependencies for scripts that were touched recently
                modifiedScripts.add(file)
            }
        }
    }

    private fun listenToVfsChanges() {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            val application = ApplicationManager.getApplication()

            override fun after(events: List<VFileEvent>) {
                if (application.isUnitTestMode && application.isScriptDependenciesUpdaterDisabled == true) {
                    return
                }

                val modifiedScripts = events.mapNotNull {
                    // The check is partly taken from the BuildManager.java
                    it.file?.takeIf {
                        // the isUnitTestMode check fixes ScriptConfigurationHighlighting & Navigation tests, since they are not trigger proper update mechanims
                        // TODO: find out the reason, then consider to fix tests and remove this check
                        (application.isUnitTestMode ||
                                scriptDefinitionProvider.isScript(it.name) && projectFileIndex.isInContent(it)) && !isProjectOrWorkspaceFile(it)
                    }
                }
                requestUpdate(modifiedScripts)

                if (KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled) {
                    reloadModifiedScripts()
                }
            }
        })
    }
}

@set: TestOnly
var Application.isScriptDependenciesUpdaterDisabled by NotNullableUserDataProperty(
    Key.create("SCRIPT_DEPENDENCIES_UPDATER_DISABLED"),
    false
)