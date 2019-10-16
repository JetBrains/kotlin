/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.ui.components.JBLabelDecorator
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.core.util.onTextChange
import java.io.File
import javax.swing.InputVerifier
import javax.swing.JComponent

class MoveRefactoringActionDialog(
    private val project: Project, private val defaultDirectory: String
) : DialogWrapper(project, true) {

    companion object {
        const val WINDOW_TITLE = "Move refactoring test"
        const val COUNT_LABEL_TEXT = "Maximum count of applied refactoring before validity check"
        const val LOG_FILE_WILL_BE_PLACED_HERE = "Test result log file will be placed here"

        const val RECENT_SELECTED_PATH = "org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.RECENT_SELECTED_PATH"
        const val RECENT_SELECTED_RUN_COUNT = "org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.RECENT_SELECTED_RUN_COUNT"
    }

    private val nameLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true)
    private val tfTargetDirectory = TextFieldWithBrowseButton()
    private val tfRefactoringRunCount = JBTextField()

    init {
        title = WINDOW_TITLE
        init()
        initializeData()
    }

    override fun createActions() = arrayOf(okAction, cancelAction)

    override fun getPreferredFocusedComponent() = tfTargetDirectory.childComponent

    override fun createCenterPanel(): JComponent? = null

    override fun createNorthPanel(): JComponent {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        tfTargetDirectory.addBrowseFolderListener(
            WINDOW_TITLE,
            LOG_FILE_WILL_BE_PLACED_HERE,
            project,
            descriptor
        )


        tfTargetDirectory.setTextFieldPreferredWidth(CopyFilesOrDirectoriesDialog.MAX_PATH_LENGTH)
        tfTargetDirectory.textField.onTextChange { validateOKButton() }
        Disposer.register(disposable, tfTargetDirectory)

        tfRefactoringRunCount.inputVerifier = object : InputVerifier() {
            override fun verify(input: JComponent?) = tfRefactoringRunCount.text.toIntOrNull()?.let { it > 0 } ?: false
        }

        tfRefactoringRunCount.onTextChange { validateOKButton() }

        return FormBuilder.createFormBuilder()
            .addComponent(nameLabel)
            .addLabeledComponent(LOG_FILE_WILL_BE_PLACED_HERE, tfTargetDirectory, UIUtil.LARGE_VGAP)
            .addLabeledComponent(COUNT_LABEL_TEXT, tfRefactoringRunCount)
            .panel
    }

    private fun initializeData() {
        tfTargetDirectory.childComponent.text = PropertiesComponent.getInstance().getValue(RECENT_SELECTED_PATH, defaultDirectory)
        tfRefactoringRunCount.text =
            PropertiesComponent.getInstance().getValue(RECENT_SELECTED_RUN_COUNT, "1")
                .toIntOrNull()
                ?.let { if (it < 1) "1" else it.toString() }

        validateOKButton()
    }

    private fun validateOKButton() {

        val isCorrectCount = tfRefactoringRunCount.text.toIntOrNull()?.let { it > 0 } ?: false
        if (!isCorrectCount) {
            isOKActionEnabled = false
            return
        }

        val isCorrectPath = tfTargetDirectory.childComponent.text
            ?.let { it.isNotEmpty() && File(it).let { path -> path.exists() && path.isDirectory } }
            ?: false

        isOKActionEnabled = isCorrectPath
    }

    val selectedDirectoryName get() = tfTargetDirectory.childComponent.text!!

    val selectedCount get() = tfRefactoringRunCount.text.toInt()

    override fun doOKAction() {

        PropertiesComponent.getInstance().setValue(RECENT_SELECTED_PATH, selectedDirectoryName)
        PropertiesComponent.getInstance().setValue(RECENT_SELECTED_RUN_COUNT, tfRefactoringRunCount.text)

        close(OK_EXIT_CODE, /* isOk = */ true)
    }
}