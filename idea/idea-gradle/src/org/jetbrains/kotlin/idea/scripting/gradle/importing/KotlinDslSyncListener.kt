/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.scripting.gradle.getJavaHomeForGradleProject
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import java.util.concurrent.ConcurrentHashMap

var Project.kotlinGradleDslSync: MutableMap<ExternalSystemTaskId, KotlinDslGradleBuildSync>
        by NotNullableUserDataProperty<Project, MutableMap<ExternalSystemTaskId, KotlinDslGradleBuildSync>>(
            Key("Kotlin DSL Scripts Models"), ConcurrentHashMap()
        )

class KotlinDslSyncListener : ExternalSystemTaskNotificationListenerAdapter() {
    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        if (workingDir != null) {
            val project = id.findProject() ?: return
            project.kotlinGradleDslSync[id] = KotlinDslGradleBuildSync(workingDir, id)
        }
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type != ExternalSystemTaskType.RESOLVE_PROJECT || id.projectSystemId != GRADLE_SYSTEM_ID) {
            return
        }

        val project = id.findProject() ?: return
        val sync = project.kotlinGradleDslSync.remove(id) ?: return

        if (sync.models.isEmpty()) return

        saveScriptModels(project, sync)
    }
}
