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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.parsing.KotlinParserDefinition.Companion.STD_SCRIPT_SUFFIX
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.*

class NewKotlinFileAction : CreateFileFromTemplateAction(
    KotlinBundle.message("action.new.file.text"),
    KotlinBundle.message("action.new.file.description"),
    KotlinFileType.INSTANCE.icon
), DumbAware {
    override fun postProcess(createdElement: PsiFile, templateName: String?, customProperties: Map<String, String>?) {
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
        builder.setTitle(KotlinBundle.message("action.new.file.dialog.title"))
            .addKind(
                KotlinBundle.message("action.new.file.dialog.file.title"),
                KotlinFileType.INSTANCE.icon,
                "Kotlin File"
            )
            .addKind(
                KotlinBundle.message("action.new.file.dialog.class.title"),
                KotlinIcons.CLASS,
                "Kotlin Class"
            )
            .addKind(
                KotlinBundle.message("action.new.file.dialog.interface.title"),
                KotlinIcons.INTERFACE,
                "Kotlin Interface"
            )
            .addKind(
                KotlinBundle.message("action.new.file.dialog.enum.title"),
                KotlinIcons.ENUM,
                "Kotlin Enum"
            )
            .addKind(
                KotlinBundle.message("action.new.file.dialog.object.title"),
                KotlinIcons.OBJECT,
                "Kotlin Object"
            )

        builder.setValidator(NameValidator)
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String =
        KotlinBundle.message("action.new.file.text")

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (super.isAvailable(dataContext)) {
            val ideView = LangDataKeys.IDE_VIEW.getData(dataContext)!!
            val project = PlatformDataKeys.PROJECT.getData(dataContext)!!
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            return ideView.directories.any { projectFileIndex.isInSourceContent(it.virtualFile) }
        }

        return false
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other is NewKotlinFileAction

    override fun startInWriteAction() = false

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) =
        createFileFromTemplateWithStat(name, template, dir)

    companion object {
        private object NameValidator : InputValidatorEx {
            override fun getErrorText(inputString: String): String? {
                if (inputString.trim().isEmpty()) {
                    return KotlinBundle.message("action.new.file.error.empty.name")
                }

                val parts: List<String> = inputString.split(*FQNAME_SEPARATORS)
                if (parts.any { it.trim().isEmpty() }) {
                    return KotlinBundle.message("action.new.file.error.empty.name.part")
                }

                return null
            }

            override fun checkInput(inputString: String): Boolean = true

            override fun canClose(inputString: String): Boolean = getErrorText(inputString) == null
        }

        @get:TestOnly
        val nameValidator: InputValidatorEx
            get() = NameValidator

        private fun findOrCreateTarget(dir: PsiDirectory, name: String, directorySeparators: CharArray): Pair<String, PsiDirectory> {
            var className = removeKotlinExtensionIfPresent(name)
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

        private fun removeKotlinExtensionIfPresent(name: String): String = when {
            name.endsWith(".$KOTLIN_WORKSHEET_EXTENSION") -> name.removeSuffix(".$KOTLIN_WORKSHEET_EXTENSION")
            name.endsWith(".$STD_SCRIPT_SUFFIX") -> name.removeSuffix(".$STD_SCRIPT_SUFFIX")
            name.endsWith(".${KotlinFileType.EXTENSION}") -> name.removeSuffix(".${KotlinFileType.EXTENSION}")
            else -> name
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
            KotlinFUSLogger.log(FUSEventGroups.NewFileTemplate, template.name)
            return createFileFromTemplate(name, template, dir)
        }


        fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
            val directorySeparators = when (template.name) {
                "Kotlin File" -> FILE_SEPARATORS
                else -> FQNAME_SEPARATORS
            }
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
