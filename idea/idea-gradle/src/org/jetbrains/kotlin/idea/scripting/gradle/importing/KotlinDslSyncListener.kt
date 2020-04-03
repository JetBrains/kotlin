/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

var Project.kotlinDslModels: ArrayList<KotlinDslScriptModel>
        by NotNullableUserDataProperty<Project, ArrayList<KotlinDslScriptModel>>(
            Key("Kotlin DSL Scripts Models"), arrayListOf()
        )

class KotlinDslSyncListener : ExternalSystemTaskNotificationListenerAdapter() {
    override fun onEnd(id: ExternalSystemTaskId) {
        if (id.type != ExternalSystemTaskType.RESOLVE_PROJECT || id.projectSystemId != GRADLE_SYSTEM_ID) {
            return
        }

        val project = id.findProject() ?: return

        val models: List<KotlinDslScriptModel> = project.kotlinDslModels
        project.kotlinDslModels = arrayListOf()

        if (models.isEmpty()) return

        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        val projectSettings = gradleSettings.getLinkedProjectsSettings()
            .filterIsInstance<GradleProjectSettings>().firstOrNull()
            ?: return

        saveScriptModels(project, id, projectSettings.gradleHome, models)
    }
}
