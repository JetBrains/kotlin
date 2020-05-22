/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
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
        synchronized(tasks) { tasks[id] = KotlinDslGradleBuildSync(workingDir, id) }

        // project may be null in case of new project
        val project = id.findProject() ?: return
        GradleBuildRootsManager.getInstance(project).markImportingInProgress(workingDir)
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        if (!isGradleProjectResolve(id)) return

        val sync = synchronized(tasks) { tasks.remove(id) } ?: return

        // project may be null in case of new project
        val project = id.findProject() ?: return
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
