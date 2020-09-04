package org.jetbrains.konan.internal

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.facet.FacetManager
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.jetbrains.cidr.xcode.XCLog
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import com.jetbrains.cidr.xcode.model.XcodeProjectEvent
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.settings.GradleSettings
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
            addModuleDependency()
        })
    }

    override fun projectOpened() {
        ExternalProjectsManager.getInstance(project).runWhenInitialized {
            if (!GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
                addModuleDependency()
                return@runWhenInitialized
            }

            findGradleProject()?.let {
                val notification = GradleNotification.NOTIFICATION_GROUP.createNotification(
                    ExternalSystemBundle.message("unlinked.project.notification.title", "Gradle"),
                    NotificationType.INFORMATION
                )
                notification.addAction(NotificationAction.createSimple(
                    ExternalSystemBundle.message("unlinked.project.notification.load.action", "Gradle")
                ) {
                    notification.expire()
                    linkAndRefreshGradleProject(it.parentFile.absolutePath, project)
                })

                notification.notify(project)
            }
        }
    }

    private fun Module.targetPlatformOrNull(): SimplePlatform? {
        val facets = FacetManager.getInstance(this).allFacets
        if (facets.size > 1) throw IllegalStateException()
        val facetConfiguration = facets.singleOrNull()?.configuration as? KotlinFacetConfiguration
        return facetConfiguration?.settings?.targetPlatform?.componentPlatforms?.singleOrNull()
    }

    private fun addModuleDependency() {
        for (projectInfo in ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
            val projectStructure = projectInfo.externalProjectStructure ?: continue
            val projectData = projectStructure.data
            val modelsProvider = IdeModelsProviderImpl(project)
            val gradleModules = modelsProvider.getModules(projectData).toSet()
            val allModules = ModuleManager.getInstance(project).modules.toSet()
            val kotlinModule = allModules.firstOrNull { it.targetPlatformOrNull() is NativePlatform }
            val xcodeModule = (allModules - gradleModules).firstOrNull()

            XCLog.LOG.assertTrue(xcodeModule != null)
            XCLog.LOG.assertTrue(kotlinModule != null)

            ModuleRootModificationUtil.addDependency(xcodeModule!!, kotlinModule!!)
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
                    xcodeProjectTrackers.workspaceChanged(XcodeProjectEvent.REFERENCES_CHANGED)
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