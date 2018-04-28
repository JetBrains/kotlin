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
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
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
    private val scriptsQueue = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)
    private val scriptChangesListenerDelay = 1400

    init {
        listenForChangesInScripts()
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

    private fun listenForChangesInScripts() {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                val scriptDef = scriptDefinitionProvider.findScriptDefinition(file) ?: return
                ScriptDependenciesLoader.updateDependencies(file, scriptDef, project, shouldNotifyRootsChanged = true)
            }
        })

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (project.isDisposed) return

                if (ApplicationManager.getApplication().isUnitTestMode && ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled == true) return

                val document = event.document
                val file = FileDocumentManager.getInstance().getFile(document)?.takeIf { it.isInLocalFileSystem } ?: return

                scriptsQueue.cancelAllRequests()

                scriptsQueue.addRequest(
                    {
                        FileDocumentManager.getInstance().saveDocument(document)
                        requestUpdate(listOf(file))

                        if (KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled) {
                            reloadModifiedScripts()
                        }
                    },
                    scriptChangesListenerDelay,
                    true
                )
            }
        }, project.messageBus.connect())
    }
}

@set: TestOnly
var Application.isScriptDependenciesUpdaterDisabled by NotNullableUserDataProperty(
    Key.create("SCRIPT_DEPENDENCIES_UPDATER_DISABLED"),
    false
)