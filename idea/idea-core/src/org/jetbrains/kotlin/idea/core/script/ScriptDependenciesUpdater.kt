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
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.Alarm
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.script.dependencies.AsyncScriptDependenciesLoader
import org.jetbrains.kotlin.idea.core.script.dependencies.FromFileAttributeScriptDependenciesLoader
import org.jetbrains.kotlin.idea.core.script.dependencies.SyncScriptDependenciesLoader
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.LegacyResolverWrapper
import org.jetbrains.kotlin.script.findScriptDefinition
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies

class ScriptDependenciesUpdater(
    private val project: Project,
    private val cache: ScriptDependenciesCache
) {
    private val scriptsQueue = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)
    private val scriptChangesListenerDelay = 1400

    private val asyncLoader = AsyncScriptDependenciesLoader(project)
    private val syncLoader = SyncScriptDependenciesLoader(project)
    private val fileAttributeLoader = FromFileAttributeScriptDependenciesLoader(project)

    init {
        listenForChangesInScripts()
    }

    fun getCurrentDependencies(file: VirtualFile): ScriptDependencies {
        cache[file]?.let { return it }

        val scriptDef = findScriptDefinition(file, project) ?: return ScriptDependencies.Empty

        fileAttributeLoader.updateDependencies(file, scriptDef)

        updateDependencies(file, scriptDef)

        return cache[file] ?: ScriptDependencies.Empty
    }

    private fun updateDependencies(file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        val loader = when (scriptDef.dependencyResolver) {
            is AsyncDependenciesResolver, is LegacyResolverWrapper -> asyncLoader
            else -> syncLoader
        }
        loader.updateDependencies(file, scriptDef)
    }

    private fun listenForChangesInScripts() {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                runScriptDependenciesUpdateIfNeeded(file)
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                event.newFile?.let { runScriptDependenciesUpdateIfNeeded(it) }
            }

            private fun runScriptDependenciesUpdateIfNeeded(file: VirtualFile) {
                if (file.fileType != KotlinFileType.INSTANCE) return
                val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return

                if (ApplicationManager.getApplication().isUnitTestMode && ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled == true) return

                val scriptDef = ktFile.script?.kotlinScriptDefinition ?: return

                if (!ProjectRootsUtil.isInProjectSource(ktFile, includeScriptsOutsideSourceRoots = true)) return

                updateDependencies(file, scriptDef)
            }
        })

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (project.isDisposed) return

                if (ApplicationManager.getApplication().isUnitTestMode && ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled == true) return

                val document = event.document
                val file = FileDocumentManager.getInstance().getFile(document)?.takeIf { it.isInLocalFileSystem } ?: return
                if (!file.isValid) {
                    cache.delete(file)
                    return
                }

                // only update dependencies for scripts that were touched recently
                if (cache[file] == null) {
                    return
                }

                val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return
                val scriptDef = ktFile.script?.kotlinScriptDefinition ?: return

                if (!ProjectRootsUtil.isInProjectSource(ktFile, includeScriptsOutsideSourceRoots = true)) return

                scriptsQueue.cancelAllRequests()

                scriptsQueue.addRequest(
                    {
                        FileDocumentManager.getInstance().saveDocument(document)
                        updateDependencies(file, scriptDef)
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