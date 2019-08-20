/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
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

    private val refactoring = MoveRefactoringCase()

    private var iteration = 0
    private var fails = 0
    private var verifications = 0

    private fun randomRefactoringAndCheck(
        project: Project,
        projectRoot: VirtualFile,
        setIndicator: (String, Double) -> Unit,
        actionRunner: CompilationStatusTracker,
        fileTracker: FileSystemChangesTracker,
        resultsFile: File,
        refactoringCountBeforeCheck: Int,
        cancelledChecker: () -> Boolean
    ) {

        try {
            setIndicator("Update indices...", 0.0)

            DumbService.getInstance(project).waitForSmartMode()

            setIndicator("Perform refactoring ...", 0.1)

            fileTracker.reset()

            var refactoringResult: RandomMoveRefactoringResult = RandomMoveRefactoringResult.Failed
            edtExecute {
                refactoringResult = refactoring.tryCreateAndRun(project, refactoringCountBeforeCheck)
                setIndicator("Saving files...", 0.3)
                FileDocumentManager.getInstance().saveAllDocuments()
                VfsUtil.markDirtyAndRefresh(false, true, true, projectRoot)
            }

            when (val localRefactoringResult = refactoringResult) {
                is RandomMoveRefactoringResult.Success -> {
                    verifications++

                    setIndicator("Compiling project...", 0.7)
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
            setIndicator("Reset files...", 0.9)

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

        setIndicator("Done", 1.0)
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

        if (projectRoot === null) {
            Messages.showErrorDialog(project, "Cannot get project root directory", WINDOW_TITLE)
            return
        }

        val dialog = MoveRefactoringActionDialog(project, projectRoot.path)
        dialog.show()
        if (!dialog.isOK) return

        val targetPath = dialog.selectedDirectoryName
        val countOfMovesBeforeCheck = dialog.selectedCount

        val resultsFile = createFileIfNotExist(targetPath)
        if (resultsFile === null) {
            Messages.showErrorDialog(project, "Cannot get or create results file", WINDOW_TITLE)
            return
        }

        PropertiesComponent.getInstance().setValue(RECENT_SELECTED_PATH, targetPath)

        ProgressManager.getInstance().run(object : Task.Modal(project, WINDOW_TITLE, /* canBeCancelled = */ true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    unsafeRefactoringAndCheck(
                        project = project,
                        indicator = indicator,
                        projectRoot = projectRoot,
                        resultsFile = resultsFile,
                        countOfMovesBeforeCheck = countOfMovesBeforeCheck
                    )
                } catch (e: Exception) {
                    if (e !is ProcessCanceledException && e.cause !is ProcessCanceledException) {
                        throw e
                    }
                }
            }
        })
    }

    private fun unsafeRefactoringAndCheck(
        project: Project,
        indicator: ProgressIndicator,
        projectRoot: VirtualFile,
        resultsFile: File,
        countOfMovesBeforeCheck: Int
    ) {
        val compilationStatusTracker = CompilationStatusTracker(project)
        val fileSystemChangesTracker = FileSystemChangesTracker(project)
        val cancelledChecker = { indicator.isCanceled }
        val setIndicator = { text: String, fraction: Double ->
            indicator.text2 = text
            indicator.fraction = fraction
        }

        iteration = 0
        fails = 0
        verifications = 0

        try {
            while (!cancelledChecker()) {
                iteration++
                indicator.text = "$WINDOW_TITLE [Try $iteration with $fails fails and $verifications verifications]"

                randomRefactoringAndCheck(
                    project = project,
                    projectRoot = projectRoot,
                    setIndicator = setIndicator,
                    actionRunner = compilationStatusTracker,
                    fileTracker = fileSystemChangesTracker,
                    resultsFile = resultsFile,
                    refactoringCountBeforeCheck = countOfMovesBeforeCheck,
                    cancelledChecker = cancelledChecker
                )
            }
        } finally {
            fileSystemChangesTracker.dispose()
            indicator.stop()
        }
    }
}
