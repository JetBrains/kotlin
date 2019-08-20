/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.ide.actions.OpenProjectFileChooserDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases.MoveRefactoringCase
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MoveRefactoringAction : AnAction() {

    companion object {
        const val WINDOW_TITLE: String = "Move refactoring testing"
        const val RECENT_SELECTED_PATH = "org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.RECENT_SELECTED_PATH"
    }

    private val nestedRefactoring = MoveRefactoringCase()

    private var iteration = 0
    private var fails = 0

    private fun randomMoveAndCheck(
        project: Project,
        projectRoot: VirtualFile,
        indicator: ProgressIndicator,
        actionRunner: CompilationStatusTracker,
        fileTracker: FileSystemChangesTracker,
        resultsFile: File,
        cancelledChecker: () -> Boolean
    ) {

        iteration++
        try {
            indicator.text = "$WINDOW_TITLE [Try $iteration with $fails fails]"

            indicator.text2 = "Update indices..."
            indicator.fraction = 0.0

            DumbService.getInstance(project).waitForSmartMode()

            indicator.text2 = "Perform refactoring ..."
            indicator.fraction = 0.1

            fileTracker.reset()

            var refactoringResult: RandomMoveRefactoringResult = RandomMoveRefactoringResult.Failed
            edtExecute {
                refactoringResult = nestedRefactoring.tryCreateAndRun(project)
                indicator.text2 = "Saving files..."
                indicator.fraction = 0.3
                FileDocumentManager.getInstance().saveAllDocuments()
                VfsUtil.markDirtyAndRefresh(false, true, true, projectRoot)
            }

            when (val localRefactoringResult = refactoringResult) {
                is RandomMoveRefactoringResult.Success -> {
                    indicator.text2 = "Compiling project..."
                    indicator.fraction = 0.7
                    if (!actionRunner.checkByBuild(cancelledChecker)) {
                        fails++
                        resultsFile.appendText("${localRefactoringResult.caseData}\n\n")
                    }
                }
                is RandomMoveRefactoringResult.ExceptionCaused -> {
                    fails++
                    resultsFile.appendText("${localRefactoringResult.caseData}\nWith exception\n${localRefactoringResult.message}\n\n")
                }
                is RandomMoveRefactoringResult.Failed -> {
                }
            }

        } finally {
            indicator.text2 = "Reset files..."
            indicator.fraction = 0.9

            fileTracker.createdFiles.toList().map {
                try {
                    edtExecute {
                        runWriteAction {
                            if (it.exists()) it.delete(null)
                        }
                    }
                } catch (e: IOException) {
                    //pass
                }
            }

            gitReset(project, projectRoot)
        }

        indicator.text2 = "Done"
        indicator.fraction = 1.0
    }

    private fun createFileIfNotExist(targetPath: String): File? {

        val stamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd-HH-mm-ss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
            .toString()

        val resultsFile = File(targetPath, "REFACTORING_TEST_RESULT-$stamp.txt")
        return try {
            resultsFile.apply { createNewFile() }
        } catch (e: IOException) {
            null
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val projectRoot = project?.guessProjectDir()

        if (projectRoot === null) return

        val targetPath = OpenProjectFileChooserDescriptor(true).let { descriptor ->
            descriptor.title = "Select test result directory path"
            val preselected = PropertiesComponent.getInstance().getValue(RECENT_SELECTED_PATH)?.let {
                File(it).toVirtualFile()
            }
            FileChooser.chooseFiles(descriptor, project, preselected).firstOrNull()?.path
        }
        if (targetPath === null) {
            Messages.showErrorDialog(project, "The path must be selected", WINDOW_TITLE)
            return
        }

        val resultsFile = createFileIfNotExist(targetPath)
        if (resultsFile === null) {
            Messages.showErrorDialog(project, "Cannot get or create results file", WINDOW_TITLE)
            return
        }

        PropertiesComponent.getInstance().setValue(RECENT_SELECTED_PATH, targetPath)

        ProgressManager.getInstance().run(object : Task.Modal(project, WINDOW_TITLE, true) {
            override fun run(indicator: ProgressIndicator) {
                val compilationStatusTracker = CompilationStatusTracker(project)
                val fileSystemChangesTracker = FileSystemChangesTracker(project)
                val cancelledChecker = { indicator.isCanceled }
                iteration = 0
                fails = 0
                while (!cancelledChecker()) {
                    iteration++
                    randomMoveAndCheck(
                        project,
                        projectRoot,
                        indicator,
                        compilationStatusTracker,
                        fileSystemChangesTracker,
                        resultsFile,
                        cancelledChecker
                    )
                }
                fileSystemChangesTracker.dispose()
                indicator.stop()
            }
        })
    }
}
