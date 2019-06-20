/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.vcs

import com.intellij.BundleBase.replaceMnemonicAmpersand
import com.intellij.CommonBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.NO
import com.intellij.openapi.ui.Messages.YES
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.util.PairConsumer
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import java.awt.GridLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

private var Project.bunchFileCheckEnabled: Boolean by NotNullableUserDataProperty(Key.create("IS_BUNCH_FILE_CHECK_ENABLED"), true)

class BunchFileCheckInHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return BunchCheckInHandler(panel)
    }

    class BunchCheckInHandler(private val checkInProjectPanel: CheckinProjectPanel) : CheckinHandler() {
        private val project get() = checkInProjectPanel.project

        override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
            BunchFileUtils.bunchFile(project) ?: return null

            val bunchFilesCheckBox = NonFocusableCheckBox(replaceMnemonicAmpersand("Check &bunch files"))
            return object : RefreshableOnComponent {
                override fun getComponent(): JComponent {
                    val panel = JPanel(GridLayout(1, 0))
                    panel.add(bunchFilesCheckBox)
                    return panel
                }

                override fun refresh() {}
                override fun saveState() {
                    project.bunchFileCheckEnabled = bunchFilesCheckBox.isSelected
                }

                override fun restoreState() {
                    bunchFilesCheckBox.isSelected = project.bunchFileCheckEnabled
                }
            }
        }

        override fun beforeCheckin(
            executor: CommitExecutor?,
            additionalDataConsumer: PairConsumer<Any, Any>?
        ): ReturnResult {
            if (!project.bunchFileCheckEnabled) return ReturnResult.COMMIT

            val extensions = BunchFileUtils.bunchExtension(project)?.toSet() ?: return ReturnResult.COMMIT

            val forgottenFiles = HashSet<File>()
            val commitFiles = checkInProjectPanel.files.filter { it.isFile }.toSet()
            for (file in commitFiles) {
                if (file.extension in extensions) continue

                val parent = file.parent ?: continue
                val name = file.name
                for (extension in extensions) {
                    val bunchFile = File(parent, "$name.$extension")
                    if (bunchFile !in commitFiles && bunchFile.exists()) {
                        forgottenFiles.add(bunchFile)
                    }
                }
            }

            if (forgottenFiles.isEmpty()) return ReturnResult.COMMIT

            val projectBaseFile = File(project.basePath)
            var filePaths = forgottenFiles.map { it.relativeTo(projectBaseFile).path }.sorted()
            if (filePaths.size > 15) {
                filePaths = filePaths.take(15) + "..."
            }

            when (Messages.showYesNoCancelDialog(
                project,
                "Several bunch files haven't been updated:\n\n${filePaths.joinToString("\n")}\n\nDo you want to review them before commit?",
                "Forgotten Bunch Files", "Review", "Commit", CommonBundle.getCancelButtonText(), Messages.getWarningIcon()
            )) {
                YES -> {
                    return ReturnResult.CLOSE_WINDOW
                }
                NO -> return ReturnResult.COMMIT
            }

            return ReturnResult.CANCEL
        }
    }
}

object BunchFileUtils {
    fun bunchFile(project: Project): VirtualFile? {
        val baseDir = project.baseDir ?: return null
        return baseDir.findChild(".bunch")
    }

    fun bunchExtension(project: Project): List<String>? {
        val bunchFile: VirtualFile = bunchFile(project) ?: return null
        val file = File(bunchFile.path)
        if (!file.exists()) return null

        val lines = file.readLines().map { it.trim() }.filter { !it.isEmpty() }
        if (lines.size <= 1) return null

        return lines.drop(1).map { it.split('_').first() }
    }
}
