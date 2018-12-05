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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.scriptRelatedModuleName
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.parsing.KotlinParserDefinition.Companion.STD_SCRIPT_EXT
import org.jetbrains.kotlin.parsing.KotlinParserDefinition.Companion.STD_SCRIPT_SUFFIX
import org.jetbrains.kotlin.psi.KtFile

class ScratchFileModuleInfoProvider(val project: Project) : ProjectComponent {
    private val LOG = Logger.getInstance(this.javaClass)

    override fun projectOpened() {
        project.messageBus.connect(project).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, ScratchFileModuleListener())
    }

    private inner class ScratchFileModuleListener : FileEditorManagerListener {
        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            if (!file.isValid) return
            if (!ScratchFileService.isInScratchRoot(file)) return

            val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return

            // BUNCH: 181 scratch files are created with .kt extension
            if (file.extension == KotlinFileType.EXTENSION) {
                runWriteAction {
                    var newName = file.nameWithoutExtension + STD_SCRIPT_EXT
                    var i = 1
                    while (file.parent.findChild(newName) != null) {
                        newName = file.nameWithoutExtension + "_" + i + STD_SCRIPT_EXT
                        i++
                    }
                    file.rename(this, newName)
                }
            }

            if (file.extension != STD_SCRIPT_SUFFIX) {
                LOG.error("Kotlin Scratch file should have .kts extension. Cannot add scratch panel for ${file.path}")
                return
            }

            val scratchPanel = getEditorWithScratchPanel(source, file)?.second
            scratchPanel?.addModuleListener { psiFile, module ->
                psiFile.virtualFile.scriptRelatedModuleName = module?.name

                // Drop caches for old module
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
                // Force re-highlighting
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }

            val module = ktFile.virtualFile.scriptRelatedModuleName?.let { ModuleManager.getInstance(project).findModuleByName(it) }
            if (module != null) {
                scratchPanel?.setModule(module)
            }
        }
    }
}