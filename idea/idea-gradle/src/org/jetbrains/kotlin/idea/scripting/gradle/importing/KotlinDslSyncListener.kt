/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.projectRoots.JdkUtil
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptDefinitionsContributor
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.scripting.gradle.validateGradleSdk
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.*

class KotlinDslSyncListener : ExternalSystemTaskNotificationListenerAdapter() {
    companion object {
        val instance: KotlinDslSyncListener
            get() = ExternalSystemTaskNotificationListener.EP_NAME
                .extensionList.filterIsInstance<KotlinDslSyncListener>().single()
    }

    val tasks = WeakHashMap<ExternalSystemTaskId, KotlinDslGradleBuildSync>()

    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        if (!isGradleProjectResolve(id)) return

        if (workingDir == null) return
        val task = KotlinDslGradleBuildSync(workingDir, id)
        synchronized(tasks) { tasks[id] = task }

        // project may be null in case of new project
        val project = id.findProject() ?: return
        task.project = project
        GradleBuildRootsManager.getInstance(project).markImportingInProgress(workingDir)
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        if (!isGradleProjectResolve(id)) return

        val sync = synchronized(tasks) { tasks.remove(id) } ?: return

        // project may be null in case of new project
        val project = id.findProject() ?: return

        if (sync.gradleHome == null) {
            sync.gradleHome = ServiceManager
                .getService(GradleInstallationManager::class.java)
                .getGradleHome(project, sync.workingDir)
                ?.canonicalPath
        }

        if (sync.javaHome == null) {
            sync.javaHome = ExternalSystemApiUtil
                .getExecutionSettings<GradleExecutionSettings>(project, sync.workingDir, GradleConstants.SYSTEM_ID)
                .javaHome
        }

        sync.javaHome = sync.javaHome?.takeIf { JdkUtil.checkForJdk(it) }
            ?: run {
                // roll back to specified in GRADLE_JVM if for some reason sync.javaHome points to corrupted SDK
                val gradleJvm = GradleSettings.getInstance(project).getLinkedProjectSettings(sync.workingDir)?.gradleJvm
                ExternalSystemJdkUtil.getJdk(project, gradleJvm)?.homePath
            }

        GradleSettings.getInstance(project).getLinkedProjectSettings(sync.workingDir)?.validateGradleSdk(project, sync.javaHome)

        @Suppress("DEPRECATION")
        ScriptDefinitionContributor.find<GradleScriptDefinitionsContributor>(project)?.reloadIfNeeded(
            sync.workingDir, sync.gradleHome, sync.javaHome
        )

        saveScriptModels(project, sync)
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        if (!isGradleProjectResolve(id)) return

        val sync = synchronized(tasks) { tasks[id] } ?: return
        sync.failed = true
    }

    override fun onCancel(id: ExternalSystemTaskId) {
        if (!isGradleProjectResolve(id)) return

        val cancelled = synchronized(tasks) { tasks.remove(id) }

        // project may be null in case of new project
        val project = id.findProject() ?: return
        if (cancelled != null) {
            GradleBuildRootsManager.getInstance(project).markImportingInProgress(cancelled.workingDir, false)
        }
    }

    private fun isGradleProjectResolve(id: ExternalSystemTaskId) =
        id.type == ExternalSystemTaskType.RESOLVE_PROJECT &&
                id.projectSystemId == GRADLE_SYSTEM_ID
}
