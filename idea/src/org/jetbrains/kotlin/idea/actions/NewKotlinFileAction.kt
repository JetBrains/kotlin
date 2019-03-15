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
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.idea.statistics.KotlinEventTrigger
import org.jetbrains.kotlin.idea.statistics.KotlinStatisticsTrigger
import java.util.*

class NewKotlinFileAction : CreateFileFromTemplateAction(
    "Kotlin File/Class",
    "Creates new Kotlin file or class",
    KotlinFileType.INSTANCE.icon
), DumbAware {
    override fun postProcess(createdElement: PsiFile?, templateName: String?, customProperties: Map<String, String>?) {
        super.postProcess(createdElement, templateName, customProperties)

        val module = ModuleUtilCore.findModuleForPsiElement(createdElement!!)

        if (createdElement is KtFile) {
            if (module != null) {
                for (hook in NewKotlinFileHook.EP_NAME.extensions) {
                    hook.postProcess(createdElement, module)
                }
            }

            val ktClass = createdElement.declarations.singleOrNull() as? KtNamedDeclaration
            if (ktClass != null) {
                CreateFromTemplateAction.moveCaretAfterNameIdentifier(ktClass)
            } else {
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

        builder.setValidator(NameValidator)
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

    override fun equals(other: Any?): Boolean {
        return other is NewKotlinFileAction
    }

    override fun startInWriteAction() = false

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) =
        createFileFromTemplateWithStat(name, template, dir)

    companion object {
        private object NameValidator : InputValidatorEx {
            override fun getErrorText(inputString: String): String? {
                if (inputString.trim().isEmpty()) {
                    return "Name can't be empty"
                }

                val parts: List<String> = inputString.split(*FQNAME_SEPARATORS)
                if (parts.any { it.trim().isEmpty() }) {
                    return "Name can't have empty parts"
                }

                return null
            }

            override fun checkInput(inputString: String): Boolean {
                return true
            }

            override fun canClose(inputString: String): Boolean {
                return getErrorText(inputString) == null
            }
        }

        @get:TestOnly
        val nameValidator: InputValidatorEx
            get() = NameValidator

        private fun findOrCreateTarget(dir: PsiDirectory, name: String, directorySeparators: CharArray): Pair<String, PsiDirectory> {
            var className = name.substringBeforeLast(".kt")
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
                CreateFromTemplateDialog(
                    project, dir, template,
                    AttributesDefaults(className).withFixedName(true),
                    properties
                ).create()
            } catch (e: IncorrectOperationException) {
                throw e
            } catch (e: Exception) {
                LOG.error(e)
                return null
            }

            return element?.containingFile
        }

        private val FILE_SEPARATORS = charArrayOf('/', '\\')
        private val FQNAME_SEPARATORS = charArrayOf('/', '\\', '.')

        fun createFileFromTemplateWithStat(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
            KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeNewFileTemplateTrigger, template.name)
            return createFileFromTemplate(name, template, dir)
        }


        fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
            val directorySeparators = if (template.name == "Kotlin File") FILE_SEPARATORS else FQNAME_SEPARATORS
            val (className, targetDir) = findOrCreateTarget(dir, name, directorySeparators)

            val service = DumbService.getInstance(dir.project)
            service.isAlternativeResolveEnabled = true
            try {
                val psiFile = createFromTemplate(targetDir, className, template)
                if (psiFile is KtFile) {
                    val singleClass = psiFile.declarations.singleOrNull() as? KtClass
                    if (singleClass != null && !singleClass.isEnum() && !singleClass.isInterface() && name.contains("Abstract")) {
                        runWriteAction {
                            singleClass.addModifier(KtTokens.ABSTRACT_KEYWORD)
                        }
                    }
                }
                return psiFile
            } finally {
                service.isAlternativeResolveEnabled = false
            }
        }
    }
}

abstract class NewKotlinFileHook {
    companion object {
        val EP_NAME: ExtensionPointName<NewKotlinFileHook> =
            ExtensionPointName.create<NewKotlinFileHook>("org.jetbrains.kotlin.newFileHook")
    }

    abstract fun postProcess(createdElement: KtFile, module: Module)
}
