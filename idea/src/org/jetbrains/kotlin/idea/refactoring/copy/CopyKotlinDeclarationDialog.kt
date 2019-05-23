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
package org.jetbrains.kotlin.idea.refactoring.copy

import com.intellij.ide.util.DirectoryChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.ui.EditorTextField
import com.intellij.ui.RecentsManager
import com.intellij.ui.ReferenceEditorComboWithBrowseButton
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.refactoring.Pass
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinDestinationFolderComboBox
import org.jetbrains.kotlin.idea.roots.getSuitableDestinationSourceRoots
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.max

// Based on com.intellij.refactoring.copy.CopyClassDialog
class CopyKotlinDeclarationDialog(
        declaration: KtNamedDeclaration,
        private val defaultTargetDirectory: PsiDirectory?,
        private val project: Project
) : DialogWrapper(project, true) {
    private val informationLabel = JLabel()
    private val classNameField = EditorTextField("")
    private val packageLabel = JLabel()
    private lateinit var packageNameField: ReferenceEditorComboWithBrowseButton
    private val openInEditorCheckBox = CopyFilesOrDirectoriesDialog.createOpenInEditorCB()
    private val destinationComboBox = object : KotlinDestinationFolderComboBox() {
        override fun getTargetPackage() = packageNameField.text.trim()
        override fun reportBaseInTestSelectionInSource() = true
    }

    private val originalFile = declaration.containingFile

    var targetDirectory: MoveDestination? = null
        private set

    val targetSourceRoot: VirtualFile?
        get() = ((destinationComboBox.comboBox.selectedItem as? DirectoryChooser.ItemWrapper)?.directory ?: originalFile).sourceRoot

    init {
        informationLabel.text = RefactoringBundle.message("copy.class.copy.0.1", UsageViewUtil.getType(declaration), UsageViewUtil.getLongName(declaration))
        informationLabel.font = informationLabel.font.deriveFont(Font.BOLD)

        init()

        destinationComboBox.setData(
                project,
                defaultTargetDirectory,
                Pass { setErrorText(it, destinationComboBox) },
                packageNameField.childComponent
        )
        classNameField.setText(UsageViewUtil.getShortName(declaration))
        classNameField.selectAll()
    }

    override fun getPreferredFocusedComponent() = classNameField

    override fun createCenterPanel() = JPanel(BorderLayout())

    override fun createNorthPanel(): JComponent? {
        val qualifiedName = qualifiedName
        packageNameField = PackageNameReferenceEditorCombo(qualifiedName, project, RECENTS_KEY, RefactoringBundle.message("choose.destination.package"))
        packageNameField.setTextFieldPreferredWidth(max(qualifiedName.length + 5, 40))
        packageLabel.text = RefactoringBundle.message("destination.package")
        packageLabel.labelFor = packageNameField

        val label = JLabel(RefactoringBundle.message("target.destination.folder"))
        val isMultipleSourceRoots = getSuitableDestinationSourceRoots(project).size > 1
        destinationComboBox.isVisible = isMultipleSourceRoots
        label.isVisible = isMultipleSourceRoots
        label.labelFor = destinationComboBox

        val panel = JPanel(BorderLayout())
        panel.add(openInEditorCheckBox, BorderLayout.EAST)
        return FormBuilder.createFormBuilder()
                .addComponent(informationLabel)
                .addLabeledComponent(RefactoringBundle.message("copy.files.new.name.label"), classNameField, UIUtil.LARGE_VGAP)
                .addLabeledComponent(packageLabel, packageNameField)
                .addLabeledComponent(label, destinationComboBox)
                .addComponent(panel)
                .panel
    }

    private val qualifiedName: String
        get() = defaultTargetDirectory?.getPackage()?.qualifiedName ?: ""

    val newName: String?
        get() = classNameField.text

    val openInEditor: Boolean
        get() = openInEditorCheckBox.isSelected

    private fun checkForErrors(): String? {
        val packageName = packageNameField.text
        val newName = newName

        val manager = PsiManager.getInstance(project)

        if (packageName.isNotEmpty() && !FqNameUnsafe(packageName).hasIdentifiersOnly()) {
            return RefactoringBundle.message("invalid.target.package.name.specified")
        }

        if (newName.isNullOrEmpty()) {
            return RefactoringBundle.message("no.class.name.specified")
        }

        try {
            targetDirectory = destinationComboBox.selectDirectory(PackageWrapper(manager, packageName), false)
        }
        catch (e: IncorrectOperationException) {
            return e.message
        }

        targetDirectory?.getTargetIfExists(defaultTargetDirectory)?.let {
            val targetFileName = newName + "." + originalFile.virtualFile.extension
            if (it.findFile(targetFileName) == originalFile) {
                return "Can't copy class to the containing file"
            }
        }

        return null
    }

    override fun doOKAction() {
        val packageName = packageNameField.text

        val errorString = checkForErrors()
        if (errorString != null) {
            if (errorString.isNotEmpty()) {
                Messages.showMessageDialog(project, errorString, RefactoringBundle.message("error.title"), Messages.getErrorIcon())
            }
            classNameField.requestFocusInWindow()
            return
        }

        RecentsManager.getInstance(project).registerRecentEntry(RECENTS_KEY, packageName)
        CopyFilesOrDirectoriesDialog.saveOpenInEditorState(openInEditorCheckBox.isSelected)

        super.doOKAction()
    }

    override fun getHelpId() = HelpID.COPY_CLASS

    companion object {
        @NonNls private val RECENTS_KEY = "CopyKotlinDeclarationDialog.RECENTS_KEY"
    }
}
