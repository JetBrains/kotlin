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
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo
import org.jetbrains.kotlin.idea.caches.project.moduleInfo
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.parsing.KotlinParserDefinition.Companion.STD_SCRIPT_EXT
import org.jetbrains.kotlin.parsing.KotlinParserDefinition.Companion.STD_SCRIPT_SUFFIX
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KProperty

class ScratchFileModuleInfoProvider(project: Project) : AbstractProjectComponent(project) {
    private val LOG = Logger.getInstance(this.javaClass)

    override fun projectOpened() {
        myProject.messageBus.connect(myProject).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, ScratchFileModuleListener())
    }

    private inner class ScratchFileModuleListener : FileEditorManagerListener {
        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            if (!file.isValid) return
            if (!ScratchFileService.isInScratchRoot(file)) return

            val ktFile = PsiManager.getInstance(myProject).findFile(file) as? KtFile ?: return

            // Hack before api in IDEA will be introduced
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
                psiFile.moduleInfo = getModuleInfo(module)
                // Drop caches for old module
                ProjectRootManager.getInstance(myProject).incModificationCount()
                // Force re-highlighting
                DaemonCodeAnalyzer.getInstance(myProject).restart(psiFile)

                psiFile.virtualFile.moduleName = module.name
            }

            val module =
                ktFile.virtualFile.moduleName?.let { ModuleManager.getInstance(myProject).findModuleByName(it) }
            if (module != null) {
                scratchPanel?.setModule(module)
            } else {
                val firstModule = ModuleManager.getInstance(myProject).modules.firstOrNull()
                if (firstModule != null) {
                    scratchPanel?.setModule(firstModule)
                }
            }
        }
    }

    private fun getModuleInfo(it: Module?) =
        it?.testSourceInfo() ?: it?.productionSourceInfo() ?: NotUnderContentRootModuleInfo
}

private var VirtualFile.moduleName: String? by ScratchModuleNameProperty()

private val moduleNameAttribute = FileAttribute("ScratchModuleName", 1, false)
private class ScratchModuleNameProperty {

    operator fun getValue(file: VirtualFile, property: KProperty<*>): String? {
        return moduleNameAttribute.readAttributeBytes(file)?.let { String(it) }
    }

    operator fun setValue(file: VirtualFile, property: KProperty<*>, newValue: String?) {
        if (newValue != null) {
            moduleNameAttribute.writeAttributeBytes(file, newValue.toByteArray())
        }
    }
}