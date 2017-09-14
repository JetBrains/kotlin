/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.actions.CreateFromTemplateAction
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.showConfigureKotlinNotificationIfNeeded
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.*

class NewKotlinFileAction
    : CreateFileFromTemplateAction("Kotlin File/Class",
                                   "Creates new Kotlin file or class",
                                   KotlinFileType.INSTANCE.icon),
      DumbAware
{
    override fun postProcess(createdElement: PsiFile?, templateName: String?, customProperties: Map<String, String>?) {
        super.postProcess(createdElement, templateName, customProperties)

        val module = ModuleUtilCore.findModuleForPsiElement(createdElement!!)
        if (module != null) {
            showConfigureKotlinNotificationIfNeeded(module)
        }

        if (createdElement is KtFile) {
            val ktClass = createdElement.declarations.singleOrNull() as? KtNamedDeclaration
            if (ktClass != null) {
                CreateFromTemplateAction.moveCaretAfterNameIdentifier(ktClass)
            }
            else {
                val editor = FileEditorManager.getInstance(createdElement.project).selectedTextEditor ?: return
                if (editor.document == createdElement.viewProvider.document) {
                    val lineCount = editor.document.lineCount
                    if (lineCount > 0) {
                        editor.caretModel.moveToLogicalPosition(LogicalPosition(lineCount - 1, 0))
                    }
                }
            }
        }
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Kotlin File/Class")
                .addKind("File", KotlinFileType.INSTANCE.icon, "Kotlin File")
                .addKind("Class", KotlinIcons.CLASS, "Kotlin Class")
                .addKind("Interface", KotlinIcons.INTERFACE, "Kotlin Interface")
                .addKind("Enum class", KotlinIcons.ENUM, "Kotlin Enum")
                .addKind("Object", KotlinIcons.OBJECT, "Kotlin Object")
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String) = "Kotlin File/Class"

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (super.isAvailable(dataContext)) {
            val ideView = LangDataKeys.IDE_VIEW.getData(dataContext)!!
            val project = PlatformDataKeys.PROJECT.getData(dataContext)!!
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            return ideView.directories.any { projectFileIndex.isInSourceContent(it.virtualFile) }
        }
        return false
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        return obj is NewKotlinFileAction
    }

    override fun startInWriteAction() = false

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) =
            Companion.createFileFromTemplate(name, template, dir)

    companion object {
        private fun findOrCreateTarget(dir: PsiDirectory, name: String, directorySeparators: Array<Char>): Pair<String, PsiDirectory> {
            var className = name.removeSuffix(".kt")
            var targetDir = dir

            for (splitChar in directorySeparators) {
                if (splitChar in className) {
                    val names = className.trim().split(splitChar)

                    for (dirName in names.dropLast(1)) {
                        targetDir = targetDir.findSubdirectory(dirName) ?: runWriteAction {
                            targetDir.createSubdirectory(dirName)
                        }
                    }

                    className = names.last()
                    break
                }
            }
            return Pair(className, targetDir)
        }

        private fun createFromTemplate(dir: PsiDirectory, className: String, template: FileTemplate): PsiFile? {
            val project = dir.project
            val defaultProperties = FileTemplateManager.getInstance(project).defaultProperties

            val properties = Properties(defaultProperties)

            val element = try {
                CreateFromTemplateDialog(project, dir, template,
                                         AttributesDefaults(className).withFixedName(true),
                                         properties).create()
            }
            catch (e: IncorrectOperationException) {
                throw e
            }
            catch (e: Exception) {
                LOG.error(e)
                return null
            }

            return element?.containingFile
        }

        fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
            val directorySeparators = if (template.name == "Kotlin File") arrayOf('/', '\\') else arrayOf('/', '\\', '.')
            val (className, targetDir) = findOrCreateTarget(dir, name, directorySeparators)

            val service = DumbService.getInstance(dir.project)
            service.isAlternativeResolveEnabled = true
            try {
                return createFromTemplate(targetDir, className, template)
            }
            finally {
                service.isAlternativeResolveEnabled = false
            }
        }
    }
}
