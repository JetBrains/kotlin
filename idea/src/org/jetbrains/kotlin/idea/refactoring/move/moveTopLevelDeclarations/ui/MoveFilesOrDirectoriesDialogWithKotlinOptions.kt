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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.ui

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesDialog
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.packageMatchesDirectory
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.properties.Delegates

public class MoveFilesOrDirectoriesDialogWithKotlinOptions(
        project: Project,
        callback: (MoveFilesOrDirectoriesDialog?) -> Unit): MoveFilesOrDirectoriesDialog(project, callback) {
    private var cbUpdatePackageDirective: JCheckBox? = null

    public val updatePackageDirective: Boolean
        get() = cbUpdatePackageDirective!!.isSelected()

    override fun createNorthPanel(): JComponent {
        val panel = super.createNorthPanel()

        val gbc = GridBagConstraints()

        gbc.gridwidth = 1
        gbc.gridx = 0
        gbc.gridy = GridBagConstraints.RELATIVE
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = Insets(UIUtil.LARGE_VGAP, 0, 0, UIUtil.DEFAULT_HGAP)
        panel.add(JLabel(), gbc)

        cbUpdatePackageDirective = NonFocusableCheckBox()
        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = Insets(UIUtil.LARGE_VGAP, 0, 0, 0)
        panel.add(cbUpdatePackageDirective, gbc)

        return panel
    }

    override fun setData(psiElements: Array<out PsiElement>, initialTargetDirectory: PsiDirectory?, helpID: String) {
        super.setData(psiElements, initialTargetDirectory, helpID)

        with (cbUpdatePackageDirective!!) {
            val jetFiles = psiElements.filterIsInstance<JetFile>()
            if (jetFiles.isEmpty()) {
                getParent().remove(cbUpdatePackageDirective)
                return
            }

            val singleFile = jetFiles.singleOrNull()
            if (singleFile != null) {
                setSelected(singleFile.packageMatchesDirectory())
                setText("Update package directive")
            }
            else {
                setSelected(true)
                setText("Update package directive for Kotlin files")
            }
        }
    }
}