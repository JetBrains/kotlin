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

package org.jetbrains.kotlin.idea.internal.makeBackup

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.history.core.RevisionsCollector
import java.io.File
import com.intellij.history.integration.patches.PatchCreator
import com.intellij.history.integration.LocalHistoryImpl
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.project.Project
import java.util.ArrayList
import java.text.SimpleDateFormat
import java.util.Date
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import java.util.zip.ZipOutputStream
import java.io.FileOutputStream
import com.intellij.util.io.ZipUtil
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.util.WaitForProgressToShow
import com.intellij.openapi.application.ApplicationManager

public class CreateIncrementalCompilationBackup: AnAction("Create backup for debugging Kotlin incremental compilation") {
    default object {
        val BACKUP_DIR_NAME = ".backup"
        val PATCHES_TO_CREATE = 5

        val PATCHES_FRACTION = .25
        val LOGS_FRACTION = .05
        val ZIP_FRACTION = 1.0 - PATCHES_FRACTION - LOGS_FRACTION
    }

    override fun update(e: AnActionEvent) {
        e.getPresentation().setVisible(incrementalCompilationEnabled(e.getProject()!!))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getProject()!!
        val projectBaseDir = File(project.getBaseDir()!!.getPath())
        val backupDir = File(FileUtil.createTempDirectory("makeBackup", null), BACKUP_DIR_NAME)

        ProgressManager.getInstance().run(
                object : Task.Backgroundable(project, "Creating backup for debugging Kotlin incremental compilation", true) {
                    override fun run(indicator: ProgressIndicator) {
                        createPatches(backupDir, project, indicator)
                        copyLogs(backupDir, indicator)

                        zipProjectDir(backupDir, project, projectBaseDir, indicator)
                    }
                }
        )
    }

    private fun createPatches(backupDir: File, project: Project, indicator: ProgressIndicator) {
        ApplicationManager.getApplication()!!.runReadAction(Runnable {
            val localHistoryImpl = LocalHistoryImpl.getInstanceImpl()!!
            val gateway = localHistoryImpl.getGateway()!!
            val localHistoryFacade = localHistoryImpl.getFacade()

            val revisionsCollector = RevisionsCollector(localHistoryFacade, gateway.createTransientRootEntry(), project.getBaseDir()!!.getPath(), project.getLocationHash(), null)

            var patchesCreated = 0

            val patchesDir = File(backupDir, "patches")
            patchesDir.mkdirs()

            val revisions = revisionsCollector.getResult()!!
            for (rev in revisions) {
                val label = rev.getLabel()
                if (label != null && label.startsWith(HISTORY_LABEL_PREFIX)) {
                    val patchFile = File(patchesDir, label.trimLeading(HISTORY_LABEL_PREFIX) + ".patch")

                    indicator.setText("Creating patch $patchFile")
                    indicator.setFraction(PATCHES_FRACTION * patchesCreated / PATCHES_TO_CREATE)

                    val differences = revisions[0].getDifferencesWith(rev)!!
                    val changes = differences.map {
                        d ->
                        Change(d.getLeftContentRevision(gateway), d.getRightContentRevision(gateway))
                    }

                    PatchCreator.create(project, changes, patchFile.getPath(), false, null)

                    if (++patchesCreated >= PATCHES_TO_CREATE) {
                        break
                    }
                }
            }
        })
    }

    private fun copyLogs(backupDir: File, indicator: ProgressIndicator) {
        indicator.setText("Copying logs")
        indicator.setFraction(PATCHES_FRACTION)

        val logsDir = File(backupDir, "logs")
        FileUtil.copyDir(File(PathManager.getLogPath()), logsDir)

        indicator.setFraction(PATCHES_FRACTION + LOGS_FRACTION)
    }

    private fun zipProjectDir(backupDir: File, project: Project, projectDir: File, indicator: ProgressIndicator) {
        // files and relative paths

        val files = ArrayList<Pair<File, String>>() // files and relative paths
        var totalBytes = 0L

        for (dir in listOf(projectDir, backupDir.getParentFile()!!)) {
            FileUtil.processFilesRecursively(
                    dir,
                    /*processor*/ {
                        if (it!!.isFile()
                            && !it.getName().endsWith(".hprof")
                            && !(it.getName().startsWith("make_backup_") && it.getName().endsWith(".zip"))
                        ) {

                            indicator.setText("Scanning project dir: $it")

                            files.add(Pair(it, FileUtil.getRelativePath(dir, it)!!))
                            totalBytes += it.length()
                        }
                        true
                    },
                    /*directoryFilter*/ {
                        val name = it!!.getName()
                        name != ".git" && name != "out"
                    }
            )
        }


        val backupFile = File(projectDir, "make_backup_" + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date()) + ".zip")


        val zos = ZipOutputStream(FileOutputStream(backupFile))

        var processedBytes = 0L

        zos.use {
            for ((file, relativePath) in files) {
                indicator.setText("Adding file to backup: $relativePath")
                indicator.setFraction(PATCHES_FRACTION + LOGS_FRACTION + processedBytes.toDouble() / totalBytes * ZIP_FRACTION)

                ZipUtil.addFileToZip(zos, file, relativePath, null, null)

                processedBytes += file.length()
            }
        }

        FileUtil.delete(backupDir)

        WaitForProgressToShow.runOrInvokeLaterAboveProgress({
            ShowFilePathAction.showDialog(
                    project,
                    "Successfully created backup " + backupFile.getAbsolutePath(),
                    "Created backup",
                    backupFile,
                    null
            )
        }, null, project)
    }
}
