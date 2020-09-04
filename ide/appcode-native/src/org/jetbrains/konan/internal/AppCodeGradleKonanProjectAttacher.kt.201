package org.jetbrains.konan.internal

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

class AppCodeGradleKonanProjectAttacher(
    private val project: Project
) : ProjectComponent {

    private val messageBusConnection = project.messageBus.connect()

    init {
        messageBusConnection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            refreshOCRoots()
            val name = XcodeMetaData.getInstance(project).projectOrWorkspaceFile?.nameWithoutExtension
            name?.let { (project as ProjectEx).setProjectName(it) }
        })
    }

    override fun projectOpened() {
        ExternalProjectsManager.getInstance(project).runWhenInitialized {
            if (!GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
                return@runWhenInitialized
            }

            findGradleProject()?.let {
                val notification = GradleNotification.NOTIFICATION_GROUP.createNotification(
                        GradleBundle.message("gradle.notifications.unlinked.project.found.title",
                                ApplicationNamesInfo.getInstance().fullProductName),
                        NotificationType.INFORMATION)
                notification.addAction(NotificationAction.createSimple(
                        GradleBundle.message("gradle.notifications.unlinked.project.found.import")) {
                    notification.expire()
                    linkAndRefreshGradleProject(it.parentFile.absolutePath, project)
                })

                notification.notify(project)
            }
        }
    }

    private fun findGradleProject(): File? {
        project.guessProjectDir()?.let { projectDir ->
            val baseDir = Paths.get(projectDir.path, "Supporting Files")
            return FileUtil.findFirstThatExist(
                    "$baseDir/${GradleConstants.DEFAULT_SCRIPT_NAME}",
                    "$baseDir/${GradleConstants.KOTLIN_DSL_SCRIPT_NAME}"
            )
        }

        return null
    }

    private fun refreshOCRoots() {
        TransactionGuard.getInstance().submitTransactionAndWait {
            runWriteAction {
                XcodeMetaData.getInstance(project).apply {
                    updateContentRoots()
                    //todo [medvedev] use proper tracker instead of `xcodeProjectTrackers.referencesTracker`
                    xcodeProjectTrackers.referencesTracker.incModificationCount()
                    dropLocalResolve(project)
                }
            }
        }
    }

    private fun dropLocalResolve(project: Project) {
        TransactionGuard.getInstance().submitTransactionLater(project, Runnable {
            (PsiManager.getInstance(project).modificationTracker as? PsiModificationTrackerImpl)?.let { psiTracker ->
                runWriteAction { psiTracker.incCounter() }
            }
            DaemonCodeAnalyzer.getInstance(project).restart()
        })
    }

}