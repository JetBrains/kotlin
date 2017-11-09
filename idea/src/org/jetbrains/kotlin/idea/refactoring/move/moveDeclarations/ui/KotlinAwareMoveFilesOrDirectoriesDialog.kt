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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.ide.util.DirectoryUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.help.HelpManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbModePermission
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.util.Disposer
import com.intellij.psi.*
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.ui.RecentsManager
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.JBLabelDecorator
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
import org.jetbrains.kotlin.idea.refactoring.isInJavaSourceRoot
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class KotlinAwareMoveFilesOrDirectoriesDialog(
        private val project: Project,
        private val callback: (KotlinAwareMoveFilesOrDirectoriesDialog?) -> Unit
) : DialogWrapper(project, true) {
    companion object {
        private val RECENT_KEYS = "MoveFile.RECENT_KEYS"
        private val MOVE_FILES_OPEN_IN_EDITOR = "MoveFile.OpenInEditor"
    }

    private val nameLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true)
    private val targetDirectoryField = TextFieldWithHistoryWithBrowseButton()
    private val openInEditorCb = NonFocusableCheckBox("Open moved files in editor")
    private val updatePackageDirectiveCb = NonFocusableCheckBox()

    private var helpID: String? = null
    var targetDirectory: PsiDirectory? = null
        private set

    init {
        title = RefactoringBundle.message("move.title")
        init()
    }

    val updatePackageDirective: Boolean
        get() = updatePackageDirectiveCb.isSelected

    override fun createActions() = arrayOf(okAction, cancelAction, helpAction)

    override fun getPreferredFocusedComponent() = targetDirectoryField.childComponent

    override fun createCenterPanel() = null

    override fun createNorthPanel(): JComponent {
        RecentsManager.getInstance(project).getRecentEntries(RECENT_KEYS)?.let { targetDirectoryField.childComponent.history = it }

        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        targetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                     RefactoringBundle.message("the.file.will.be.moved.to.this.directory"),
                                                     project,
                                                     descriptor,
                                                     TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT)
        val textField = targetDirectoryField.childComponent.textEditor
        FileChooserFactory.getInstance().installFileCompletion(textField, descriptor, true, disposable)
        textField.document.addDocumentListener(
                object : DocumentAdapter() {
                    override fun textChanged(e: DocumentEvent) {
                        validateOKButton()
                    }
                }
        )
        targetDirectoryField.setTextFieldPreferredWidth(CopyFilesOrDirectoriesDialog.MAX_PATH_LENGTH)
        Disposer.register(disposable, targetDirectoryField)

        openInEditorCb.isSelected = isOpenInEditor()

        val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION))
        return FormBuilder.createFormBuilder()
                .addComponent(nameLabel)
                .addLabeledComponent(RefactoringBundle.message("move.files.to.directory.label"), targetDirectoryField, UIUtil.LARGE_VGAP)
                .addTooltip(RefactoringBundle.message("path.completion.shortcut", shortcutText))
                .addComponentToRightColumn(openInEditorCb, UIUtil.LARGE_VGAP)
                .addComponentToRightColumn(updatePackageDirectiveCb, UIUtil.LARGE_VGAP)
                .panel
    }

    fun setData(psiElements: Array<out PsiElement>, initialTargetDirectory: PsiDirectory?, helpID: String) {
        val psiElement = psiElements.singleOrNull()
        if (psiElement != null) {
            val shortenedPath = CopyFilesOrDirectoriesDialog.shortenPath((psiElement as PsiFileSystemItem).virtualFile)
            nameLabel.text = when (psiElement) {
                is PsiFile -> RefactoringBundle.message("move.file.0", shortenedPath)
                else -> RefactoringBundle.message("move.directory.0", shortenedPath)
            }
        }
        else {
            val isFile = psiElements.all { it is PsiFile }
            val isDirectory = psiElements.all { it is PsiDirectory }
            nameLabel.text = when {
                isFile -> RefactoringBundle.message("move.specified.files")
                isDirectory -> RefactoringBundle.message("move.specified.directories")
                else -> RefactoringBundle.message("move.specified.elements")
            }
        }

        targetDirectoryField.childComponent.text = initialTargetDirectory?.virtualFile?.presentableUrl ?: ""

        validateOKButton()
        this.helpID = helpID

        with (updatePackageDirectiveCb) {
            val jetFiles = psiElements.filterIsInstance<KtFile>().filter(KtFile::isInJavaSourceRoot)
            if (jetFiles.isEmpty()) {
                parent.remove(updatePackageDirectiveCb)
                return
            }

            val singleFile = jetFiles.singleOrNull()
            isSelected = singleFile == null || singleFile.packageMatchesDirectory()
            text = "Update package directive (Kotlin files)"
        }
    }

    override fun doHelpAction() = HelpManager.getInstance().invokeHelp(helpID)

    private fun isOpenInEditor(): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) return false
        return PropertiesComponent.getInstance().getBoolean(MOVE_FILES_OPEN_IN_EDITOR, false)
    }

    private fun validateOKButton() {
        isOKActionEnabled = targetDirectoryField.childComponent.text.length > 0
    }

    override fun doOKAction() {
        PropertiesComponent.getInstance().setValue(MOVE_FILES_OPEN_IN_EDITOR, openInEditorCb.isSelected, false)
        RecentsManager.getInstance(project).registerRecentEntry(RECENT_KEYS, targetDirectoryField.childComponent.text)

        if (DumbService.isDumb(project)) {
            Messages.showMessageDialog(project, "Move refactoring is not available while indexing is in progress", "Indexing", null)
            return
        }

        project.executeCommand(RefactoringBundle.message("move.title"), null) {
            DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_MODAL) {
                runWriteAction {
                    val directoryName = targetDirectoryField.childComponent.text.replace(File.separatorChar, '/')
                    try {
                        targetDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(project), directoryName)
                    }
                    catch (e: IncorrectOperationException) {
                        // ignore
                    }
                }

                if (targetDirectory == null) {
                    CommonRefactoringUtil.showErrorMessage(title,
                                                           RefactoringBundle.message("cannot.create.directory"),
                                                           helpID,
                                                           project)
                    return@allowStartingDumbModeInside
                }

                callback(this@KotlinAwareMoveFilesOrDirectoriesDialog)
            }
        }
    }
}